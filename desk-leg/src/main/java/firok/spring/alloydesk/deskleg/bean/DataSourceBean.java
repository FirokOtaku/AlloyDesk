package firok.spring.alloydesk.deskleg.bean;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import firok.spring.dbsculptor.Dubnium;
import firok.spring.mvci.MVCIntrospective;
import firok.tool.labelstudio.LabelStudioConnector;
import lombok.Data;

import java.net.URL;
import java.util.*;

/**
 * 数据源信息
 * */
@Data
@MVCIntrospective
@TableName("d_data_source")
@Dubnium(sculpturalScript = """
create table if not exists d_data_source (
    id varchar(48) not null,
    create_timestamp timestamp not null,
    create_user_id varchar(48),
    
    name_display varchar(64),
    url varchar(256),
    token varchar(128),
    description varchar(128),
    
    primary key(id)
);
""")
public class DataSourceBean extends BaseBean
{
	/**
	 * 数据源名称
	 * */
	String nameDisplay;

	/**
	 * 数据源地址
	 * */
	String url;

	/**
	 * 数据源鉴权 token
	 * */
	String token;

	/**
	 * 数据源备注
	 * */
	String description;

	/**
	 * 构建一个连接器
	 * */
	@JsonIgnore
	public LabelStudioConnector getConnector()
	{
		try
		{
			var url = new URL(this.getUrl());
			return new LabelStudioConnector(url, this.getToken());
		}
		catch (Exception any)
		{
			throw new RuntimeException(any);
		}
	}
}
