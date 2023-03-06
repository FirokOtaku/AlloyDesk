package firok.spring.alloydesk.deskleg.service_multi;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Service
public class ModelMultiService
{
	@Value("${firok.spring.alloydesk.folder-model}")
	File folderModelStorage;

	@Value("${firok.spring.alloydesk.folder-test}")
	File folderModelTest;

	public File fileOfModel(String modelId) throws IOException
	{
		return new File(folderModelStorage, modelId + ".model.bin").getCanonicalFile();
	}
	public File folderOfModelTest(String testId) throws IOException
	{
		return new File(folderModelTest, testId).getCanonicalFile();
	}
}
