package firok.spring.alloydesk.deskleg.service_multi;

import com.baomidou.mybatisplus.extension.service.IService;
import firok.spring.alloydesk.deskleg.bean.DatasetBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 拉取数据集相关操作
 * */
@Service
public class DatasetMultiService
{
	@Autowired
	IService<DatasetBean> service;

	public void startPulling(String datasetId)
	{
		;
	}
	public void stopPulling()
	{
		;
	}
}
