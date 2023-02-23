package firok.spring.alloydesk.deskleg.bean;

import com.baomidou.mybatisplus.annotation.TableName;
import firok.spring.dbsculptor.Dubnium;
import firok.spring.mvci.MVCIntrospective;
import lombok.Data;

/**
 * 标签数据
 * */
@Data
@MVCIntrospective
@TableName("d_tag")
@Dubnium(sculpturalScript = """
        create table if not exists d_tag (
            id varchar(48) not null,
            create_timestamp timestamp not null,
            create_user_id varchar(48) not null,
            
            tag_type varchar(32),
            target_id varchar(48),
            tag_value varchar(64),
            
            primary key (id)
        );
        """)
public class TagBean extends BaseBean
{
	/**
	 * 标签类型
	 * */
	String tagType;

	/**
	 * 标签所属目标 id
	 * */
	String targetId;

	/**
	 * 标签数据
	 * */
	String tagValue;
}
