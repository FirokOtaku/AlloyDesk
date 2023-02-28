package firok.spring.alloydesk.deskleg.controller;

import com.baomidou.mybatisplus.extension.service.IService;
import firok.spring.alloydesk.deskleg.bean.DataSourceBean;
import firok.topaz.spring.Ret;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

@CrossOrigin
@RequestMapping("/data-source")
@RestController
public class DataSourceController
{
	@Autowired
	IService<DataSourceBean> service;

	/**
	 * 获取所有数据源信息
	 * */
	@GetMapping("/list-all")
	public Ret<List<DataSourceBean>> listDataSource()
	{
		var ret = service.list();
		return Ret.success(ret);
	}

	@Autowired
	ReentrantLock GlobalLock;

	/**
	 * 创建一个数据源
	 * */
	@GetMapping("/create")
	@Transactional(rollbackFor = Throwable.class)
	public Ret<DataSourceBean> createDataSource(
			@RequestParam("name") String name,
			@RequestParam("url") String url,
			@RequestParam("token") String token,
			@RequestParam("desc") String description
	)
	{
		var bean = new DataSourceBean();
		bean.setUrl(url);
		bean.setToken(token);
		bean.setDescription(description);
		bean.setNameDisplay(name);
		bean.setId(UUID.randomUUID().toString());
		bean.setCreateTimestamp(new Date());
		bean.setCreateUserId("");

		try
		{
			GlobalLock.lock();
			service.save(bean);
		}
		catch (Exception any)
		{
			return Ret.fail(any);
		}
		finally
		{
			GlobalLock.unlock();
		}

		return Ret.success(bean);
	}

	@GetMapping("/delete")
	@Transactional(rollbackFor = Throwable.class)
	public Ret<?> deleteDataSource(
			@RequestParam("id") String id
	)
	{
		try
		{
			GlobalLock.lock();
			service.removeById(id);
		}
		catch (Exception any)
		{
			return Ret.fail(any);
		}
		finally
		{
			GlobalLock.unlock();
		}

		return Ret.success();
	}

	/**
	 * 更新所有数据源
	 * */
	@PostMapping("/update")
	@Transactional(rollbackFor = Throwable.class)
	public Ret<DataSourceBean> update(
			@RequestBody DataSourceBean bean
	)
	{
		var beanUpdate = new DataSourceBean();
		beanUpdate.setId(bean.getId());
		beanUpdate.setDescription(bean.getDescription());
		beanUpdate.setToken(bean.getToken());
		beanUpdate.setUrl(bean.getUrl());
		beanUpdate.setNameDisplay(bean.getNameDisplay());
		try
		{
			GlobalLock.lock();
			service.updateById(bean);
		}
		catch (Exception any)
		{
			return Ret.fail(any);
		}
		finally
		{
			GlobalLock.unlock();
		}
		return Ret.success(bean);
	}
}
