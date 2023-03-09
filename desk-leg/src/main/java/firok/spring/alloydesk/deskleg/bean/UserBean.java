package firok.spring.alloydesk.deskleg.bean;

import com.baomidou.mybatisplus.annotation.TableName;
import firok.spring.dbsculptor.Dubnium;
import firok.spring.mvci.MVCIntrospective;
import lombok.Data;

@Data
@MVCIntrospective
@TableName("d_user")
@Dubnium(sculpturalScript = """
create table if not exists d_user (
	id varchar(48) not null,
    create_timestamp timestamp not null,
    create_user_id varchar(48) not null,
    
    username varchar(64),
    nickname varchar(64),
    password varchar(64),
    avatar blob,
    is_disable bool,
    
    primary key (id)
);
""")
public class UserBean extends BaseBean
{
	private String username;
	private String nickname;
	private String password;
	private byte[] avatar;
	/**
	 * 是否可用账户 被删除的话就为true
	 * */
	private Boolean isDisable;
}
