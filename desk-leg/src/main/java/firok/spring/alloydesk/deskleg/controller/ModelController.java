package firok.spring.alloydesk.deskleg.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import firok.spring.alloydesk.deskleg.bean.FrameworkTypeEnum;
import firok.spring.alloydesk.deskleg.bean.ModelBean;
import firok.spring.alloydesk.deskleg.bean.TagTypeEnum;
import firok.spring.alloydesk.deskleg.service_multi.TagMultiService;
import firok.topaz.spring.Ret;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.util.*;
import java.util.stream.Collectors;

import static firok.topaz.general.Collections.sizeOf;

@CrossOrigin
@RestController
@RequestMapping("/model")
public class ModelController
{
	@Autowired
	IService<ModelBean> serviceModel;
	@Autowired
	TagMultiService serviceTag;

	@Value("${firok.spring.alloydesk.folder-model}")
	File folderModelStorage;

	public record SearchResult(Page<ModelBean> page, Map<String, Set<String>> mapModelTags) { }
	/**
	 * 查询模型列表
	 * */
	@GetMapping("/search")
	public Ret<SearchResult> search(
			@RequestParam(value = "keywordName", required = false) String keywordName,
			@RequestParam(value = "keywordTag", required = false) String keywordTag,
			@RequestParam(value = "pageIndex", defaultValue = "1") int pageIndex,
			@RequestParam(value = "pageSize", defaultValue = "20") int pageSize
	)
	{
		var paramPage = new Page<ModelBean>(pageIndex, pageSize);

		var qwModelId = new QueryWrapper<ModelBean>().lambda()
				.select(ModelBean::getId)
				.like(ModelBean::getDisplayName, keywordName);
		var setModelId = serviceModel.list(qwModelId).stream()
				.map(ModelBean::getId)
				.collect(Collectors.toSet());

		if(sizeOf(setModelId) == 0)
			return Ret.success(new SearchResult(paramPage, new HashMap<>()));
		else
			return Ret.success(new SearchResult(paramPage, serviceTag.getTagValues(TagTypeEnum.ModelTag, setModelId)));
	}

	/**
	 * 手动上传一个模型
	 * */
	@SuppressWarnings("ResultOfMethodCallIgnored")
	@Transactional(rollbackFor = Throwable.class)
	@PostMapping("/upload")
	public Ret<?> upload(
			@RequestParam("name") String name,
			@RequestParam("tags") String[] tags,
			@RequestPart("file")MultipartFile file
			)
	{
		var id = UUID.randomUUID().toString();
		var now = new Date();
		var bean = new ModelBean();
		bean.setId(id);
		bean.setDisplayName(name);
		bean.setModelType(FrameworkTypeEnum.Mmdetection);
		bean.setCreateUserId(""); // todo
		bean.setCreateTimestamp(now);
		bean.setSourceTaskId(null);

		var fileModel = new File(folderModelStorage, id + ".model.bin");

		try(
				var ifs = file.getInputStream();
				var ofs = new FileOutputStream(fileModel)
		)
		{
			ifs.transferTo(ofs);
		}
		catch (Exception any)
		{
			return Ret.fail("无法保存模型文件");
		}

		try
		{
			serviceModel.save(bean);
			serviceTag.setTagValues(id, TagTypeEnum.ModelTag, Arrays.asList(tags));
		}
		catch (Exception any)
		{
			fileModel.delete();
			return Ret.fail("无法创建数据库记录");
		}

		return Ret.success();
	}

	/**
	 * 删除一个模型
	 * */
	@Transactional(rollbackFor = Throwable.class)
	@GetMapping("/delete")
	public Ret<?> delete(
			@RequestParam("id") String id
	)
	{
		var bean = serviceModel.getById(id);
		if(bean == null)
			return Ret.fail("不存在的模型");

		// 检查模型是否被占用
		// serviceTask.findAnyUsing(...)

		var fileModel = new File(folderModelStorage, id + ".model.bin");
		try
		{
			fileModel.delete();
			serviceModel.removeById(id);
			return Ret.success();
		}
		catch (Exception any)
		{
			TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
			return Ret.fail("删除模型出错");
		}
	}
}
