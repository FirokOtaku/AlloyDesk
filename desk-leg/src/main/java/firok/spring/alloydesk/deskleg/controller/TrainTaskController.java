package firok.spring.alloydesk.deskleg.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import firok.spring.alloydesk.deskleg.bean.*;
import firok.spring.alloydesk.deskleg.service_multi.TrainTaskMultiService;
import firok.topaz.spring.Ret;
import firok.topaz.thread.Threads;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.bind.annotation.*;

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

	public record CreateTaskParam(
			String displayName,
			String modelId,
			String datasetId,
			TaskModelStorageEnum modelStorageMethod,
			TaskProcessControlEnum processControlMethod,
			FrameworkTypeEnum frameworkType,
			String[] labels,
			String script,
			Integer epochX,
			Integer epochY
	) { }
	@Transactional(rollbackFor = Throwable.class)
	@PostMapping("/create")
	public Ret<?> create(
			@RequestBody CreateTaskParam params
	)
	{
		try
		{
			serviceMulti.createTask(params);
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
