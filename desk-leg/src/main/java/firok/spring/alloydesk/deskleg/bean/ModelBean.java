package firok.spring.alloydesk.deskleg.bean;

import com.baomidou.mybatisplus.annotation.TableName;
import firok.spring.dbsculptor.Dubnium;
import firok.spring.mvci.MVCIntrospective;
import lombok.Data;

/**
 * mmdetection 模型信息
 * */
@Data
@MVCIntrospective
@TableName("d_model")
@Dubnium(sculpturalScript = """
        create table if not exists d_model (
            id varchar(48) not null,
            create_timestamp timestamp not null,
            create_user_id varchar(48) not null,
            
            display_name varchar(128),
            model_type varchar(32),
            source_task_id varchar(48),
            
            primary key(id)
        );
        """)
public class ModelBean extends BaseBean
{
	/**
	 * 显示名称
	 * */
	String displayName;

	/**
	 * 模型类型
	 * */
	FrameworkTypeEnum modelType;

	/**
	 * 相关训练任务 id
	 * */
	String sourceTaskId;
}
