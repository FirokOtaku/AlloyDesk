package firok.spring.alloydesk.deskleg.service_multi;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.fasterxml.jackson.databind.ObjectMapper;
import firok.spring.alloydesk.deskleg.bean.*;
import firok.spring.alloydesk.deskleg.config.MmdetectionHelper;
import firok.spring.alloydesk.deskleg.controller.TrainTaskController;
import firok.topaz.platform.NativeProcess;
import firok.topaz.resource.Files;
import firok.topaz.spring.Ret;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
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

	public Set<String> findIdWithState(TaskStateEnum... states)
	{
		var qw = new QueryWrapper<TrainTaskBean>().lambda()
				.select(TrainTaskBean::getId)
				.in(TrainTaskBean::getState, Arrays.asList(states));
		return serviceTask.list(qw).stream().map(TrainTaskBean::getId).collect(Collectors.toSet());
	}
	public void updateStates(Set<String> setId, TaskStateEnum state)
	{
		try
		{
			GlobalLock.lock();
			var uw = new UpdateWrapper<TrainTaskBean>().lambda()
					.set(TrainTaskBean::getState, state)
					.in(TrainTaskBean::getState, setId);
			serviceTask.update(uw);
		}
		finally
		{
			GlobalLock.unlock();
		}
	}
	private void updateState(String taskId, TaskStateEnum state)
	{
		try
		{
			GlobalLock.lock();
			var beanUpdate = new TrainTaskBean();
			beanUpdate.setId(taskId);
			beanUpdate.setState(state);
			serviceTask.updateById(beanUpdate);
		}
		finally
		{
			GlobalLock.unlock();
		}
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
	public File fileOfTrainBatch(String taskId) throws IOException
	{
		return new File(folderTaskStorage, taskId + "/" + taskId + ".bat").getCanonicalFile();
	}
	public File fileOfMmdetectionTaskConfig(String taskId) throws IOException
	{
		return new File(folderTaskStorage, taskId + "/" + taskId + ".py").getCanonicalFile();
	}
	public File folderOfMmdetectionTaskWorkdir(String taskId) throws IOException
	{
		return new File(folderTaskStorage, taskId + "/mmdetection_workdir").getCanonicalFile();
	}
	public File fileOfMmdetectionTaskWorkdirCheckpoint(String taskId, int epoch) throws IOException
	{
		var workdir = folderOfMmdetectionTaskWorkdir(taskId);
		return new File(workdir, "epoch_" + epoch + ".pth");
	}

	@Autowired
	DatasetMultiService serviceDatasetMulti;

	@Autowired
	ModelMultiService serviceModelMulti;

	/**
	 * 任务 id -> 本地进程
	 * */
	private final Map<String, MmdetectionTrainThread> mapContext = new HashMap<>();

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
			var modelId = params.initModelId();
			var datasetId = params.initDatasetId();
			var startControlMethod = params.startControlMethod();

			var om = new ObjectMapper();
			var config = om.createObjectNode();
			config.put("epochX", params.epochX());
			config.put("epochY", params.epochY());
			config.put("lr", params.lr());
			config.put("momentum", params.momentum());
			config.put("weightDecay", params.weightDecay());

			// 创建任务记录
			var now = new Date();
			var task = new TrainTaskBean();
			task.setDisplayName(params.displayName());
			task.setState(TaskStateEnum.WaitingStart);
			task.setInitModelId(modelId);
			task.setCurrentModelId(modelId);
			task.setInitDatasetId(datasetId);
			task.setCurrentDatasetId(datasetId);
			task.setStorageMethod(params.modelStorageMethod());
			task.setProcessControlMethod(params.processControlMethod());
			task.setStartControlMethod(params.startControlMethod());
			task.setFrameworkType(frameworkType);
			task.setConfigValue(config);
			task.setCreateTimestamp(now);
			task.setCreateUserId(""); // todo
			serviceTask.save(task);

			var taskId = task.getId(); // 任务 id

			// 保存任务标签
			serviceTag.setTagValues(taskId, TagTypeEnum.TaskGenerateModelTag, params.labels());

			addLog(taskId, LevelKeypoint, "任务创建完成");

			var shouldStartNow = switch(startControlMethod)
			{
				case Now -> true;
				case Auto -> {
					var qw = new QueryWrapper<TrainTaskBean>().lambda()
							.in(TrainTaskBean::getState, TaskStateEnum.Running, TaskStateEnum.Starting, TaskStateEnum.Stopping);
					var count = serviceTask.count(qw);
					yield count <= 0;
				}
				case null, default -> false;
			};
			if(shouldStartNow) startTask(taskId);
		}
		finally
		{
			GlobalLock.unlock();
		}
	}

	public void startTask(String taskId) throws Exception
	{
		try
		{
			GlobalLock.lock();

			var bean = serviceTask.getById(taskId);
			switch (bean == null ? null : bean.getState())
			{
				case null -> throw new IllegalArgumentException("任务不存在");
				case Starting -> throw new IllegalStateException("任务正在启动");
				case Stopping -> throw new IllegalStateException("任务正在停止");
				case Running -> throw new IllegalStateException("任务正在运行");
				case ShutdownEnd, ErrorEnd, SuccessfulEnd -> throw new IllegalStateException("任务已经停止");
				default -> {
					updateState(taskId, TaskStateEnum.Starting);

					var thread = new MmdetectionTrainThread(taskId);
					mapContext.put(taskId, thread);
				}
			}
		}
		finally
		{
			GlobalLock.unlock();
		}

	}

	@Autowired
	TagMultiService serviceTag;

	private class MmdetectionTrainThread extends Thread
	{
		final String taskId;
		MmdetectionTrainThread(String taskId)
		{
			this.taskId = taskId;
		}

		NativeProcess process;
		int currentEpochY;
		@Override
		public void run() // 这个方法进来的时候 任务的状态已经是 Starting 了
		{
			TaskStateEnum stateFinal = TaskStateEnum.ErrorEnd;
			File folderTask = null;
			try
			{
				// 初始化上下文
				currentEpochY = 0;
				final TrainTaskBean task;
				final var taskModelTags = new HashSet<String>();
				try
				{
					GlobalLock.lock();

					task = serviceTask.getById(taskId);
					if(task == null) throw new IllegalArgumentException("任务不存在");
					folderTask = folderOfTask(taskId);

					taskModelTags.add("任务生成");
					taskModelTags.addAll(serviceTag.getTagValues(TagTypeEnum.TaskGenerateModelTag, taskId));
				}
				finally
				{
					GlobalLock.unlock();
				}

				var displayName = task.getDisplayName();
				final var config = task.getConfigValue();
				final var epochX = config.get("epochX").asInt();
				final var epochY = config.get("epochY").intValue();
				final var lr = new BigDecimal(config.get("lr").asText());
				final var momentum = new BigDecimal(config.get("momentum").asText());
				final var weightDecay = new BigDecimal(config.get("weightDecay").asText());

				var shouldContinue = true;
				// 任务循环主体
				while(shouldContinue)
				{
					currentEpochY++;

					addLog(taskId, LevelKeypoint, "第 %d 大轮开始".formatted(currentEpochY));

					// 决定本次要用的数据集
					//   目前数据集暂时不会变
					var currentDatasetId = task.getCurrentDatasetId();

					//   获取数据集和模型目录
					var folderDataset = serviceDatasetMulti.folderOfDataset(currentDatasetId);
					var fileDatasetAnnotations = serviceDatasetMulti.fileOfCocoDatasetAnnotation(folderDataset);
					var folderDatasetImages = serviceDatasetMulti.folderOfCocoDatasetImages(folderDataset);
					var folderWorkdir = folderOfMmdetectionTaskWorkdir(taskId);

					// 决定本次要用的模型
					var currentModelId =  switch (task.getProcessControlMethod())
					{
						case RoundX -> task.getInitModelId(); // 不变
						case Round1X, RoundXY -> {
							if(currentEpochY == 1)
								yield task.getInitModelId(); // 第一大轮的话就是初始模型
							// 根据当前大轮轮数获取应该用哪个模型
							var currentEpochX = currentEpochY * epochX;
							var modelName = generateModelNameOfTaskEpoch(taskId, displayName, currentEpochX);
							var listModel = serviceModel.list(
									new QueryWrapper<ModelBean>().lambda()
											.eq(ModelBean::getDisplayName, modelName)
							);
							if(listModel.isEmpty())
								throw new IllegalArgumentException("找不到所需模型文件");
							else if(listModel.size() > 1)
							{
								var listName = listModel.stream()
										.map(ModelBean::getDisplayName)
										.collect(Collectors.toList());
								throw new IllegalArgumentException("无法决定所用模型文件: " + String.join(",", listName));
							}
							var model = listModel.get(0);
							yield model.getId();
						} // 取最后生成的模型
					};
					var fileModel = serviceModelMulti.fileOfModel(currentModelId);
					task.setCurrentDatasetId(currentDatasetId);
					task.setCurrentModelId(currentModelId);

					try // 检查数据集和模型状态
					{
						GlobalLock.lock();

						var dataset = serviceDataset.getById(currentDatasetId);
						DatasetBean.checkForTraining(dataset);

						var model = serviceModel.getById(currentModelId);
						if(model == null) throw new IllegalStateException("模型不存在"); // todo low

						serviceTask.updateById(task);
					}
					finally
					{
						GlobalLock.unlock();
					}

					// 生成各种配置文件 启动任务
					//   创建 mmdetection 需要的配置文件
					var contentConfigPy = helper.generateTrainScript(
							fileModel,
							folderWorkdir,
							fileDatasetAnnotations,
							folderDatasetImages,
							taskId,
							displayName,
							currentModelId,
							currentDatasetId,
							lr,
							momentum,
							weightDecay,
							epochX
					);
					var fileConfig = fileOfMmdetectionTaskConfig(taskId);
					Files.writeTo(fileConfig, contentConfigPy);
					//   创建启动训练所需的批处理
					var fileBatch = fileOfTrainBatch(taskId);
					var contentBatch = """
                    %s
                    python "%s" "%s"
                    """.formatted(preScript, fileMmdetectionTrainPy, fileConfig);
					Files.writeTo(fileBatch, contentBatch);
					//   创建本地进程并等待其运行结束
					try(var process = new NativeProcess(fileBatch.getAbsolutePath()))
					{
						addLog(taskId, LevelKeypoint, "启动训练");
						process.waitFor();
						addLog(taskId, LevelKeypoint, "训练结束");
					}

					// 任务执行完一大轮 检查需要保存的模型
					var indexModels = switch (task.getStorageMethod())
					{
						case SaveEnd -> new int[] { epochX }; // 把所有生成的模型全保存到系统里
						case SaveAll -> {
							var ret = new int[epochX];
							for(var step = 0; step < epochX; step++)
								ret[step] = step + 1;
							yield ret;
						}
					};
					for(var indexModel : indexModels) // 复制文件
					{
						var fileModelSource = fileOfMmdetectionTaskWorkdirCheckpoint(taskId, indexModel);
						var modelName = generateModelNameOfTaskEpoch(taskId, displayName, indexModel + currentEpochY * epochX);
						var now = new Date();
						var bean = new ModelBean();
						bean.setDisplayName(modelName);
						bean.setModelType(FrameworkTypeEnum.Mmdetection);
						bean.setCreateUserId(""); // todo
						bean.setCreateTimestamp(now);
						bean.setSourceTaskId(taskId);

						String modelSaveId;
						try
						{
							GlobalLock.lock();
							serviceModel.save(bean);
							modelSaveId = bean.getId();
							serviceTag.setTagValues(modelSaveId, TagTypeEnum.ModelTag, taskModelTags);
						}
						finally
						{
							GlobalLock.unlock();
						}

						var fileModelTarget = serviceModelMulti.fileOfModel(modelSaveId);
						fileModelTarget.getParentFile().mkdirs();
						FileSystemUtils.copyRecursively(fileModelSource, fileModelTarget);
					}

					// 执行每一大轮的清理工作
					FileSystemUtils.deleteRecursively(folderWorkdir);
					FileSystemUtils.deleteRecursively(fileConfig);
					FileSystemUtils.deleteRecursively(fileBatch);

					addLog(taskId, LevelKeypoint, "第 %d 大轮完成".formatted(currentEpochY));

					// 决定是否还要继续下一轮
					shouldContinue = switch (task.getProcessControlMethod()) {
						case RoundX -> false;
						case Round1X -> true;
						case RoundXY -> currentEpochY == epochY;
					};
				}

				stateFinal = TaskStateEnum.SuccessfulEnd;
			}
			catch (InterruptedException stopSignal) // 用户手动停止
			{
				stateFinal = TaskStateEnum.ShutdownEnd;
			}
			catch (Exception any) // 遇到其它运行错误
			{
				stateFinal = TaskStateEnum.ErrorEnd;
			}
			finally // 任务执行完成 清理任务工作目录
			{
				updateState(taskId, TaskStateEnum.Stopping);
				// 删除工作目录里所有数据
				if(folderTask != null)
					FileSystemUtils.deleteRecursively(folderTask);

				// 至此 任务结束
				updateState(taskId, stateFinal);
			}
		}
	}

	/**
	 * 根据任务名称和轮数生成一个模型
	 * */
	public static String generateModelNameOfTaskEpoch(String taskId, String taskName, int epoch)
	{
		return "%s - %s - %d".formatted(taskId, taskName, epoch);
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
					var taskUpdate = new TrainTaskBean();
					taskUpdate.setId(taskId);
					taskUpdate.setState(TaskStateEnum.ShutdownEnd);
					serviceTask.updateById(taskUpdate);
					addLog(taskId, LevelKeypoint, "用户手动停止任务");

					var context = mapContext.get(taskId);
					if(context != null)
					{
						context.interrupt();
					}
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

	public void deleteTask(String taskId) throws Exception
	{
		try
		{
			GlobalLock.lock();

			var task = serviceTask.getById(taskId);
			if (Objects.requireNonNull(task.getState()) == TaskStateEnum.Running)
			{
				throw new IllegalStateException("任务正在运行, 无法删除");
			}

			var folderTask = folderOfTask(taskId);
			FileSystemUtils.deleteRecursively(folderTask);
			var folderWorkdir = folderOfMmdetectionTaskWorkdir(taskId);
			FileSystemUtils.deleteRecursively(folderWorkdir);
			serviceTask.removeById(taskId);
		}
		finally
		{
			GlobalLock.unlock();
		}
	}
}
