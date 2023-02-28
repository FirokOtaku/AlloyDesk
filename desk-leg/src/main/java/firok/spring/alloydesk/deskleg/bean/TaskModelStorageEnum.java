package firok.spring.alloydesk.deskleg.bean;

import com.fasterxml.jackson.annotation.JsonValue;

public enum TaskModelStorageEnum
{
	SaveAll("saveAll"),
	SaveEnd("saveEnd"),

	;
	@JsonValue
	public final String key;
	TaskModelStorageEnum(String key)
	{
		this.key = key;
	}
}
