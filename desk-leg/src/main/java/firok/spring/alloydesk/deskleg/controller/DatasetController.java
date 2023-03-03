package firok.spring.alloydesk.deskleg.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.fasterxml.jackson.databind.ObjectMapper;
import firok.spring.alloydesk.deskleg.bean.DataSourceBean;
import firok.spring.alloydesk.deskleg.bean.DatasetBean;
import firok.spring.alloydesk.deskleg.bean.DatasetStatusEnum;
import firok.spring.alloydesk.deskleg.service_multi.DatasetMultiService;
import firok.tool.alloywrench.bean.CocoData;
import firok.tool.labelstudio.LabelStudioConnector;
import firok.tool.labelstudio.bean.ProjectBean;
import firok.topaz.spring.Ret;
import jakarta.annotation.PostConstruct;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

import static firok.topaz.general.Collections.*;
import static firok.topaz.general.Enums.*;

@CrossOrigin
@RestController
@RequestMapping("/dataset")
public class DatasetController
{
	@Autowired
	IService<DataSourceBean> serviceSource;
	@Autowired
	IService<DatasetBean> serviceDataset;

	@Autowired
	ReentrantLock GlobalLock;

	/**
	 * 获取所有数据集
	 * */
	@GetMapping("/list-all")
	public Ret<Page<DatasetBean>> listAll(
			@RequestParam(value = "name",required = false) String filterName,
			@RequestParam(value = "status",required = false) String[] filterStatuses,
			@RequestParam(value = "pageIndex", defaultValue = "1") int pageIndex,
			@RequestParam(value = "pageSize", defaultValue = "3") int pageSize
	)
	{
		var page = serviceDataset.page(
				new Page<>(pageIndex, pageSize),
				new QueryWrapper<DatasetBean>().lambda()
						.like(filterName != null && !filterName.isBlank(), DatasetBean::getNameDisplay, filterName)
						.in(filterStatuses != null && filterStatuses.length > 0, DatasetBean::getStatus, setOf(DatasetStatusEnum.class, filterStatuses))
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

	@Autowired
	DatasetMultiService serviceDatasetMulti;

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
			GlobalLock.lock();

			var source = serviceSource.getById(sourceId);
			var conn = source.getConnector();
			var project = conn.Projects.getProjectById(projectId);

			var now = new Date();

			var datasetId = UUID.randomUUID().toString();
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
			bean.setId(datasetId);
			bean.setNameDisplay(params.nameDisplay);
			bean.setDescription(params.description);
			serviceDataset.save(bean);

			var folderDataset = serviceDatasetMulti.folderOfDataset(datasetId);
			// 启动拉取
			new PullThread(conn, project, datasetId, folderDataset).start();

			return Ret.success(bean);
		}
		catch (Exception any)
		{
			TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
			return Ret.fail(any);
		}
		finally
		{
			GlobalLock.unlock();
		}
	}

	private class PullThread extends Thread
	{
		private final LabelStudioConnector conn;
		private final ProjectBean project;
		private final File folder;
		private final String datasetId;
		PullThread(LabelStudioConnector conn, ProjectBean project, String datasetId, File folder)
		{
			setDaemon(true);
			this.conn = conn;
			this.project = project;
			this.folder = folder;
			this.datasetId = datasetId;
		}

		@Override
		public void run()
		{
			var bean = new DatasetBean();
			bean.setId(datasetId);
			try
			{
				conn.AdvancedExport.exportFullCocoDataset(project.getId(), folder);
				var fileCoco = serviceDatasetMulti.fileOfCocoDatasetAnnotation(folder);
				var om = new ObjectMapper();
				var coco = om.readValue(fileCoco, CocoData.class);
				var imageCount = sizeOf(coco.getImages());
				var annotationCount = sizeOf(coco.getAnnotations());
				bean.setPictureCount((long) imageCount);
				bean.setAnnotationCount((long) annotationCount);
				bean.setStatus(DatasetStatusEnum.Ready);
			}
			catch (Exception any)
			{
				System.out.println("拉取数据集出错");
				any.printStackTrace(System.err);
				bean.setStatus(DatasetStatusEnum.Broken);
			}

			try
			{
				GlobalLock.lock();
				serviceDataset.updateById(bean);
			}
			finally
			{
				GlobalLock.unlock();
			}
		}
	}

	// 把没拉取完的数据集标记为损坏
	@PostConstruct
	void updateUnfinishedDataset()
	{
		try
		{
			var uw = new UpdateWrapper<DatasetBean>().lambda()
					.set(DatasetBean::getStatus, DatasetStatusEnum.Broken)
					.eq(DatasetBean::getStatus, DatasetStatusEnum.Pulling);
			serviceDataset.update(uw);
		}
		catch (Exception ignored) { }
	}

	/**
	 * 删除一个数据集
	 * */
	@Transactional(rollbackFor = Throwable.class)
	@GetMapping("/delete")
	public Ret<?> delete(
			@RequestParam("id") String id
	)
	{
		try
		{
			GlobalLock.lock();
			var bean = serviceDataset.getById(id);
			if(bean == null)
				throw new IllegalArgumentException("不存在的数据集");
			var hasDelete = serviceDataset.removeById(id);
			if(!hasDelete)
				throw new IllegalArgumentException("删除数据库记录出错");

			switch (bean.getStatus()) // 对的 就是让 switch 顺着往下执行
			{
				// 删除文件
				case Broken:
				case Logical:
				case Ready:
					serviceDatasetMulti.deleteDataset(id);
					break;

				// 停止任务
				case Pulling:
					throw new IllegalArgumentException("仍在拉取");
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
		finally
		{
			GlobalLock.unlock();
		}
	}
}
