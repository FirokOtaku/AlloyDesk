package firok.spring.alloydesk.deskleg.bean;

/**
 * 任务状态
 * */
public enum TaskStateEnum
{
	/**
	 * 刚创建 还没开始
	 * */
	WaitingStart,

	/**
	 * 进行中的任务
	 * */
	Running,

	/**
	 * 成功并停止
	 * */
	SuccessfulEnd,

	/**
	 * 失败并停止
	 * */
	ErrorEnd,

	/**
	 * 被手动停止
	 * */
	ShutdownEnd,


}
