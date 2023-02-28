package firok.spring.alloydesk.deskleg.bean;

import com.fasterxml.jackson.annotation.JsonValue;

public enum FrameworkTypeEnum
{
	Mmdetection("mmdetection"),

	;

	@JsonValue
	public final String key;
	FrameworkTypeEnum(String key)
	{
		this.key = key;
	}
}
