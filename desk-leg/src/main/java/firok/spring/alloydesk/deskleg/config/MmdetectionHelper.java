package firok.spring.alloydesk.deskleg.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import firok.spring.alloydesk.deskleg.controller.TrainTaskController;
import firok.tool.alloywrench.bean.CocoData;
import firok.topaz.general.RegexPipeline;
import firok.topaz.resource.Files;
import jakarta.annotation.PostConstruct;
import org.intellij.lang.annotations.RegExp;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static firok.topaz.resource.Files.unixPathOf;

@Component
public class MmdetectionHelper
{
	@Value("${firok.spring.alloydesk.folder-mmdetection}")
	File folderMmdetection; // 主目录

//	@Value("${firok.spring.alloydesk.mmdetection-pre-script}")
//	String preScript; // 环境预执行脚本

	@Value("${firok.spring.alloydesk.mmdetection-base-model}")
	File fileBaseModel; // 基础模型文件


	String fileTrainPy; // 启动训练使用的脚本文件
	String fileBaseConfig; // 默认训练配置文件

	RegexPipeline pipeline;

	private static final @RegExp String PatternWorkDir = "WORK_DIR";
	private static final @RegExp String PatternGenerateDatetime = "GENERATE_DATETIME";
	private static final @RegExp String PatternTaskId = "TASK_ID";
	private static final @RegExp String PatternTaskName = "TASK_NAME";
	private static final @RegExp String PatternUsedModel = "MODEL_ID";
	private static final @RegExp String PatternUsedDataset = "DATASET_ID";
	private static final @RegExp String PatternBaseConfig = "BASE_CONFIG";
	private static final @RegExp String PatternImagePrefix = "IMG_PREFIX";
	private static final @RegExp String PatternAnnoFile = "ANNO_FILE";
	private static final @RegExp String PatternClasses = "CLASSES";
	private static final @RegExp String PatternCountClasses = "COUNT_TYPES";
	private static final @RegExp String PatternLr = "LR";
	private static final @RegExp String PatternMomentum = "MOMENTUM";
	private static final @RegExp String PatternWeightDecay = "WEIGHT_DECAY";
	private static final @RegExp String PatternMaxEpoch = "MAX_EPOCH";
	private static final @RegExp String PatternCheckpoint = "CHECKPOINT";
	private static final String TemplateMmdetectionConfig;

	private static final @RegExp String PatternModelFile = "MODEL_FILE";
	private static final @RegExp String PatternConfigFile = "CONFIG_FILE";
	private static final @RegExp String PatternTestBatch = "TEST_BATCH";
	private static final String TemplateMmdetectionTest;
	static
	{
		var cprTrain = new ClassPathResource("firok/spring/alloydesk/deskleg/template-mmdetection-train.py");
		try(var ifs = cprTrain.getInputStream()) { TemplateMmdetectionConfig = new String(ifs.readAllBytes(), StandardCharsets.UTF_8); }
		catch (Exception any) { throw new RuntimeException(any); }
		var cprTest = new ClassPathResource("firok/spring/alloydesk/deskleg/template-mmdetection-test.py");
		try(var ifs = cprTest.getInputStream()) { TemplateMmdetectionTest = new String(ifs.readAllBytes(), StandardCharsets.UTF_8); }
		catch (Exception any) { throw new RuntimeException(any); }
	}

	@SuppressWarnings("LanguageMismatch")
	@PostConstruct
	void postCons() throws Exception
	{
		fileTrainPy = unixPathOf(new File(folderMmdetection, "tools/train.py"));
		fileBaseConfig = unixPathOf(new File(folderMmdetection, "configs/mask_rcnn/mask_rcnn_r50_caffe_fpn_mstrain_1x_coco.py"));
		pipeline = new RegexPipeline();
		for(var pattern : new String[] {
				PatternWorkDir,
				PatternGenerateDatetime,
				PatternTaskId,
				PatternTaskName,
				PatternUsedModel,
				PatternUsedDataset,
				PatternBaseConfig,
				PatternImagePrefix,
				PatternAnnoFile,
				PatternClasses,
				PatternCountClasses,
				PatternLr,
				PatternMomentum,
				PatternWeightDecay,
				PatternMaxEpoch,
				PatternCheckpoint,

				PatternModelFile,
				PatternConfigFile,
				PatternTestBatch,
		}) pipeline.getPattern(pattern);
	}

	/**
	 * 生成训练模型所需脚本
	 * */
	public String generateTrainScript(
			@Nullable File fileModel,
			File folderWorkdir,
			File fileCoco,
			File folderImage,
			String taskId,
			String displayName,
			String modelId,
			String datasetId,
			BigDecimal lr,
			BigDecimal momentum,
			BigDecimal weightDecay,
			int epoch
	) throws Exception
	{
		if(fileModel == null) fileModel = fileBaseModel;

		var om = new ObjectMapper();
		// 读取 coco 数据
		var coco = om.readValue(fileCoco, CocoData.class);

		final var countTypes = coco.getCategories().size();
		final var setTypes = coco.getCategories().stream().map(CocoData.Category::getName).collect(Collectors.toSet());
		var sbTypes = new StringBuilder();
		var iterTypes = setTypes.iterator();
		while(iterTypes.hasNext())
		{
			var type = iterTypes.next();
			sbTypes.append('\'').append(type).append('\'');
			if(iterTypes.hasNext()) sbTypes.append(',');
		}
		final var contentTypes = sbTypes.toString();

		// 最后的生成工作
		var map = new HashMap<String, String>();
		map.put(PatternWorkDir, unixPathOf(folderWorkdir));
		//noinspection deprecation
		map.put(PatternGenerateDatetime, new Date().toLocaleString());
		map.put(PatternTaskId, taskId);
		map.put(PatternTaskName, displayName);
		map.put(PatternUsedModel, modelId);
		map.put(PatternUsedDataset, datasetId);
		map.put(PatternBaseConfig, fileBaseConfig);
		map.put(PatternImagePrefix, unixPathOf(folderImage));
		map.put(PatternAnnoFile, unixPathOf(fileCoco));
		map.put(PatternClasses, contentTypes);
		map.put(PatternCountClasses, String.valueOf(countTypes));
		map.put(PatternLr, lr.toPlainString());
		map.put(PatternMomentum, momentum.toPlainString());
		map.put(PatternWeightDecay, weightDecay.toPlainString());
		map.put(PatternMaxEpoch, String.valueOf(epoch));
		map.put(PatternCheckpoint, unixPathOf(fileModel));
		return pipeline.replaceAll(TemplateMmdetectionConfig, map);
	}

	/**
	 * 生成测试模型所需脚本
	 * */
	public String generateTestScript(
			File fileModel,
			File fileConfig,
			File[] fileInputs,
			File[] fileOutputs
	) throws Exception
	{
		var testBatch = new StringBuilder();
		final int count = fileInputs.length;
		if(fileInputs.length != fileOutputs.length)
			throw new IllegalArgumentException("");

		for(var step = 0; step < count; step++)
		{
			var fileInput = fileInputs[step];
			var fileOutput = fileOutputs[step];
			testBatch.append("""
                    test('%s', '%s')
                    """.formatted(unixPathOf(fileInput), unixPathOf(fileOutput)));
			testBatch.append('\n');
		}

		var map = new HashMap<String, String>();
		map.put(PatternConfigFile, unixPathOf(fileConfig));
		map.put(PatternModelFile, unixPathOf(fileModel));
		map.put(PatternTestBatch, testBatch.toString());
		return pipeline.replaceAll(TemplateMmdetectionTest, map);
	}
}
