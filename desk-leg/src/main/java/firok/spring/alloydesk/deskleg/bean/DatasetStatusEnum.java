package firok.spring.alloydesk.deskleg.bean;

import com.fasterxml.jackson.annotation.JsonValue;

public enum DatasetStatusEnum
{
	/**
	 * 正在拉取
	 * */
	Pulling("pulling"),

	/**
	 * 数据集正常, 可以用于创建训练任务
	 * */
	Ready("ready"),

	/**
	 * 数据集损坏
	 * */
	Broken("broken"),

	/**
	 * 数据已经删除, 但数据集信息没有删除
	 * */
	Logical("logical"),
	;

//	@JsonValue
	public final String value;
	DatasetStatusEnum(String value)
	{
		this.value = value;
	}
}
