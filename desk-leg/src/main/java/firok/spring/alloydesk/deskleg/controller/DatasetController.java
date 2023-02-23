package firok.spring.alloydesk.deskleg.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import firok.spring.alloydesk.deskleg.bean.DataSourceBean;
import firok.spring.alloydesk.deskleg.bean.DatasetBean;
import firok.spring.alloydesk.deskleg.bean.DatasetStatusEnum;
import firok.tool.labelstudio.bean.ProjectBean;
import firok.topaz.spring.Ret;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@CrossOrigin
@RestController
@RequestMapping("/dataset")
public class DatasetController
{
	@Autowired
	IService<DataSourceBean> serviceSource;
	@Autowired
	IService<DatasetBean> serviceDataset;

	/**
	 * 获取所有数据集
	 * */
	@GetMapping("/list-all")
	public Ret<Page<DatasetBean>> listAll(
			@RequestParam(value = "pageIndex", defaultValue = "1") int pageIndex,
			@RequestParam(value = "pageSize", defaultValue = "20") int pageSize
	)
	{
		var page = serviceDataset.page(
				new Page<>(pageIndex, pageSize),
				new QueryWrapper<>()
		);
		return Ret.success(page);
	}

	@GetMapping("/list-all-raw")
	public Ret<firok.tool.labelstudio.bean.Page<ProjectBean>> listAllRaw(
			@RequestParam("sourceId") String sourceId
	)
	{
		try
		{
			var source = serviceSource.getById(sourceId);
			var conn = source.getConnector();
			var projects = conn.Projects.listProjects(null, null, 1, Integer.MAX_VALUE);
			return Ret.success(projects);
		}
		catch (Exception any)
		{
			return Ret.fail(any);
		}
	}

	public record PullDatasetParam(String nameDisplay, String description) { }
	@PostMapping("/pull")
	@Transactional(rollbackFor = Throwable.class)
	public Ret<DatasetBean> pull(
			@RequestParam("sourceId") String sourceId,
			@RequestParam("projectId") Long projectId,
			@RequestBody PullDatasetParam params
	)
	{
		try
		{
			var source = serviceSource.getById(sourceId);
			var conn = source.getConnector();
			var project = conn.Projects.getProjectById(projectId);

			var now = new Date();

			var bean = new DatasetBean();
			bean.setPullSourceId(source.getId());
			bean.setPullSourceName(source.getNameDisplay());
			bean.setPullSourceProjectId(projectId);
			bean.setPullSourceProjectName(project.getTitle());
			bean.setPullTimestamp(now);
			bean.setCreateTimestamp(now);
			bean.setCreateUserId(""); // todo
			bean.setPictureCount(-1L);
			bean.setAnnotationCount(-1L);
			bean.setStatus(DatasetStatusEnum.Pulling);
			bean.setId(UUID.randomUUID().toString());
			bean.setNameDisplay(params.nameDisplay);
			bean.setDescription(params.description);
			serviceDataset.save(bean);

			return Ret.success(bean);
		}
		catch (Exception any)
		{
			TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
			return Ret.fail(any);
		}
	}

	/**
	 * 删除一个数据集
	 * */
	@Transactional(rollbackFor = Throwable.class)
	@GetMapping("/delete")
	public Ret<?> delete(
			@RequestParam String id
	)
	{
		try
		{
			var bean = serviceDataset.getById(id);
			switch (bean.getStatus()) // 对的 就是让 switch 顺着往下执行
			{
				// 停止任务
				case Pulling:


				// 删除文件
				case Broken:
				case Logical:
				case Ready:

					break;

				// 不能删除
				case Occupied:
					throw new IllegalArgumentException("数据集正被使用中");
				default:
					throw new IllegalArgumentException("数据集状态异常");
			}

			return Ret.success();
		}
		catch (Exception any)
		{
			TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
			return Ret.fail(any);
		}
	}
}
