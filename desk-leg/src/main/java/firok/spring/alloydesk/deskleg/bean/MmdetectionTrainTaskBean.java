package firok.spring.alloydesk.deskleg.bean;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.JsonNode;
import firok.spring.dbsculptor.Dubnium;
import firok.spring.mvci.MVCIntrospective;
import lombok.Data;

import java.util.*;

/**
 * mmdetection 训练任务信息
 * */
@Data
@MVCIntrospective
@TableName("d_mmdetection_train_task")
@Dubnium(sculpturalScript = """
        create table if not exists d_mmdetection_train_task (
            id varchar(48) not null,
            create_timestamp timestamp not null,
            create_user_id varchar(48) not null,
            
            source_model_id varchar(48),
            config_value json,
            
            primary key(id)
        );
        """)
public class MmdetectionTrainTaskBean extends BaseBean
{
	/**
	 * 训练开始时使用的模型 id
	 * */
	String sourceModelId;

	/**
	 * 训练配置项
	 * */
	JsonNode configValue;

}
