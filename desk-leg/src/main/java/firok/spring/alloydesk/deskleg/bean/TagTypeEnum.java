package firok.spring.alloydesk.deskleg.bean;

public enum TagTypeEnum
{
	/**
	 * 模型拥有的标签
	 * @apiNote targetId == model.id
	 * */
	ModelTag,

	/**
	 * 某个任务生成的模型会拥有的标签
	 * @apiNote targetId == task.id
	 * */
	TaskGenerateModelTag,
}
