package firok.spring.alloydesk.deskleg.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import firok.spring.alloydesk.deskleg.controller.TrainTaskController;
import firok.tool.alloywrench.bean.CocoData;
import firok.topaz.general.RegexPipeline;
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

@Component
public class MmdetectionHelper
{
	@Value("${firok.spring.alloydesk.folder-mmdetection}")
	File folderMmdetection; // 主目录

	@Value("${firok.spring.alloydesk.mmdetection-pre-script}")
	String[] preScript; // 环境预执行脚本

	@Value("${firok.spring.alloydesk.mmdetection-base-model}")
	File fileBaseModel; // 基础模型文件


	String fileTrainPy; // 启动训练使用的脚本文件
	String fileBaseConfig; // 默认训练配置文件

	RegexPipeline pipeline;

	private static final @RegExp String PatternGenerateDatetime = "GENERATE_DATETIME";
	private static final @RegExp String PatternTaskId = "TASK_ID";
	private static final @RegExp String PatternTaskName = "TASK_NAME";
	private static final @RegExp String PatternUsedModel = "MODEL_ID";
	private static final @RegExp String PatternUsedDataset = "DATASET_ID";
	private static final @RegExp String PatternBaseConfig = "BASE_CONFIG";
	private static final @RegExp String PatternImagePrefix = "IMG_PREFIX";
	private static final @RegExp String PatternAnnoFile = "ANNO_FILE";
	private static final @RegExp String PatternClasses = "CLASSES";
	private static final @RegExp String PatternCountClasses = "COUNT_CLASSES";
	private static final @RegExp String PatternLr = "LR";
	private static final @RegExp String PatternMomentum = "MOMENTUM";
	private static final @RegExp String PatternWeightDecay = "WEIGHT_DECAY";
	private static final @RegExp String PatternMaxEpoch = "MAX_EPOCH";
	private static final @RegExp String PatternCheckpoint = "CHECKPOINT";
	private static final String TemplateMmdetectionConfig;
	static
	{
		var cpr = new ClassPathResource("firok/spring/alloydesk/deskleg/template-mmdetection.py");
		try(var ifs = cpr.getInputStream()) { TemplateMmdetectionConfig = new String(ifs.readAllBytes(), StandardCharsets.UTF_8); }
		catch (Exception any) { throw new RuntimeException(any); }
	}

	@SuppressWarnings("LanguageMismatch")
	@PostConstruct
	void postCons() throws Exception
	{
		fileTrainPy = new File(folderMmdetection, "tools/train.py").getCanonicalPath();
		fileBaseConfig = new File(folderMmdetection, "configs/mask_rcnn/mask_rcnn_r50_caffe_fpn_mstrain_1x_coco.py").getCanonicalPath();
		pipeline = new RegexPipeline();
		for(var pattern : new String[] {
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
		}) pipeline.getPattern(pattern);
	}

	public String generateTrainScript(
			@Nullable File fileModel,
			File fileCoco,
			File folderImage,
			String taskId,
			TrainTaskController.CreateTaskParam params,
			BigDecimal lr,
			BigDecimal momentum,
			BigDecimal weightDecay,
			int maxEpoch
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
		map.put(PatternGenerateDatetime, "unknown");
		map.put(PatternTaskId, taskId);
		map.put(PatternTaskName, params.displayName());
		map.put(PatternUsedModel, params.modelId());
		map.put(PatternUsedDataset, params.datasetId());
		map.put(PatternBaseConfig, fileBaseConfig);
		map.put(PatternImagePrefix, folderImage.getAbsolutePath());
		map.put(PatternAnnoFile, fileCoco.getAbsolutePath());
		map.put(PatternClasses, contentTypes);
		map.put(PatternCountClasses, String.valueOf(countTypes));
		map.put(PatternLr, lr.toPlainString());
		map.put(PatternMomentum, momentum.toPlainString());
		map.put(PatternWeightDecay, weightDecay.toPlainString());
		map.put(PatternMaxEpoch, String.valueOf(maxEpoch));
		map.put(PatternCheckpoint, fileModel.getAbsolutePath());
		return pipeline.replaceAll(TemplateMmdetectionConfig, map);
	}
}
