package firok.spring.alloydesk.deskleg.service_multi;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import firok.spring.alloydesk.deskleg.bean.*;
import firok.spring.alloydesk.deskleg.config.MmdetectionHelper;
import firok.spring.alloydesk.deskleg.controller.TrainTaskController;
import firok.topaz.platform.NativeProcess;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

	private Set<String> findIdWithState(TaskStateEnum state)
	{
		var qw = new QueryWrapper<TrainTaskBean>().lambda()
				.select(TrainTaskBean::getId)
				.eq(TrainTaskBean::getState, state);
		return serviceTask.list(qw).stream().map(TrainTaskBean::getId).collect(Collectors.toSet());
	}
	private void updateStateByIds(Set<String> setId, TaskStateEnum state)
	{
		var uw = new UpdateWrapper<TrainTaskBean>().lambda()
				.set(TrainTaskBean::getState, state)
				.in(TrainTaskBean::getState, setId);
		serviceTask.update(uw);
	}
	@PostConstruct
	void postCons()
	{
		// 每次启动的时候检查数据库
		// 如果数据库里有标记为正在运行的任务
		// 就把这个任务标记为失败

		var setId = findIdWithState(TaskStateEnum.Running);
		if(setId.isEmpty()) return;

		for(var taskId : setId)
			addLog(taskId, LevelKeypoint, "未正常结束, 强制停止");

		updateStateByIds(setId, TaskStateEnum.ErrorEnd);
	}
	@PreDestroy
	void preDes()
	{
		var setId = findIdWithState(TaskStateEnum.Running);
		if(setId.isEmpty()) return;

		for(var taskId : setId)
			addLog(taskId, LevelKeypoint, "系统停止, 强制停止");

		updateStateByIds(setId, TaskStateEnum.ErrorEnd);
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

	/**
	 * 任务 id -> 本地进程
	 * */
	private final Map<String, NativeProcess> mapTaskProcess = new HashMap<>();

	/**
	 * 创建一个新任务
	 * */
	public void createTask(TrainTaskController.CreateTaskParam params) throws Exception
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

			// 创建 mmdetection 需要的配置文件
//			helper.generateTrainScript();
			// todo

			// 创建本地进程
//			var process = new NativeProcess("""
//					""");

			var taskId = task.getId();
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
