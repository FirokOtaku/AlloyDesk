package firok.spring.alloydesk.deskleg.service_multi;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import firok.spring.alloydesk.deskleg.bean.*;
import firok.spring.alloydesk.deskleg.config.MmdetectionHelper;
import firok.spring.alloydesk.deskleg.controller.TrainTaskController;
import firok.topaz.platform.NativeProcess;
import firok.topaz.resource.Files;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * 所有需要更新任务数据的操作都是通过这个服务进行
 * */
@Service
public class TrainTaskMultiService
{
	@Autowired
	IService<TrainTaskBean> serviceTask;
	@Autowired
	IService<TrainTaskLogBean> serviceLog;

	public static final int LevelKeypoint = 100;
	public static final int LevelDetail = 80;
	public static final int LevelDebug = 20;

	public Set<String> findIdWithState(TaskStateEnum state)
	{
		var qw = new QueryWrapper<TrainTaskBean>().lambda()
				.select(TrainTaskBean::getId)
				.eq(TrainTaskBean::getState, state);
		return serviceTask.list(qw).stream().map(TrainTaskBean::getId).collect(Collectors.toSet());
	}
	public void updateStateByIds(Set<String> setId, TaskStateEnum state)
	{
		var uw = new UpdateWrapper<TrainTaskBean>().lambda()
				.set(TrainTaskBean::getState, state)
				.in(TrainTaskBean::getState, setId);
		serviceTask.update(uw);
	}

	public void addLog(String taskId, int level, String content)
	{
		var now = new Date();
		var bean = new TrainTaskLogBean();
		bean.setTrainTaskId(taskId);
		bean.setLogLevel(level);
		bean.setLogValue(content);
		bean.setCreateTimestamp(now);
		bean.setCreateUserId(""); // todo

		try
		{
			GlobalLock.lock();
			serviceLog.save(bean);
		}
		finally
		{
			GlobalLock.unlock();
		}
	}

	/**
	 * 定时器
	 * 检查所有的数据集
	 * */
	public void tick()
	{
		;
	}

	@Autowired
	IService<ModelBean> serviceModel;
	@Autowired
	IService<DatasetBean> serviceDataset;

	@Autowired
	ReentrantLock GlobalLock;

	@Autowired
	MmdetectionHelper helper;

	@Value("${firok.spring.alloydesk.folder-task}")
	File folderTaskStorage;

	@Value("${firok.spring.alloydesk.folder-mmdetection}")
	File folderMmdetection;

	String fileMmdetectionTrainPy;
	@PostConstruct
	void postCons() throws IOException
	{
		fileMmdetectionTrainPy = new File(folderMmdetection, "tools/train.py").getCanonicalPath();
	}

	public File folderOfTask(String taskId) throws IOException
	{
		return new File(folderTaskStorage, taskId).getCanonicalFile();
	}
	public File fileOfMmdetectionTaskConfig(String taskId) throws IOException
	{
		return new File(folderTaskStorage, taskId + "/" + taskId + ".py").getCanonicalFile();
	}
	public File folderOfMmdetectionTaskWorkdir(String taskId) throws IOException
	{
		return new File(folderMmdetection, "workdir/" + taskId).getCanonicalFile();
	}

	@Autowired
	DatasetMultiService serviceDatasetMulti;

	@Autowired
	ModelMultiService serviceModelMulti;

	/**
	 * 任务 id -> 本地进程
	 * */
	private final Map<String, NativeProcess> mapTaskProcess = new HashMap<>();

	@Value("${firok.spring.alloydesk.mmdetection-pre-script}")
	String preScript;

	/**
	 * 创建一个新任务
	 * */
	public void createMmdetectionTask(TrainTaskController.CreateMmdetectionTaskParam params) throws Exception
	{
		try
		{
			GlobalLock.lock();

			var frameworkType = params.frameworkType();
			var modelId = params.modelId();
			var datasetId = params.datasetId();
			var model = serviceModel.getById(modelId);
			var dataset = serviceDataset.getById(datasetId);

			if(model == null)
				throw new IllegalArgumentException("不存在的模型");
			if(dataset == null)
				throw new IllegalArgumentException("不存在的数据集");
			if(model.getModelType() != frameworkType)
				throw new IllegalArgumentException("不匹配的框架类型");
			switch (dataset.getStatus())
			{
				case Broken -> throw new IllegalStateException("数据集已损坏");
				case Logical -> throw new IllegalStateException("数据集仅存档状态");
				case Pulling -> throw new IllegalStateException("数据集正在拉取");
			}
			if(dataset.getAnnotationCount() <= 0)
				throw new IllegalStateException("数据集没有标注数据");
			if(dataset.getPictureCount() <= 0)
				throw new IllegalStateException("数据集没有图片数据");

			// 获取数据集和模型目录
			var folderDataset = serviceDatasetMulti.folderOfDataset(datasetId);
			var fileDatasetAnnotations = serviceDatasetMulti.fileOfCocoDatasetAnnotation(folderDataset);
			var folderDatasetImages = serviceDatasetMulti.folderOfCocoDatasetImages(folderDataset);
			var fileModel = serviceModelMulti.fileOfModel(modelId);

			// 更新数据集状态
			var beanDatasetUpdate = new DatasetBean();
			beanDatasetUpdate.setId(datasetId);
			beanDatasetUpdate.setStatus(DatasetStatusEnum.Occupied);
			serviceDataset.updateById(beanDatasetUpdate);

			// 创建任务记录
			var now = new Date();
			var task = new TrainTaskBean();
			task.setState(TaskStateEnum.WaitingStart);
			task.setDatasetId(datasetId);
			task.setSourceModelId(modelId);
			task.setCurrentModelId(modelId);
			task.setFrameworkType(frameworkType);
			task.setCreateTimestamp(now);
			task.setCreateUserId(""); // todo
			task.setStorageMethod(params.modelStorageMethod());
			task.setProcessControlMethod(params.processControlMethod());
			task.setConfigValue(null);
			task.setDisplayName(params.displayName());
			serviceTask.save(task);

			var taskId = task.getId(); // 任务 id

			// 创建 mmdetection 需要的配置文件
			var contentConfigPy = helper.generateTrainScript(
					fileModel,
					fileDatasetAnnotations,
					folderDatasetImages,
					taskId,
					params
			);
			var fileConfig = fileOfMmdetectionTaskConfig(taskId);
			Files.writeTo(fileConfig, contentConfigPy);

			// 创建本地进程
			// todo 等真机能用的时候再改
//			var process = new NativeProcess(preScript);
//			var cmd = """
//                    %s -c %s
//                    """.formatted(fileMmdetectionTrainPy, fileConfig);
//			process.println(cmd);

			addLog(taskId, LevelKeypoint, "任务已创建");
		}
		finally
		{
			GlobalLock.unlock();
		}
	}

	/**
	 * 停止一个进行中的任务
	 * */
	public void shutdownTask(String taskId) throws Exception
	{
		try
		{
			GlobalLock.lock();

			var task = serviceTask.getById(taskId);
			if(task == null) throw new IllegalArgumentException("不存在的任务");
			switch (task.getState())
			{
				case Running -> {
					Exception exceptionStopProcess = null, exceptionStopTask = null;
					try
					{
						var process = mapTaskProcess.get(taskId);
						if(process == null) throw new IllegalStateException("找不到本地进程");
						process.close();
					}
					catch (Exception any)
					{
						exceptionStopProcess = any;
					}
					try
					{
						var taskUpdate = new TrainTaskBean();
						taskUpdate.setId(taskId);
						taskUpdate.setState(TaskStateEnum.ShutdownEnd);
						serviceTask.updateById(taskUpdate);
						addLog(taskId, LevelKeypoint, "用户手动停止任务");
					}
					catch (Exception any)
					{
						exceptionStopTask = any;
					}

					if(exceptionStopTask != null && exceptionStopProcess != null)
						throw new IllegalArgumentException(
								"无法停止任务和本地进程: %s; %s".formatted(
										exceptionStopTask.getLocalizedMessage(),
										exceptionStopProcess.getLocalizedMessage()
								)
						);
					else if(exceptionStopTask != null)
						throw new IllegalArgumentException(
								"无法停止任务: %s".formatted(exceptionStopTask.getLocalizedMessage())
						);
					else if (exceptionStopProcess != null)
						throw new IllegalArgumentException(
								"无法停止本地进程: %s".formatted(exceptionStopProcess.getLocalizedMessage())
						);
				}
				case WaitingStart -> throw new IllegalStateException("任务未开始");
				case ShutdownEnd, SuccessfulEnd, ErrorEnd -> throw new IllegalStateException("任务已停止");
			}
		}
		finally
		{
			GlobalLock.unlock();
		}
	}
}
