package firok.spring.alloydesk.deskleg.bean;

import com.baomidou.mybatisplus.annotation.TableName;
import firok.spring.mvci.MVCIntrospective;
import lombok.Data;

import java.util.*;

/**
 * mmdetection 模型信息
 * */
@Data
@MVCIntrospective
@TableName("d_mmdetection_model")
public class MmdetectionModelBean extends BaseBean
{
	/**
	 * 显示名称
	 * */
	String displayName;
	/**
	 * 创建时间
	 * */
	Date createTimestamp;
	/**
	 * 创建用户 id
	 * */
	String createUserId;
	/**
	 * 相关训练任务 id
	 * */
	String sourceTaskId;
}
