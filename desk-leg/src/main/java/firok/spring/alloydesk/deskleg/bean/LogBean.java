package firok.spring.alloydesk.deskleg.bean;

import com.baomidou.mybatisplus.annotation.TableName;
import firok.spring.dbsculptor.Dubnium;
import firok.spring.mvci.MVCIntrospective;
import lombok.Data;

import java.util.*;

/**
 * 系统日志信息
 * */
@Data
@MVCIntrospective
@TableName("d_log")
@Dubnium(sculpturalScript = """
create table if not exists d_log (
    id varchar(48) not null,
    create_timestamp timestamp not null,
    create_user_id varchar(48) not null,
    
    log_type varchar(48),
    log_info varchar(128),
    
    primary key(id)
);
""")
public class LogBean extends BaseBean
{
	/**
	 * 日志类型
	 * */
	String logType;

	/**
	 * 日志信息
	 * */
	String logInfo;
}
