<style scoped>

</style>

<template>
<div>
	<div class="right-align">
		<w-input class="d-inline text-left small-margin"
		         placeholder="过滤名称"></w-input>
		<w-select :items="[]" class="d-inline">
			过滤状态
		</w-select>

		<w-button class="small-margin" :disabled="isRefreshingDataset" @click="refreshDataset">
			刷新
		</w-button>

		<w-button bg-color="success"
		          class="small-margin"
		          @click="isDisplayPullDatasetModal = true">
			拉取数据集
		</w-button>
	</div>

	<div>
		<w-table :headers="tableModel.headers"
		         :items="tableModel.items" fixed-headers
		         :loading="isRefreshingDataset"
		         style="min-height: 350px">
			<template #item-cell.op="{ item, label, header, index }">
				<w-button class="small-margin">
					编辑
				</w-button>

				<w-confirm class="d-inline-block"
				           bg-color="error"
				           question="确认删除?"
				           cancel="删除" confirm="取消">
					删除
				</w-confirm>
			</template>
		</w-table>
	</div>

	<w-overlay :model-value="isDisplayPullDatasetModal" :persistent="true">
		<w-card title="拉取数据集"
		        title-class="primary--bg"
		        content-class="white--bg" style="width: 400px">

			<div class="right-align">
				<w-button
					bg-color="primary"
					class="small-margin"
					@click="refreshDataSource"
					:disabled="false">
					刷新数据源列表
				</w-button>
			</div>

			<w-select
				:items="listDataSource"
				item-label-key="labelDisplay"
				item-value-key="id"
				v-model="selectedSource"
				@item-select="refreshSourceDataset"
				:disabled="isRefreshingDataSource || isRefreshingSourceDataset"
				color="blue">
				数据源

				<template #item="{ item, selected }">
					<div :class="selected ? 'grey-light4' : ''">
						<span class="text-bold margin">{{ item.labelDisplay }}</span>
						<span class="small-text grey">{{ item.url }}</span>
					</div>
				</template>
			</w-select>

			<div class="space"></div>

			<w-select
				:items="listSourceDataset"
				item-label-key="labelDisplay"
				item-value-key="id"
				v-model="selectedDataset"
				:disabled="isRefreshingDataSource || isRefreshingSourceDataset"
				color="blue">
				项目
			</w-select>

			<div class="space"></div>

			<w-input
				class="mb4"
				label="显示名称"
				v-model="inNameDisplay"
				:disabled="isRequestingPulling"/>

			<w-input
				class="mb4"
				label="备注"
				v-model="inDescription"
				:disabled="isRequestingPulling"/>


			<div class="right-align">
				<w-button
					bg-color="primary"
					class="small-margin"
					:disabled="false"
					@click="btnPullDatasetModal">
					拉取
				</w-button>

				<w-button
					bg-color="secondary"
					class="small-margin"
					:disabled="false"
					@click="isDisplayPullDatasetModal = false">
					关闭
				</w-button>
			</div>
		</w-card>
	</w-overlay>
</div>
</template>

<script setup>

import { ref, onMounted } from "vue";
import WaveUI from "wave-ui";
import {get, post} from "@/components/networks";

const isRefreshingDataset = ref(false)
const tableModel = ref({
	headers: [
		{ label: '名称', key: 'nameDisplay' },
		{ label: '内容', key: 'content' },
		{ label: '状态', key: 'status' },
		{ label: '源', key: 'source' },
		{ label: '备注', key: 'description' },
		{ label: '操作', key: 'op' },
	],
	items: [],
})
async function refreshDataset()
{
	if(isRefreshingDataset.value) return
	isRefreshingDataset.value = true

	try
	{
		const storage = tableModel.value.items
		const raw = await get({
			url: '/dataset/list-all',
		})
		const listDataset = raw['records']
		for(const dataset of listDataset)
		{
			;
		}
		storage.splice(0, storage.length)
		storage.push(...listDataset)
	}
	catch (any)
	{
		console.log('any', any)
		WaveUI.instance.notify('刷新失败', 'error', 5000)
	}
	finally
	{
		isRefreshingDataset.value = false
	}
}

const Placeholder = {isPlaceholder: true, labelDisplay: '未选择'}
const selectedSource = ref(Placeholder)
const listDataSource = ref([])
const isRefreshingDataSource = ref(false)
async function refreshDataSource()
{
	if(isRefreshingDataSource.value) return
	isRefreshingDataSource.value = true

	try
	{
		const storage = listDataSource.value
		const listSource = await get({
			url: '/data-source/list-all',
		})
		storage.splice(0, storage.length)
		for(const source of listSource)
		{
			const { nameDisplay } = source
			source.labelDisplay = nameDisplay
		}
		storage.push(...listSource)
	}
	catch (any)
	{
		WaveUI.instance.notify('获取数据源列表出错', 'error', 5000)
	}
	finally
	{
		isRefreshingDataSource.value = false
	}

	await refreshSourceDataset()
}
const isRefreshingSourceDataset = ref(false)
const listSourceDataset = ref([])
const selectedDataset = ref('')
async function refreshSourceDataset()
{
	if(selectedSource.value.isPlaceholder || isRefreshingSourceDataset.value) return
	isRefreshingSourceDataset.value = true

	const sourceId = selectedSource.value

	try
	{
		const storage = listSourceDataset.value
		const rawDataset = await get({
			url: '/dataset/list-all-raw',
			params: { sourceId },
		})
		const listDataset = rawDataset['results']
		storage.splice(0, storage.length)
		for(const dataset of listDataset)
		{
			const { title } = dataset
			dataset.labelDisplay = title
		}
		storage.push(...listDataset)
	}
	catch (any)
	{
		WaveUI.instance.notify('无法读取数据源', 'error', 5000)
	}
	finally
	{
		isRefreshingSourceDataset.value = false
	}
}

const isDisplayPullDatasetModal = ref(false)
const isRequestingPulling = ref(false)
const inNameDisplay = ref('')
const inDescription = ref('')
async function btnPullDatasetModal()
{
	if(isRequestingPulling.value || !isDisplayPullDatasetModal.value ||
		selectedSource.value.isPlaceholder) return

	isRequestingPulling.value = true

	const sourceId = selectedSource.value
	const projectId = selectedDataset.value
	const nameDisplay = inNameDisplay.value
	const description = inDescription.value

	try
	{
		await post({
			url: '/dataset/pull',
			params: { sourceId, projectId },
			data: { nameDisplay, description },
		})
		WaveUI.instance.notify('创建拉取任务成功, 需要一段时间完成拉取', 'success', 3000)
		isDisplayPullDatasetModal.value = false
	}
	catch (any)
	{
		WaveUI.instance.notify('创建拉取任务失败', 'error', 5000)
	}
	finally
	{
		isRequestingPulling.value = false
	}
}

onMounted(() => {
	refreshDataset()
	refreshDataSource()
})


</script>
