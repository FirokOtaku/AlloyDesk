package firok.spring.alloydesk.deskleg.service_multi;

import com.baomidou.mybatisplus.extension.service.IService;
import firok.spring.alloydesk.deskleg.bean.DatasetBean;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * 拉取数据集相关操作
 * */
@Service
public class DatasetMultiService
{
	@Autowired
	IService<DatasetBean> service;

	@Value("${firok.spring.alloydesk.folder-dataset}")
	File folderDatasetStorage;

	public File folderOfDataset(String datasetId) throws IOException
	{
		return new File(folderDatasetStorage, datasetId).getCanonicalFile();
	}
	public File fileOfCocoDatasetAnnotation(String datasetId) throws IOException
	{
		return new File(folderDatasetStorage, datasetId + "/coco.json").getCanonicalFile();
	}
	public File fileOfCocoDatasetAnnotation(File folderCocoDataset) throws IOException
	{
		return new File(folderCocoDataset, "coco.json").getCanonicalFile();
	}
	public File folderOfCocoDatasetImages(String datasetId) throws IOException
	{
		return new File(folderDatasetStorage, datasetId + "/images").getCanonicalFile();
	}
	public File folderOfCocoDatasetImages(File folderCocoDataset) throws IOException
	{
		return new File(folderCocoDataset, "images").getCanonicalFile();
	}

	public void deleteDataset(String datasetId)
	{
		try
		{
			var folderDataset = folderOfDataset(datasetId);
			FileUtils.forceDelete(folderDataset);
		}
		catch (IOException ignored) { }
	}
}
