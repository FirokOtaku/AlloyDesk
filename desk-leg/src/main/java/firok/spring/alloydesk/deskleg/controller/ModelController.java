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
import jakarta.servlet.http.HttpServletResponse;
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
	 * ??????????????????
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

		// ?????????????????????????????? id
		var qwModelId = new QueryWrapper<ModelBean>().lambda()
				.select(ModelBean::getId)
				.like(keywordName != null && !keywordName.isBlank(), ModelBean::getDisplayName, keywordName);
		var setModelId = serviceModel.list(qwModelId).stream()
				.map(ModelBean::getId)
				.collect(Collectors.toSet());

		if(sizeOf(setModelId) == 0)
			return Ret.success(new SearchResult(paramPage, new HashMap<>()));

		final Set<String> setModelTagId;
		if(keywordTag != null && !keywordTag.isBlank()) // ??????????????????????????? tag
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

		// ??????????????????
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
	 * ????????????????????????
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
			return Ret.fail("???????????????????????????");
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
			return Ret.fail("????????????????????????");
		}

		return Ret.success();
	}

	/**
	 * ??????????????????
	 * */
	@Transactional(rollbackFor = Throwable.class)
	@GetMapping("/delete")
	public Ret<?> delete(
			@RequestParam("id") String id
	)
	{
		var bean = serviceModel.getById(id);
		if(bean == null)
			return Ret.fail("??????????????????");

		// todo ???????????????????????????
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
			return Ret.fail("??????????????????");
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
			HttpServletResponse response
	)
	{
		var uuid = UUID.randomUUID().toString(); // ??????????????????????????? id
		File folderTest = null;
		OutputStream os;
		try { os = response.getOutputStream(); }
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
				throw new IllegalArgumentException("??????????????????");
			var dataset = serviceDataset.getById(datasetId);
			if(dataset == null)
				throw new IllegalArgumentException("?????????????????????");
			var fileModel = serviceModelMulti.fileOfModel(modelId);
			if(!fileModel.exists())
				throw new IllegalArgumentException("??????????????????");

			var fileCoco = serviceDatasetMulti.fileOfCocoDatasetAnnotation(datasetId);
			var folderImage = serviceDatasetMulti.folderOfCocoDatasetImages(datasetId);

			// ????????????
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

			// ?????????????????????
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

			// ????????????????????????????????????
			try(var process = new NativeProcess(
					unixPathOf(fileBatch),
					System.out::println,
					System.err::println
			))
			{
				var result = process.waitFor();
				if(result != 0) throw new RuntimeException("???????????????????????????");
			}

			// ???????????????
			response.setStatus(HttpStatus.OK.value());
			// ??????????????????????????? ???????????????
			var om = new ObjectMapper();

			ozs.putNextEntry(new ZipEntry("index.json"));
			var jsonIndex = om.createObjectNode();
			jsonIndex.put("count", fileOutputs.length);
			ozs.write(jsonIndex.toString().getBytes(StandardCharsets.UTF_8));
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
			response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
			any.printStackTrace();
		}
		finally
		{
			if(folderTest != null)
				FileSystemUtils.deleteRecursively(folderTest);
		}
	}
}
