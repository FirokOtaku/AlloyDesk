<style scoped>

</style>

<template>
<div>
	训练任务管理

	<div class="right-align">
		<w-input class="d-inline text-left small-margin"
		         @update:model-value="triggerRefreshTaskList">
			过滤名称
		</w-input>
		<w-select class="d-inline text-left small-margin"
		          @update:model-value="triggerRefreshTaskList"
		          :items="['全部', '进行中', '已完成', '迭代中']">
			过滤状态
		</w-select>
		<w-button @click="triggerRefreshTaskList" :disabled="isRefreshingTaskList">
			刷新
		</w-button>

		<w-button class="small-margin"
		          :class="isDisplayCreateTaskModal ? 'yellow-dark6--bg' : 'green-dark3--bg'"
		          @click="isDisplayCreateTaskModal = !isDisplayCreateTaskModal">
			{{ isDisplayCreateTaskModal ? '取消创建' : '创建任务'}}
		</w-button>
	</div>

	<div v-show="isDisplayCreateTaskModal">
		<w-card class="small-padding">
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

			<w-dialog v-model="isDisplayTrainProcessHelp" width="600" title="训练控制">
				<dl v-for="HelpInfo in ListAllHelp">
					<div class="large-text text-bold">{{ HelpInfo.label }}</div>
					<div class="tiny-space"></div>
					<template v-for="HelpPart in HelpInfo.value">
						<dt class="text-bold purple-dark3">{{ HelpPart.label }}</dt>
						<dd v-for="HelpDesc in HelpPart.desc" v-html="HelpDesc"></dd>
						<div class="space"></div>
					</template>
				</dl>
			</w-dialog>

		</w-card>
		<div class="tiny-space"></div>
	</div>

	<w-table :headers="tableModel.headers"
	         :items="tableModel.items" fixed-headers
	         style="min-height: 250px">
		<template #item-cell.op>
			<w-button class="small-margin">
				查看
			</w-button>

			<w-button>
				停止
			</w-button>
		</template>
	</w-table>
</div>
</template>

<script setup>

import {computed, ref} from "vue"
import WaveUI from "wave-ui";
import {get, post} from "@/components/networks";
import {debounce} from "@/components/util";

const props = defineProps({
	listDataset: Array,
	listModel: Array,
})
const emits = defineEmits([
	'show-right-panel', // 显示右侧暂存区
])

const isDisplayCreateTaskModal = ref(true)
const tableModel = {
	headers: [
		{ label: '名称', key: 'nameDisplay' },
		{ label: '状态', key: 'state' },
		{ label: '操作', key: 'op' },
	],
	items: [ {
		nameDisplay: '123',
		state: 'none'
	} ],
}

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
		label: 'FOR (x) FOR (y)',
		value: 'roundXY',
		desc: [
			`训练将会运行 <span class="blue-dark1">x</span> 轮, 然后以最后一轮生成的模型再次开始, 重复 <span class="blue-dark1">y</span> 次`
		],
	},
	{
		label: 'WHILE (TRUE) FOR (x)',
		value: 'round1X',
		desc: [
			'训练将会运行 <span class="blue-dark1">x</span> 轮, 然后以最后一轮生成的模型再次开始, 直到手动停止',
			`<span class="small-text yellow-dark3">遇到错误后自动停止</span>`
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
			`使用 <span class="purple-dark2">FOR (x) FOR (y)</span> 模式时会保存每一大轮末尾的模型数据`
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
const ListLabeling = ref([
	{
		label: '',
		value: 'null',
		desc: [
			`所有保存的模型数据都有如下基础标签:`,
			`<span class="blue-dark1">任务生成 - {任务名称} - {轮数}</span>`,
			`<span class="blue-dark1">生成日期 - {年}/{月}/{日}</span>`,
			`可以手动指定更多标签`
		],
	},
])
const ListAllHelp = ref([
	{ label: '训练过程', value: ListTrainProcess.value },
	{ label: '保存数据', value: ListModelSave.value },
	{ label: '模型标签', value: ListLabeling.value },
])

const isRefreshingTaskList = ref(false)
async function refreshTaskList()
{
	if(isRefreshingTaskList.value) return
	isRefreshingTaskList.value = true

	try
	{
		let result = await get({
			url: '/task/list-all',
			params: {},
		})
		console.log('刷新任务列表', result)
	}
	catch (any)
	{
		WaveUI.instance.notify('刷新任务列表出错: ' + any, 'error', 5000)
	}
	finally
	{
		isRefreshingTaskList.value = false
	}
}
function triggerRefreshTaskList()
{
	debounce(refreshTaskList, 1000)
}

const inCreateTaskDatasetId = ref({})
const inCreateTaskModelId = ref({})
const inCreateTaskProcessControlMethod = ref('roundX')
const inCreateTaskScript = ref('')
const inCreateTaskModelStorageMethod = ref('saveEnd')
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
	const labelSet = [...inCreateTaskCustomLabelSet.value]
	const epochX = inCreateTaskEpochX.value
	const epochY = inCreateTaskEpochY.value

	try
	{
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
			url: '/task/create',
			data: {
				datasetId, modelId,
				processControlMethod,
				modelStorageMethod,
				labels: labelSet,
				script, epochX, epochY,
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
</script>
