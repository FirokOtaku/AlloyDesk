package firok.spring.alloydesk.deskleg.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import firok.spring.alloydesk.deskleg.bean.*;
import firok.spring.alloydesk.deskleg.service_multi.TrainTaskMultiService;
import firok.topaz.spring.Ret;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

/**
 * @implNote 具体数据操作全在 multi service 里, 这个类不加锁
 * */
@RestController
@RequestMapping("/task")
@CrossOrigin
public class TrainTaskController
{
	@Autowired
	IService<TrainTaskBean> serviceRaw;
	@Autowired
	TrainTaskMultiService serviceMulti;
	@Autowired
	IService<TrainTaskLogBean> serviceLog;

	@PostConstruct
	void postCons()
	{
		// 每次启动的时候检查数据库
		// 如果数据库里有标记为正在运行的任务
		// 就把这个任务标记为失败

		// 因为有可能数据库还没初始化 这个可能出错 先不用管
		try
		{
			var setId = serviceMulti.findIdWithState(TaskStateEnum.Running);
			if(setId.isEmpty()) return;

			for(var taskId : setId)
				serviceMulti.addLog(taskId, TrainTaskMultiService.LevelKeypoint, "未正常结束, 强制停止");

			serviceMulti.updateStateByIds(setId, TaskStateEnum.ErrorEnd);
		}
		catch (Exception ignored) { }
	}
	@PreDestroy
	void preDes()
	{
		var setId = serviceMulti.findIdWithState(TaskStateEnum.Running);
		if(setId.isEmpty()) return;

		for(var taskId : setId)
			serviceMulti.addLog(taskId, TrainTaskMultiService.LevelKeypoint, "系统停止, 强制停止");

		serviceMulti.updateStateByIds(setId, TaskStateEnum.ErrorEnd);
	}

	public record CreateMmdetectionTaskParam(
			String displayName,
			String modelId,
			String datasetId,
			TaskModelStorageEnum modelStorageMethod,
			TaskProcessControlEnum processControlMethod,
			FrameworkTypeEnum frameworkType,
			String[] labels,
			String script,
			Integer epochX,
			Integer epochY,
			BigDecimal lr,
			BigDecimal momentum,
			BigDecimal weightDecay
	) { }
	@Transactional(rollbackFor = Throwable.class)
	@PostMapping("/create-mmdetection")
	public Ret<?> createMmdetection(
			@RequestBody CreateMmdetectionTaskParam params
	)
	{
		try
		{
			serviceMulti.createMmdetectionTask(params);
			return Ret.success();
		}
		catch (Exception any)
		{
			TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
			any.printStackTrace(System.err);
			return Ret.fail(any);
		}
	}

	@GetMapping("/list-all")
	public Ret<List<TrainTaskBean>> listAll(
			@RequestParam(value = "filterName", required = false) String filterName,
			@RequestParam(value = "filterState", required = false) List<TaskStateEnum> filterState
	)
	{
		var qw = new QueryWrapper<TrainTaskBean>().lambda()
				.like(filterName != null && !filterName.isBlank(), TrainTaskBean::getDisplayName, filterName)
				.in(filterState != null && !filterState.isEmpty(), TrainTaskBean::getState, filterState);
		return Ret.success(serviceRaw.list(qw));
	}

	@GetMapping("/shutdown")
	@Transactional(rollbackFor = Throwable.class)
	public Ret<?> shutdownTask(
			@RequestParam("taskId") String taskId
	)
	{
		try
		{
			serviceMulti.shutdownTask(taskId);
			return Ret.success();
		}
		catch (Exception any)
		{
			TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
			any.printStackTrace(System.err);
			return Ret.fail(any);
		}
	}

	@GetMapping("/get-log")
	public Ret<Page<TrainTaskLogBean>> listLog(
			@RequestParam("taskId") String taskId,
			@RequestParam(value = "level", defaultValue = "100") int level,
			@RequestParam(value = "pageSize", defaultValue = "50") int pageSize
	)
	{
		var qw = new QueryWrapper<TrainTaskLogBean>().lambda()
				.eq(TrainTaskLogBean::getTrainTaskId, taskId)
				.ge(TrainTaskLogBean::getLogLevel, level);
		var page = new Page<TrainTaskLogBean>(1, pageSize);
		return Ret.success(serviceLog.page(page, qw));
	}
}
