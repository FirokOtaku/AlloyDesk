package firok.spring.alloydesk.deskleg.bean;

/**
 * 任务启动时间点
 * */
public enum TaskStartControlEnum
{
	/**
	 * 自动调度. 如果有其它任务结束, 这个任务就可能启动
	 * */
	Auto,

	/**
	 * 立刻开始
	 * */
	Now,

	/**
	 * 计划任务时间点
	 * */
	Planned,

	/**
	 * 仅等待手动启动, 不会自动启动
	 * */
	Wait,
}
