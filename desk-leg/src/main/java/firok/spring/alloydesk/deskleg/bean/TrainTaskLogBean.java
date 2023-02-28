package firok.spring.alloydesk.deskleg.bean;

import com.baomidou.mybatisplus.annotation.TableName;
import firok.spring.dbsculptor.Dubnium;
import firok.spring.mvci.MVCIntrospective;
import lombok.Data;

@Data
@MVCIntrospective
@TableName("d_train_task_log")
@Dubnium(sculpturalScript = """
create table if not exists d_train_task_log (
    id varchar(48) not null,
    create_timestamp timestamp not null,
    create_user_id varchar(48) not null,
    
    train_task_id varchar(48) not null,
    log_value varchar(128),
    log_level int,
    
    primary key(id)
);
""")
public class TrainTaskLogBean extends BaseBean
{
	private String trainTaskId;
	private String logValue;
	private Integer logLevel;
}
