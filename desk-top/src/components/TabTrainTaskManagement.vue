<style scoped>

</style>

<template>
<div>
	训练任务管理

	<div class="right-align">
		<w-input class="d-inline text-left small-margin">
			过滤名称
		</w-input>
		<w-select class="d-inline text-left small-margin"
		          :items="['全部', '进行中', '已完成', '迭代中']">
			过滤状态
		</w-select>
		<w-button>
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
			<w-input readonly required @click="$emit('show-right-panel')"
			         :model-value="displaySelectedDataset">
				数据集
			</w-input>

			<div class="space"></div>

			<w-input readonly required @click="$emit('show-right-panel')"
			         :model-value="displaySelectedModel">
				模型
			</w-input>

			<div class="space"></div>

			<w-select :items="ListTrainProcess">
				训练过程
			</w-select>

			<div class="tiny-space"></div>

			<template v-if="true">
				<w-textarea label="控制脚本" no-autogrow rows="12"></w-textarea>
			</template>
			<template v-if="true">
				<w-select :items="ListModelSave">
					模型保存
				</w-select>

				<div class="tiny-space"></div>

				<w-input>
					标签
				</w-input>
			</template>

			<div class="right-align">
				<w-button bg-color="blue-light5" class="small-margin" @click="isDisplayTrainProcessHelp = true">
					关于训练控制
				</w-button>
				<w-button bg-color="success">
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

const emits = defineEmits([
	'show-right-panel', // 显示右侧暂存区
])

const isDisplayCreateTaskModal = ref(true)
const allSelectablePallet = new Set()
allSelectablePallet.add('model')
allSelectablePallet.add('dataset')
const selectablePallet = computed(() => isDisplayCreateTaskModal.value ? allSelectablePallet : null)

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

const selectedPlaceholder = { isPlaceholder: true }
const selectedDataset = ref(null)
const selectedModel = ref(null)
const displaySelectedDataset = computed(() => selectedDataset.value != null ? selectedDataset.value.displayName : '请在暂存区中选中数据集')
const displaySelectedModel = computed(() => selectedModel.value != null ? selectedModel.value.displayName : '请在暂存区选中模型')
function selectPallet(bean)
{
	bean = Object.assign({}, bean)
	const type = bean['palletType'] ?? 'unknown'
	switch (type)
	{
		case 'dataset':
			selectedDataset.value = bean
			break
		case 'model':
			selectedModel.value = bean
			break
	}
}
const isDisplayTrainProcessHelp = ref(true)
const ListTrainProcess = ref([
	{
		label: 'FOR (x)',
		value: 'fixed-round',
		desc: [
			`训练将会运行 <span class="blue-dark1">x</span> 轮, 然后任务将会结束`,
		],
	},
	{
		label: 'FOR (x) FOR (y)',
		value: 'no-stop',
		desc: [
			`训练将会运行 <span class="blue-dark1">x</span> 轮, 然后以最后一轮生成的模型再次开始, 重复 <span class="blue-dark1">y</span> 次`
		],
	},
	{
		label: 'WHILE (TRUE) FOR (x)',
		value: 'while',
		desc: [
			'训练将会运行 <span class="blue-dark1">x</span> 轮, 然后以最后一轮生成的模型再次开始, 直到手动停止'
		],
	},
	{
		label: 'SCRIPT',
		value: 'script',
		desc: [
			`<span class="text-bold small-text red-dark3">高级</span> 使用给定脚本控制训练过程`
		],
	},
])
const ListModelSave = ref([
	{
		label: '保存轮末',
		value: 'save-round-final',
		desc: [
			`保存最后一轮训练生成的模型数据`,
			`使用 <span class="purple-dark2">FOR (x) FOR (y)</span> 模式时会保存每一大轮末尾的模型数据`
		],
	},
	{
		label: '全部保存',
		value: 'save-all',
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

defineExpose({ selectablePallet, selectPallet })
</script>
