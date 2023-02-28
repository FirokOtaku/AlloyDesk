<style>
.left-panel-list-group
{
	padding-left: 36px !important;
	font-weight: bold;
}
.left-panel-list-item
{
	padding-left: 44px !important;
	cursor: pointer;
}
</style>

<template>
	<w-drawer :model-value="isOpen" :left="true" @before-close="$emit('close-panel')">
		<div style="width: 100%; height: 100%; overflow: auto">
			<div style="padding: 3px 0 9px 28px">
				<w-button @click="$emit('close-panel')">
					<span class="material-icons">close</span>
				</w-button>
			</div>

			<w-list
				:items="items"
				bg-color="blue-light5"
				color="primary"
				item-class="left-panel-list-item"
				@click="itemClick"
				hover>
			</w-list>
		</div>

	</w-drawer>
</template>

<script setup>
import {ref} from "vue"
import Tabs from './tabs'

const props = defineProps({
	isOpen: Boolean
})

const emits = defineEmits([
	'close-panel',
	'open-tab',
])

const items = ref([
	{
		label: '系统',
		class: 'left-panel-list-group'
	},
	{
		label: '概览',
		tab: Tabs.Index
	},
	{
		label: '操作手册',
		tab: Tabs.UserManual
	},

	{
		label: '数据',
		class: 'left-panel-list-group'
	},
	{
		label: '数据源管理',
		tab: Tabs.DataSourceManagement
	},
	{
		label: '数据集管理',
		tab: Tabs.DatasetManagement
	},
	{
		label: '模型',
		class: 'left-panel-list-group'
	},
	{
		label: '模型管理',
		tab: Tabs.ModelManagement
	},
	{
		label: '训练任务管理',
		tab: Tabs.TrainTaskManagement
	},
	{
		label: '模型测试',
		tab: Tabs.ModelTestingManagement
	},
	{
		label: '推理接口管理',
		tab: Tabs.InferenceInterfaceManagement
	},

])

function itemClick(item)
{
	const title = item?.target?.innerText ?? ''
	for(const item of items.value)
		if(item.label === title && item.tab != null)
			emits('open-tab', item.tab ?? Tabs.Unknown)
}

</script>
