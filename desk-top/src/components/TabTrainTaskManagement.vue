<style scoped>
#create-task-modal
{
	transition: height 0.5s ease;
	overflow: hidden;
}
</style>

<template>
<div>
	<div class="right-align">
		<w-input class="d-inline text-left small-margin"
		         v-model="inFilterName"
		         @update:model-value="triggerRefreshTaskList">
			过滤名称
		</w-input>
		<w-select class="d-inline text-left small-margin"
		          v-model="inFilterStatus"
		          :multiple="true"
		          @update:model-value="triggerRefreshTaskList"
		          :items="ListStatus">
			过滤状态
		</w-select>
		<w-button @click="triggerRefreshTaskList"
		          :disabled="isRefreshingTaskList">
			刷新
		</w-button>

		<w-button class="small-margin"
		          :class="isDisplayCreateTaskModal ? 'yellow-dark6--bg' : 'green-dark3--bg'"
		          @click="isDisplayCreateTaskModal = !isDisplayCreateTaskModal">
			{{ isDisplayCreateTaskModal ? '取消创建' : '创建任务'}}
		</w-button>
	</div>

	<div id="create-task-modal"
	     :style="{height: isDisplayCreateTaskModal ? '600px' : '0'}">
		<w-card class="small-padding">
			<w-input v-model="inCreateTaskName" :disabled="isCreatingTask">
				任务名称
			</w-input>

			<div class="medium-space"></div>

			<w-select no-unselect
			          :disabled="isCreatingTask"
			          v-model="inCreateTaskStartControlMethod"
			          :items="ListStartControl"
								item-value-key="value">
				启动控制
			</w-select>

			<div class="space"></div>

			<w-select no-unselect
			          :disabled="isCreatingTask"
			          v-model="inCreateTaskDatasetId"
			          :items="listDataset"
			          item-label-key="nameDisplay"
			          item-value-key="id">
				数据集
			</w-select>

			<div class="space"></div>

			<w-select no-unselect
			          :disabled="isCreatingTask"
			          v-model="inCreateTaskModelId"
			          :items="listModel"
			          item-label-key="displayName"
			          item-value-key="id">
				模型
			</w-select>

			<div class="space"></div>

			<w-select no-unselect
			          :disabled="isCreatingTask"
			          v-model="inCreateTaskProcessControlMethod"
			          :items="ListTrainProcess"
			          item-value-key="value">
				训练过程
			</w-select>

			<div class="small-space"></div>

			<template v-if="inCreateTaskProcessControlMethod === 'script'">
				<w-textarea label="控制脚本" no-autogrow rows="12"
				            :disabled="isCreatingTask"
				            v-model="inCreateTaskScript"></w-textarea>
			</template>
			<template v-else>
				<w-input v-model="inCreateTaskEpochX" type="number" min="1" max="120000" step="1">
					轮次 X
				</w-input>

				<div class="space"></div>

				<w-input v-model="inCreateTaskEpochY"
				         type="number" min="1" max="120000" step="1"
				         v-if="inCreateTaskProcessControlMethod === 'roundXY'">
					轮次 Y
				</w-input>

				<div class="space" v-if="inCreateTaskProcessControlMethod === 'roundXY'"></div>

				<w-select no-unselect
				          :disabled="isCreatingTask"
				          v-model="inCreateTaskModelStorageMethod"
				          :items="ListModelSave"
				          item-value-key="value">
					模型保存
				</w-select>

				<div class="small-space"></div>

				<w-input placeholder="回车以添加新标签"
				         :disabled="isCreatingTask"
				         v-model="inCreateTaskCustomLabel"
				         @keypress.enter="addLabel">
					标签
				</w-input>

				<div class="tiny-space"></div>

				<div>
					<template v-for="tag in inCreateTaskCustomLabelSet">
						<w-tag
							:disabled="isCreatingTask"
							@click="removeLabel(tag)"
							:model-value="true"
							:outline="true"
							color="primary" style="margin-right: 6px">
							{{ tag }}
						</w-tag>
					</template>
				</div>
			</template>

			<div class="right-align">
				<w-button bg-color="blue-light5" class="small-margin" @click="isDisplayTrainProcessHelp = true">
					关于训练控制
				</w-button>
				<w-button bg-color="success" :disabled="isCreatingTask" @click="createTask">
					创建任务
				</w-button>
			</div>

			<w-dialog v-model="isDisplayTrainProcessHelp"
			          width="600"
			          title="训练控制">
				<div style="max-height: 80vh; overflow-y: auto; margin: 0; padding: 0">
					<dl v-for="HelpInfo in ListAllHelp">
						<div class="large-text text-bold">{{ HelpInfo.label }}</div>
						<div class="tiny-space"></div>
						<template v-for="HelpPart in HelpInfo.value">
							<dt class="text-bold purple-dark3">{{ HelpPart.label }}</dt>
							<dd v-for="HelpDesc in HelpPart.desc" v-html="HelpDesc"></dd>
							<div class="space"></div>
						</template>
					</dl>
				</div>
			</w-dialog>

		</w-card>
	</div>

	<div class="space"></div>

	<w-table :headers="tableModel.headers"
	         :items="tableModel.items" fixed-headers
	         style="min-height: 250px">
		<template #item-cell.state="{ item }">
			<template v-for="Status in ListStatus">
				<span v-if="item.state === Status.value">{{Status.label}}</span>
			</template>
		</template>
		<template #item-cell.op="{ item }">
			<w-button class="small-margin"
			          @click="checkTask(item)">
				查看
			</w-button>

			<w-button v-if="item.state === 'WaitingStart'"
			          class="mr2" :disabled="isRefreshingTaskList || isRequestingStart"
			          @click="startTask(item)">
				启动
			</w-button>

			<w-button v-if="item.state === 'Running'"
			          :disabled="isRefreshingTaskList || isRequestingShutdown"
			          @click="shutdownTask(item)">
				停止
			</w-button>

			<w-button v-else-if="item.state === 'WaitingStart' || item.state.endsWith('End')"
			          :disabled="isRefreshingTaskList || isRequestingDelete"
			          @click="deleteTask(item)">
				删除
			</w-button>
		</template>
	</w-table>

	<div class="space"></div>
	<div class="center-align">
		<Pagination :page="tableModel.page" :disabled="isRefreshingTaskList" @go-page="goPage"/>
	</div>

	<w-dialog :model-value="contextCheckingTask != null"
	          @before-close="contextCheckingTask = null"
	          width="700"
	          title="任务信息">
		<div v-if="contextCheckingTask != null">
			<w-card>
				<table class="pl2">
					<tr>
						<td class="grey-dark1 small-text right-align top-align tiny-padding">任务名称</td>
						<td class="pl4">{{contextCheckingTask.task.displayName}}</td>

						<td class="grey-dark1 small-text right-align top-align tiny-padding">类型</td>
						<td class="pl4">{{contextCheckingTask.task.frameworkType}}</td>
					</tr>
					<tr>
						<td class="grey-dark1 small-text right-align top-align tiny-padding">创建时间</td>
						<td class="pl4">
							{{
								contextCheckingTask?.task?.createTimestamp != null ?
								new Date(contextCheckingTask.task.createTimestamp).toLocaleString() :
								''
							}}
						</td>

						<td class="grey-dark1 small-text right-align top-align tiny-padding" rowspan="4">参数</td>
						<td class="pl4 pt2 small-padding" rowspan="4">
							<pre class="grey-light5--bg small-padding"><code>{{
									contextCheckingTask?.task?.configValue != null ?
										JSON.stringify(contextCheckingTask?.task?.configValue, null, 2) :
										''
								}}</code></pre>
						</td>
					</tr>
					<tr>
						<td class="grey-dark1 small-text right-align top-align tiny-padding">启动控制</td>
						<td class="pl4">
							<template v-for="StartControl in ListStartControl">
								<span v-if="StartControl.value === contextCheckingTask.task.startControlMethod">
									{{StartControl.label}}
								</span>
							</template>
						</td>
					</tr>
					<tr>
						<td class="grey-dark1 small-text right-align top-align tiny-padding">模型存储</td>
						<td class="pl4">
							<template v-for="ModelSave in ListModelSave">
								<span v-if="ModelSave.value === contextCheckingTask.task.storageMethod">
									{{ModelSave.label}}
								</span>
							</template>
						</td>
					</tr>
					<tr>
						<td class="grey-dark1 small-text right-align top-align tiny-padding">流程控制</td>
						<td class="pl4">
							<template v-for="ProcessControl in ListTrainProcess">
								<span v-if="ProcessControl.value === contextCheckingTask.task.processControlMethod">
									{{ProcessControl.label}}
								</span>
							</template>
						</td>
					</tr>
					<tr>
						<td class="grey-dark1 small-text right-align top-align tiny-padding">状态</td>
						<td class="pl4">
							<template v-for="Status in ListStatus">
								<span v-if="Status.value === contextCheckingTask.task.state">
									{{Status.label}}
								</span>
							</template>
						</td>

						<td colspan="2" class="left-align">
							<w-button sm
							          :loading="contextCheckingTask.statusLog === 'pulling'"
							          :disabled="contextCheckingTask.statusLog === 'pulling'"
							          @click="requestTaskLogs">
								刷新日志
							</w-button>
						</td>
					</tr>
				</table>
			</w-card>

			<div class="tiny-space"></div>

			<w-table :headers="logModel.headers"
			         :fixed-layout="true"
			         :fixed-headers="true"
			         style="height: 250px"
			         :items="contextCheckingTask.logs"
			         :loading="contextCheckingTask.statusLog === 'pulling'">
				<template #item-cell.createTimestamp="{item}">
					{{ item.createTimestamp?.toLocaleString() ?? '' }}
				</template>
				<template #loading>
					日志加载中
				</template>
				<template #no-data>
					<span v-if="contextCheckingTask.statusLog === 'finished'">
						日志为空
					</span>
					<span v-else-if="contextCheckingTask.statusLog === 'error'">
						加载日志错误
					</span>
					<span v-else>-</span>
				</template>
			</w-table>
		</div>
	</w-dialog>
</div>
</template>

<script setup>

import {onMounted, ref} from "vue"
import WaveUI from "wave-ui";
import {get, post} from "@/components/networks";
import {debounce, replace} from "@/components/util";
import Pagination from "@/components/Pagination.vue";

const props = defineProps({
	listDataset: Array,
	listModel: Array,
})
const emits = defineEmits([
	'show-right-panel', // 显示右侧暂存区
])

const isDisplayCreateTaskModal = ref(false)
const tableModel = ref({
	headers: [
		{ label: '名称', key: 'displayName' },
		{ label: '状态', key: 'state' },
		{ label: '操作', key: 'op' },
	],
	items: [],
	page: null,
})

const ListStatus = ref([
	{ label: '等待开始', value: 'WaitingStart' },
	{ label: '进行中', value: 'Running' },
	{ label: '启动中', value: 'Starting' },
	{ label: '停止中', value: 'Stopping' },
	{ label: '成功结束', value: 'SuccessfulEnd' },
	{ label: '失败结束', value: 'ErrorEnd' },
	{ label: '手动结束', value: 'ShutdownEnd' },
])
const isDisplayTrainProcessHelp = ref(false)
const ListTrainProcess = ref([
	{
		label: 'FOR (x)',
		value: 'roundX',
		desc: [
			`训练将会运行 <span class="blue-dark1">x</span> 轮, 然后任务将会结束`,
		],
	},
	{
		label: 'FOR (y) FOR (x)',
		value: 'roundXY',
		desc: [
			`训练将会运行 <span class="blue-dark1">x</span> 轮, 然后以最后一轮生成的模型再次开始, 重复 <span class="blue-dark1">y</span> 次`
		],
	},
	{
		label: 'WHILE (TRUE) FOR (x)',
		value: 'round1X',
		desc: [
			'训练将会运行 <span class="blue-dark1">x</span> 轮, 然后以最后一轮生成的模型再次开始, 需手动停止'
		],
	},
	// {
	// 	label: 'SCRIPT',
	// 	value: 'script',
	// 	desc: [
	// 		`<span class="text-bold small-text red-dark3">高级</span> 使用给定脚本控制训练过程`
	// 	],
	// },
])
const ListModelSave = ref([
	{
		label: '保存轮末',
		value: 'saveEnd',
		desc: [
			`保存最后一轮训练生成的模型数据`,
			`使用 <span class="purple-dark2">FOR (y) FOR (x)</span> 或 <span class="purple-dark2">WHILE(TRUE) FOR(x)</span> 模式时会保存每一大轮末尾的模型数据`
		],
	},
	{
		label: '全部保存',
		value: 'saveAll',
		desc: [
			`保存训练过程中生成的全部模型数据`,
			`<span class="small-text yellow-dark3">这会占用大量储存空间</span>`
		],
	},
])
const ListStartControl = ref([
	{
		label: '自动',
		value: 'auto',
		desc: [
			`如果当前没有正在运行的其它训练任务, 此任务将会在创建后自动启动`,
			`或当某个任务结束后可能被自动启动`
		],
	},
	{
		label: '立刻启动',
		value: 'now',
		desc: [
			`任务创建后立刻启动`,
			`<span class="small-text yellow-dark3">同时启动多个任务可能导致系统卡顿或训练任务卡死</span>`,
		],
	},
	{
		label: '计划启动',
		value: 'planned',
		desc: [
			`任务将在指定时间点启动`
		],
	},
	{
		label: '手动启动',
		value: 'wait',
		desc: [
			`任务创建后只能手动启动, 不会自动启动`
		],
	},
])
const ListLabeling = ref([
	{
		label: '',
		value: 'null',
		desc: [
			`所有保存的模型数据都有如下基础标签:`,
			`<span class="blue-dark1">任务生成</span>`,
			`可以手动指定更多标签`
		],
	},
])
const ListAllHelp = ref([
	{ label: '启动控制', value: ListStartControl.value },
	{ label: '训练过程', value: ListTrainProcess.value },
	{ label: '保存数据', value: ListModelSave.value },
	{ label: '模型标签', value: ListLabeling.value },
])

const inFilterName = ref('')
const inFilterStatus = ref([])
const isRefreshingTaskList = ref(false)
async function refreshTaskList(name, status, pageIndex = 1, pageSize = 10)
{
	if(isRefreshingTaskList.value) return
	isRefreshingTaskList.value = true

	if(name == null) name = inFilterName.value
	if(status == null) status = [...inFilterStatus.value]

	let tableItems = tableModel.value.items
	try
	{
		let raw = await get({
			url: '/task/list-all',
			params: {
				filterName: name,
				filterStatus: status,
				pageIndex,
				pageSize,
			},
		})
		raw.name = name
		raw.status = status
		tableModel.value.page = raw

		replace(tableItems, raw.records)
	}
	catch (any)
	{
		WaveUI.instance.notify('刷新任务列表出错: ' + any, 'error', 5000)
		tableModel.value.page = null
		replace(tableItems)
	}
	finally
	{
		isRefreshingTaskList.value = false
	}
}
function triggerRefreshTaskList()
{
	if(isRefreshingTaskList.value) return
	debounce(refreshTaskList, 1000)()
}
function goPage(page)
{
	isRefreshingTaskList.value = true
	const name = tableModel.value.page?.name ?? ''
	const status = tableModel.value.page?.status ?? []
	inFilterName.value = name
	replace(inFilterStatus.value, status)
	isRefreshingTaskList.value = false
	refreshTaskList(name, status, page)
}

const inCreateTaskName = ref('')
const inCreateTaskDatasetId = ref({})
const inCreateTaskModelId = ref({})
const inCreateTaskProcessControlMethod = ref('roundX')
const inCreateTaskScript = ref('')
const inCreateTaskModelStorageMethod = ref('saveEnd')
const inCreateTaskStartControlMethod = ref('auto')
const inCreateTaskCustomLabel = ref('')
const inCreateTaskCustomLabelSet = ref(new Set())
const inCreateTaskEpochX = ref(12)
const inCreateTaskEpochY = ref(12)
function addLabel()
{
	if(isCreatingTask.value) return
	const value = inCreateTaskCustomLabel.value
	inCreateTaskCustomLabelSet.value.add(value)
}
function removeLabel(value)
{
	if(isCreatingTask.value) return
	inCreateTaskCustomLabelSet.value.delete(value)
}
const isCreatingTask = ref(false)
async function createTask()
{
	if(isCreatingTask.value) return
	isCreatingTask.value = true

	const datasetId = inCreateTaskDatasetId.value
	const modelId = inCreateTaskModelId.value
	const processControlMethod = inCreateTaskProcessControlMethod.value
	const script = inCreateTaskScript.value
	const modelStorageMethod = inCreateTaskModelStorageMethod.value
	const startControlMethod = inCreateTaskStartControlMethod.value
	const labelSet = [...inCreateTaskCustomLabelSet.value]
	const epochX = inCreateTaskEpochX.value
	const epochY = inCreateTaskEpochY.value
	const name = inCreateTaskName.value

	try
	{
		if(name.trim().length === 0)
		{
			WaveUI.instance.notify('任务名称不可为空', 'error', 5000)
			return
		}
		if(typeof(datasetId) !== 'string' || datasetId === '')
		{
			WaveUI.instance.notify('未选中数据集', 'error', 5000)
			return
		}
		if(typeof(datasetId) !== 'string' || modelId === '')
		{
			WaveUI.instance.notify('未选中模型', 'error', 5000)
			return
		}
		if(processControlMethod === 'script' && script.trim().length === 0)
		{
			WaveUI.instance.notify('未提供脚本', 'error', 5000)
			return
		}

		await post({
			url: '/task/create-mmdetection',
			data: {
				initDatasetId: datasetId,
				initModelId: modelId,
				processControlMethod,
				modelStorageMethod,
				startControlMethod,
				labels: labelSet,
				script, epochX, epochY,
				frameworkType: 'mmdetection',
				displayName: name,
				lr: '0.0025',
				momentum: '0.9',
				weightDecay: '0.0001',
			},
		})
		WaveUI.instance.notify('创建任务成功', 'success', 3000)
		isDisplayCreateTaskModal.value = false
	}
	catch (any)
	{
		WaveUI.instance.notify('创建任务失败: ' + any, 'error', 5000)
	}
	finally
	{
		isCreatingTask.value = false
		await refreshTaskList()
	}
}

const isRequestingShutdown = ref(false)
async function shutdownTask(task)
{
	if(isRequestingShutdown.value) return
	isRequestingShutdown.value = true

	try
	{
		await get({
			url: '/task/shutdown',
			params: { taskId: task.id },
		})
		WaveUI.instance.notify('成功停止任务', 'success', 3000)
	}
	catch (any)
	{
		WaveUI.instance.notify('无法停止任务: ' + any, 'error', 5000)
	}
	finally
	{
		isRequestingShutdown.value = false

		await refreshTaskList()
	}
}
const isRequestingDelete = ref(false)
async function deleteTask(task)
{
	if(isRequestingDelete.value) return
	isRequestingDelete.value = true

	try
	{
		await get({
			url: '/task/delete',
			params: { taskId: task.id },
		})
		WaveUI.instance.notify('成功删除任务', 'success', 3000)
	}
	catch (any)
	{
		WaveUI.instance.notify('删除任务失败: ' + any, 'error', 5000)
	}
	finally
	{
		isRequestingDelete.value = false

		await refreshTaskList()
	}
}
const isRequestingStart = ref(false)
async function startTask(task)
{
	if(isRequestingStart.value) return
	isRequestingStart.value = true

	try
	{
		const taskId = task.id
		await get({
			url: '/task/start',
			params: { id: taskId }
		})
		WaveUI.instance.notify('请求成功, 任务正在启动', 'success', 5000)
	}
	catch(any)
	{
		WaveUI.instance.notify('启动任务失败: ' + any, 'error', 5000)
	}
	finally
	{
		isRequestingStart.value = false

		await refreshTaskList()
	}
}

const logModel = ref({
	headers: [
		{ label: '时间', key: 'createTimestamp', width: '220' },
		// { label: '等级', key: 'logLevel', width: '80' },
		{ label: '信息', key: 'logValue' },
	]
})
const contextCheckingTask = ref(null)
async function checkTask(task)
{
	contextCheckingTask.value = {
		task,
		logs: [],
		statusLog: 'no-init', // no-init, pulling, finished, error
	}

	await requestTaskLogs()
}
async function requestTaskLogs()
{
	const context = contextCheckingTask.value

	if(context.statusLog === 'pulling') return
	context.statusLog = 'pulling'

	try
	{
		const pageLogs = await get({
			url: '/task/get-log',
			params: {
				taskId: context.task.id,
				pageSize: 200,
				level: 100,
			},
		})
		for(const record of pageLogs.records)
		{
			record.createTimestamp = record.createTimestamp != null ? new Date(record.createTimestamp) : null
		}
		replace(context.logs, pageLogs.records)
		context.statusLog = 'finished'
	}
	catch(any)
	{
		WaveUI.instance.notify('读取日志失败: ' + any)
		replace(context.logs)
		context.statusLog = 'error'
	}
}


onMounted(() => triggerRefreshTaskList())
</script>
