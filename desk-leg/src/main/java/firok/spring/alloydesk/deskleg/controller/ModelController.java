package firok.spring.alloydesk.deskleg.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import firok.spring.alloydesk.deskleg.bean.FrameworkTypeEnum;
import firok.spring.alloydesk.deskleg.bean.ModelBean;
import firok.spring.alloydesk.deskleg.bean.TagBean;
import firok.spring.alloydesk.deskleg.bean.TagTypeEnum;
//import firok.spring.alloydesk.deskleg.mapper.ModelMultiMapper;
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
import java.io.IOException;
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

	@Autowired
	IService<TagBean> serviceTagRaw;

//	@Autowired
//	ModelMultiMapper mapper;

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

		// 根据名称过滤模型查询 id
		var qwModelId = new QueryWrapper<ModelBean>().lambda()
				.select(ModelBean::getId)
				.like(keywordName != null && !keywordName.isBlank(), ModelBean::getDisplayName, keywordName);
		var setModelId = serviceModel.list(qwModelId).stream()
				.map(ModelBean::getId)
				.collect(Collectors.toSet());

		if(sizeOf(setModelId) == 0)
			return Ret.success(new SearchResult(paramPage, new HashMap<>()));

		final Set<String> setModelTagId;
		if(keywordTag != null && !keywordTag.isBlank()) // 查询这些模型有什么 tag
		{
			var qwModelTagId = new QueryWrapper<TagBean>().lambda()
					.select(TagBean::getTargetId)
					.eq(TagBean::getTagType, TagTypeEnum.ModelTag)
					.like(TagBean::getTagValue, keywordTag)
					.in(TagBean::getTargetId, setModelId);
			setModelTagId = serviceTagRaw.list(qwModelTagId).stream()
					.map(TagBean::getTargetId)
					.collect(Collectors.toSet());
		}
		else
			setModelTagId = setModelId;


		if(sizeOf(setModelTagId) == 0)
			return Ret.success(new SearchResult(paramPage, new HashMap<>()));

		// 查询最终结果
		var qwFinal = new QueryWrapper<ModelBean>().lambda()
				.in(ModelBean::getId, setModelTagId);
		var finalPage = serviceModel.page(paramPage, qwFinal);
		return Ret.success(new SearchResult(finalPage, serviceTag.getTagValues(TagTypeEnum.ModelTag, setModelTagId)));

//		var page = new Page<ModelBean>(pageIndex, pageSize);
//		mapper.searchModel(page, keywordName, keywordTag);
//		var setId = page.getRecords().stream().map(ModelBean::getId).collect(Collectors.toSet());
//		var map = serviceTag.getTagValues(TagTypeEnum.ModelTag, setId);
//		return Ret.success(new SearchResult(page, map));
	}

	/**
	 * 手动上传一个模型
	 * */
	@SuppressWarnings("ResultOfMethodCallIgnored")
	@Transactional(rollbackFor = Throwable.class)
	@PostMapping("/upload")
	public Ret<?> upload(
			@RequestParam("name") String name,
			@RequestParam(value = "tags", required = false) String[] tags,
			@RequestPart("file")MultipartFile file
			)
	{
		var now = new Date();
		var bean = new ModelBean();
		bean.setDisplayName(name);
		bean.setModelType(FrameworkTypeEnum.Mmdetection);
		bean.setCreateUserId(""); // todo
		bean.setCreateTimestamp(now);
		bean.setSourceTaskId(null);

		final String id;
		try
		{
			serviceModel.save(bean);
			id = bean.getId();
			serviceTag.setTagValues(id, TagTypeEnum.ModelTag, tags);
		}
		catch (Exception any)
		{
			TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
			return Ret.fail("无法创建数据库记录");
		}

		File fileModel = null;
		try
		{
			fileModel = new File(folderModelStorage, id + ".model.bin").getCanonicalFile();
			fileModel.getParentFile().mkdirs();
			fileModel.createNewFile();
			file.transferTo(fileModel);
		}
		catch (IOException any)
		{
			var ignored = fileModel != null ? fileModel.delete() : null;
			return Ret.fail("无法保存模型文件");
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
			serviceTag.setTagValues(id, TagTypeEnum.ModelTag);
			return Ret.success();
		}
		catch (Exception any)
		{
			TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
			return Ret.fail("删除模型出错");
		}
	}
}
