package firok.spring.alloydesk.deskleg.bean;

import com.fasterxml.jackson.annotation.JsonValue;

public enum TaskProcessControlEnum
{
	RoundX("roundX"),
	RoundXY("roundXY"),
	Round1X("round1X"),
	Script("script"),

	;
	@JsonValue
	public final String key;
	TaskProcessControlEnum(String key)
	{
		this.key = key;
	}
}
