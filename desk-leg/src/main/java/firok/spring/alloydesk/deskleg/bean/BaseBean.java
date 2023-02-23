package firok.spring.alloydesk.deskleg.bean;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.util.*;

@Data
public class BaseBean
{
	@TableId
	String id;

	Date createTimestamp;

	/**
	 * 创建人 id
	 * */
	String createUserId;

}
