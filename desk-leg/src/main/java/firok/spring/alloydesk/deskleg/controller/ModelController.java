package firok.spring.alloydesk.deskleg.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.fasterxml.jackson.databind.ObjectMapper;
import firok.spring.alloydesk.deskleg.bean.*;
//import firok.spring.alloydesk.deskleg.mapper.ModelMultiMapper;
import firok.spring.alloydesk.deskleg.config.MmdetectionHelper;
import firok.spring.alloydesk.deskleg.service_multi.DatasetMultiService;
import firok.spring.alloydesk.deskleg.service_multi.ModelMultiService;
import firok.spring.alloydesk.deskleg.service_multi.TagMultiService;
import firok.topaz.function.MayConsumer;
import firok.topaz.platform.NativeProcess;
import firok.topaz.resource.Files;
import firok.topaz.spring.Ret;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static firok.topaz.general.Collections.sizeOf;
import static firok.topaz.resource.Files.unixPathOf;

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

	@Autowired
	ModelMultiService serviceModelMulti;

	@Autowired
	DatasetMultiService serviceDatasetMulti;

	@Autowired
	IService<DatasetBean> serviceDataset;

	@Autowired
	MmdetectionHelper helper;

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
			fileModel = serviceModelMulti.fileOfModel(id);
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

		// todo 检查模型是否被占用
		// serviceTask.findAnyUsing(...)

		try
		{
			var fileModel = serviceModelMulti.fileOfModel(id);
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

	@Value("${firok.spring.alloydesk.mmdetection-pre-script}")
	String preScript;

	@SuppressWarnings("ResultOfMethodCallIgnored")
	@PostMapping("/test")
	public void test(
			@RequestPart("modelId") String modelId,
			@RequestParam("datasetId") String datasetId,
			@RequestPart("files") MultipartFile[] files,
			ServletServerHttpResponse response
	)
	{
		var uuid = UUID.randomUUID().toString(); // 本次测试任务的唯一 id
		File folderTest = null;
		OutputStream os;
		try { os = response.getBody(); }
		catch (Exception any) { throw new RuntimeException(any); }

		try(
				os;
				var ozs = new ZipOutputStream(os)
		)
		{
			folderTest = serviceModelMulti.folderOfModelTest(uuid);
			folderTest.mkdirs();

			var model = serviceModel.getById(modelId);
			if(model == null)
				throw new IllegalArgumentException("不存在的模型");
			var dataset = serviceDataset.getById(datasetId);
			if(dataset == null)
				throw new IllegalArgumentException("不存在的数据集");
			var fileModel = serviceModelMulti.fileOfModel(modelId);
			if(!fileModel.exists())
				throw new IllegalArgumentException("模型文件丢失");

			var fileCoco = serviceDatasetMulti.fileOfCocoDatasetAnnotation(datasetId);
			var folderImage = serviceDatasetMulti.folderOfCocoDatasetImages(datasetId);

			// 转移文件
			var fileInputs = new File[files.length];
			var fileOutputs = new File[files.length];
			for(var step = 0; step < files.length; step++)
			{
				var fileRaw = files[step];
				var fileInput = new File(folderTest, step + ".input.png");
				var fileOutput = new File(folderTest, step + ".output.png");
				fileInput.createNewFile();
				fileRaw.transferTo(fileInput);
				fileInputs[step] = fileInput;
				fileOutputs[step] = fileOutput;
			}

			// 生成测试用脚本
			var contentTrainScript = helper.generateTrainScript(
					fileModel,
					folderTest,
					fileCoco,
					folderImage,
					uuid,
					"test-task-" + uuid,
					modelId,
					datasetId,
					BigDecimal.ZERO,
					BigDecimal.ZERO,
					BigDecimal.ZERO,
					12
			);
			var fileTrainScript = new File(folderTest, "config.py");
			Files.writeTo(fileTrainScript, contentTrainScript);
			var contentTestScript = helper.generateTestScript(
					fileModel,
					fileTrainScript,
					fileInputs,
					fileOutputs
			);
			var fileTestScript = new File(folderTest, "test.py");
			Files.writeTo(fileTestScript, contentTestScript);
			var contentBatch = """
                    %s
                    python "%s"
                    """.formatted(preScript, unixPathOf(fileTestScript));
			var fileBatch = new File(folderTest, "run.bat");
			Files.writeTo(fileBatch, contentBatch);

			// 开始调用本地进程执行测试
			try(var process = new NativeProcess(
					unixPathOf(fileBatch),
					System.out::println,
					System.err::println
			))
			{
				var result = process.waitFor();
				if(result != 0) throw new RuntimeException("测试进程未正常结束");
			}

			// 写入响应头
			response.setStatusCode(HttpStatus.OK);
			// 读取执行出来的结果 返回给前台
			var om = new ObjectMapper();

			ozs.putNextEntry(new ZipEntry("index.json"));
			var jsonIndex = om.createObjectNode();
			jsonIndex.put("count", fileOutputs.length);
			om.writeValue(ozs, jsonIndex);
			ozs.closeEntry();

			for(var step = 0; step < fileOutputs.length; step++)
			{
				try(var ifs = new FileInputStream(fileOutputs[step]))
				{
					ozs.putNextEntry(new ZipEntry(String.valueOf(step)));
					ifs.transferTo(ozs);
					ozs.closeEntry();
				}
			}

			ozs.flush();
			os.flush();
		}
		catch (Exception any)
		{
			response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
			any.printStackTrace();
		}
		finally
		{
			if(folderTest != null)
				FileSystemUtils.deleteRecursively(folderTest);
		}
	}
}
