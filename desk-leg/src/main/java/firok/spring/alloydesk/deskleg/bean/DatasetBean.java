package firok.spring.alloydesk.deskleg.bean;

import com.baomidou.mybatisplus.annotation.TableName;
import firok.spring.dbsculptor.Dubnium;
import firok.spring.mvci.MVCIntrospective;
import lombok.Data;

import java.util.*;

/**
 * 数据集信息
 * */
@Data
@MVCIntrospective
@TableName("d_dataset")
@Dubnium(sculpturalScript = """
create table if not exists d_dataset (
    id varchar(48) not null,
    create_timestamp timestamp not null,
    create_user_id varchar(48) not null,
    
    name_display varchar(64),
    description varchar(128),
    status varchar(16),
    pull_source_id varchar(48),
    pull_source_project_id bigint,
    pull_source_name varchar(64),
    pull_source_project_name varchar(64),
    pull_timestamp timestamp,
    picture_count bigint,
    annotation_count bigint,
    
    primary key (id)
);
""")
public class DatasetBean extends BaseBean
{
	/**
	 * 显示名称
	 * */
	String nameDisplay;

	/**
	 * 描述
	 * */
	String description;

	/**
	 * 数据集状态
	 * */
	DatasetStatusEnum status;

	/**
	 * 拉取源 id
	 * */
	String pullSourceId;

	/**
	 * 拉取源项目 id
	 * */
	Long pullSourceProjectId;

	/**
	 * 拉取源名称
	 * */
	String pullSourceName;

	/**
	 * 拉取源项目名称
	 * */
	String pullSourceProjectName;

	/**
	 * 拉取时间
	 * */
	Date pullTimestamp;

	/**
	 * 图片数量
	 * */
	Long pictureCount;

	/**
	 * 标注数量
	 * */
	Long annotationCount;
}
