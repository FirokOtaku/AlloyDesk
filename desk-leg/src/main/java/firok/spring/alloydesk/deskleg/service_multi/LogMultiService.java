package firok.spring.alloydesk.deskleg.service_multi;

import com.baomidou.mybatisplus.extension.service.IService;
import firok.spring.alloydesk.deskleg.bean.LogBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class LogMultiService
{
	@Autowired
	IService<LogBean> service;

	@Value("${firok.spring.alloydesk.logger.print-console}")
	boolean shouldPrintConsole;

	private final ReentrantLock LocalLock = new ReentrantLock();
	private final List<LogBean> pool = new LinkedList<>();

	@Deprecated
	public void log(String type, String value)
	{
		var now = new Date();
		var bean = new LogBean();
		bean.setId(UUID.randomUUID().toString());
		bean.setCreateTimestamp(now);
		bean.setCreateUserId(""); // todo
		bean.setLogType(type);
		bean.setLogInfo(value);

		if(shouldPrintConsole)
			System.out.println(type + " | " + value);

		LocalLock.lock();
		pool.add(bean);
		LocalLock.unlock();
	}

	/**
	 * 接口调用
	 * */
	public void apiCall(String value)
	{
		log("api-call", value);
	}

	/**
	 * 训练任务生命周期
	 * */
	public void taskLifecycle(String value)
	{
		log("task-lifecycle", value);
	}

	/**
	 * 数据维护工作
	 * */
	public void systemMaintenance(String value)
	{
		log("system-maintenance", value);
	}

	/**
	 * 系统关键节点
	 * */
	public void systemKeypoint(String value)
	{
		log("system-keypoint", value);
	}

	@Scheduled(initialDelay = 5000, fixedRate = 10000)
	public void flush()
	{
		LocalLock.lock();
		var caches = new ArrayList<>(pool);
		pool.clear();
		LocalLock.unlock();

		service.saveBatch(caches);
	}
}
