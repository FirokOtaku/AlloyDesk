package firok.spring.alloydesk.deskleg.bean;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.databind.JsonNode;
import firok.spring.dbsculptor.Dubnium;
import firok.spring.mvci.MVCIntrospective;
import lombok.Data;

/**
 * mmdetection 训练任务信息
 * */
@Data
@MVCIntrospective
@TableName(value = "d_train_task", autoResultMap = true)
@Dubnium(sculpturalScript = """
create table if not exists d_train_task (
    id varchar(48) not null,
    create_timestamp timestamp not null,
    create_user_id varchar(48) not null,
    
    display_name varchar(48),
    state varchar(16),
    init_model_id varchar(48),
    current_model_id varchar(48),
    init_dataset_id varchar(48),
	current_dataset_id varchar(48),
    storage_method varchar(16),
    process_control_method varchar(16),
    start_control_method varchar(16),
    framework_type varchar(32),
    config_value json,
    
    primary key(id)
);
""")
public class TrainTaskBean extends BaseBean
{
	private String displayName;
	/**
	 * 任务状态
	 * */
	private TaskStateEnum state;

	/**
	 * 训练开始时使用的模型 id
	 * */
	private String initModelId;

	/**
	 * 训练任务当前使用的模型 id
	 * */
	private String currentModelId;

	/**
	 * 训练任务使用的模型 id
	 * */
	private String initDatasetId;
	/**
	 * 当前使用的数据集 id
	 * */
	private String currentDatasetId;

	private TaskModelStorageEnum storageMethod;

	private TaskProcessControlEnum processControlMethod;

	private TaskStartControlEnum startControlMethod;

	/**
	 * 框架类型
	 * */
	private FrameworkTypeEnum frameworkType;

	/**
	 * 训练配置项
	 * */
	@TableField(typeHandler = JacksonTypeHandler.class)
	private JsonNode configValue;

}
