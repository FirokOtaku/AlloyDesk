<!--suppress ExceptionCaughtLocallyJS -->
<style scoped>

</style>

<template>
<div>
	<div class="right-align">
		<w-input class="d-inline text-left small-margin"
		         v-model="inFilterDatasetName"
		         @update:model-value="triggerRefreshDataset">
			过滤名称
		</w-input>
		<w-select :items="listStatus" :return-object="false"
		          v-model="inFilterDatasetStatus"
		          @update:model-value="triggerRefreshDataset"
		          class="d-inline"
		          item-label-key="name"
		          item-value-key="key"
		          item-color-key="colorText"
		          :multiple="true">
			过滤状态
		</w-select>

		<w-button class="small-margin" :disabled="isRefreshingDataset" @click="triggerRefreshDataset">
			刷新
		</w-button>

		<w-button bg-color="success-light1"
		          class="small-margin"
		          @click="isDisplayPullDatasetModal = true">
			拉取
		</w-button>

		<w-button bg-color="success-light1"
		          class="small-margin"
		          @click="isDisplayUploadDatasetModel = true">
			上传
		</w-button>
	</div>

	<div>
		<w-table :headers="tableModel.headers"
		         :items="tableModel.items" fixed-headers
		         :loading="isRefreshingDataset"
		         style="min-height: 350px">
			<template #item-cell.nameDisplay="{ item }">
				{{item.nameDisplay}}
				<CopyIcon :enable-copy="false"
				          v-if="item.description !== ''"
				          color="#201a30"
				          size="0.9"
				          icon="more"
				          :tooltip="item.description"/>
			</template>
			<template #item-cell.content="{ item, label, header, index }">
				<span v-if="item.status === 'Pulling' "></span>

				<div v-else>
					<div v-if="item.annotationCount >= 0 && item.pictureCount >= 0">
						<div>
							<i class="material-icons small-text d-inline-block" style="width: 16px">
								photo_library
							</i>
							<span>
								{{ item.pictureCount }}
							</span>
						</div>
						<div>
							<i class="material-icons small-text d-inline-block" style="width: 16px">
								collections_bookmarks
							</i>
							<span>
								{{ item.annotationCount }}
							</span>
						</div>
					</div>
					<span v-else>
						错误
					</span>
				</div>
			</template>

			<template #item-cell.status="{ item, label, header, index }">
				<template v-for="status in listStatus">
					<span v-if="status.key === item.status" :style="{ color: status.colorText }">
						{{ status.name }}
					</span>
				</template>
			</template>

			<template #item-cell.op="{ item, label, header, index }">
				<div class="right-align">
					<w-button class="small-margin"
					          v-if="item.status === 'Ready' && item.pullSourceId !== ''"
					          @click="viewDataset(item)">
						查看
					</w-button>

					<w-button class="small-margin" v-if="false">
						编辑
					</w-button>

					<w-button class="small-margin" @click="$emit('pop-pallet', Object.assign({ palletType: 'dataset' }, item))">
						暂存
					</w-button>

					<w-confirm class="d-inline-block"
					           bg-color="error"
					           question="确认删除?"
					           v-if="item.status !== 'Pulling'"
					           cancel="删除" confirm="取消" @cancel="deleteDataset(item)">
						删除
					</w-confirm>
				</div>
			</template>
		</w-table>
		<div class="space"></div>
		<div class="center-align">
			<Pagination :page="tableModel.page" :disabled="isRefreshingDataset" @go-page="goPage"/>
		</div>
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
				:readonly="isRefreshingDataSource || isRefreshingSourceDataset"
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
				:readonly="isRefreshingDataSource || isRefreshingSourceDataset"
				color="blue">
				项目
			</w-select>

			<div class="space"></div>

			<w-input
				class="mb4"
				label="显示名称"
				v-model="inNameDisplay"
				:disabled="isRequestingPulling"
				:readonly="isRequestingPulling"/>

			<w-input
				class="mb4"
				label="备注"
				v-model="inDescription"
				:disabled="isRequestingPulling"
				:readonly="isRequestingPulling"/>


			<div class="right-align">
				<w-button
					bg-color="primary"
					class="small-margin"
					:disabled="isRequestingPulling"
					@click="btnPullDatasetModal">
					拉取
				</w-button>

				<w-button
					bg-color="secondary"
					class="small-margin"
					:disabled="isRequestingPulling"
					@click="isDisplayPullDatasetModal = false">
					关闭
				</w-button>
			</div>
		</w-card>
	</w-overlay>

	<w-overlay :model-value="isDisplayUploadDatasetModel" :persistent="true">
		<w-card title="上传数据集"
		        title-class="primary--bg"
		        content-class="white--bg" style="width: 400px">

			<w-input v-model="inUploadDatasetName">
				名称
			</w-input>

			<div class="space"></div>

			<w-input v-model="inUploadDatasetDesc">
				描述
			</w-input>

			<div class="space"></div>

			<w-input type="file" :preview="false" :multiple="false"
			         v-model="inUploadDatasetFile">
				数据集文件
			</w-input>

			<div class="space"></div>

			<div class="select-none">支持上传 .zip 格式文件, 其结构如下:</div>
			<div class="grey-light5--bg select-none">
				<div class="ml0">file.zip</div>
				<div class="ml4">/coco.json <span class="grey small-text italic">COCO 格式索引文件</span></div>
				<div class="ml4">/images <span class="grey small-text italic">存放图片子目录</span></div>
				<div class="ml8">/1.jpg</div>
				<div class="ml8">/2.png</div>
				<div class="ml8">/...</div>
			</div>

			<div class="space"></div>

			<div class="right-align">
				<w-button :disabled="isUploadingDataset"
				          class="small-margin"
				          @click="uploadDataset">
					上传
				</w-button>

				<w-button bg-color="secondary"
				          class="small-margin"
				          :disabled="isUploadingDataset"
				          @click="isDisplayUploadDatasetModel = false">
					关闭
				</w-button>
			</div>
		</w-card>
	</w-overlay>
</div>
</template>

<script setup>

import {ref, onMounted} from 'vue'
import WaveUI from 'wave-ui'
import {get, post} from '@/components/networks'
import {callAnyway, debounce, replace} from '@/components/util'
import Pagination from '@/components/Pagination.vue'
import CopyIcon from '@/components/CopyIcon.vue'

const emits = defineEmits([
	'pop-pallet',
	'open-tab',
])
const props = defineProps({
	initParams: { type: Object, required: false }
})

const isRefreshingDataset = ref(false)
const tableModel = ref({
	headers: [
		{ label: '名称', key: 'nameDisplay' },
		{ label: '内容', key: 'content' },
		{ label: '状态', key: 'status' },
		// { label: '源', key: 'source' },
		// { label: '备注', key: 'description' },
		{ label: '操作', key: 'op' },
	],
	items: [],
	page: null,
})
const inFilterDatasetName = ref('')
const inFilterDatasetStatus = ref([])
const listStatus = ref([
	{ key: 'Pulling', name: '拉取中', colorText: '#0066ff' },
	{ key: 'Ready', name: '就绪', colorText: '#22bb00' },
	// { key: 'Occupied', name: '使用中', colorText: '#4400ff' },
	{ key: 'Broken', name: '损坏', colorText: '#ff1111' },
	{ key: 'Logical', name: '仅存档', colorText: '#ff7700' },
])
async function refreshDataset(name, status, pageIndex = 1, pageSize = 10)
{
	if(isRefreshingDataset.value) return
	isRefreshingDataset.value = true

	if(!name) name = inFilterDatasetName.value
	if(!status) status = [...inFilterDatasetStatus.value]

	const tableItems = tableModel.value.items
	try
	{
		const raw = await get({
			url: '/dataset/list-all',
			params: {
				name,
				status,
				pageIndex,
				pageSize,
			},
		})
		const listDataset = raw['records']
		replace(tableItems, listDataset)
		raw.filterName = name
		raw.filterStatus = status
		tableModel.value.page = raw
	}
	catch (any)
	{
		WaveUI.instance.notify('刷新失败', 'error', 5000)
		tableModel.value.page = null
		replace(tableItems)
	}
	finally
	{
		isRefreshingDataset.value = false
	}
}
function triggerRefreshDataset()
{
	if(isRefreshingDataset.value) return
	debounce(refreshDataset, 1000)()
}
function goPage(page)
{
	const name = tableModel.value.page.filterName, status = tableModel.value.page.filterStatus
	isRefreshingDataset.value = true
	inFilterDatasetName.value = name
	replace(inFilterDatasetStatus.value, status)
	isRefreshingDataset.value = false
	refreshDataset(name, status, page)
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
		for(const source of listSource)
		{
			const { nameDisplay } = source
			source.labelDisplay = nameDisplay
		}
		replace(storage, listSource)
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
	if(selectedSource.value == null || selectedSource.value.isPlaceholder || isRefreshingSourceDataset.value) return
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
		for(const dataset of listDataset)
		{
			const { title } = dataset
			dataset.labelDisplay = title
		}
		replace(storage, listDataset)
	}
	catch (any)
	{
		WaveUI.instance.notify('无法读取数据源', 'error', 5000)
		listSourceDataset.value.splice(0, listSourceDataset.value.length)
		selectedDataset.value = ''
	}
	finally
	{
		isRefreshingSourceDataset.value = false
	}
}

const isGettingLink = ref(false)
async function viewDataset(dataset)
{
	if(isGettingLink.value) return
	isGettingLink.value = true
	try
	{
		const link = await get({
			url: '/dataset/view-link',
			params: { id: dataset.id },
		})
		const url = new URL(link)
		window.open(url, '_blank')
	}
	catch(any)
	{
		WaveUI.instance.notify('无法查看数据集: ' + any, 'error', 5000)
	}
	finally
	{
		isGettingLink.value = false
	}

	// todo 本来是准备自己做一个查看功能 想了想直接打开 LabelStudio 算了
	// emits('open-tab', {
	// 	tab: Tabs.DatasetView,
	// 	params: { dataset, page: tableModel.value.page }
	// })
}

const isDisplayPullDatasetModal = ref(false)
const isRequestingPulling = ref(false)
const inNameDisplay = ref('')
const inDescription = ref('')
async function btnPullDatasetModal()
{
	if(isRequestingPulling.value || !isDisplayPullDatasetModal.value ||
		selectedSource.value == null ||
		selectedSource.value.isPlaceholder) return

	isRequestingPulling.value = true

	const sourceId = selectedSource.value
	const projectId = selectedDataset.value
	const nameDisplay = inNameDisplay.value
	const description = inDescription.value

	try
	{
		const lenNameDisplay = nameDisplay.length
		if(lenNameDisplay <= 0 || lenNameDisplay > 64)
			throw '显示名称为空或过长'
		const lenDesc = description.length
		if(lenDesc > 128)
			throw '描述过长'

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
		WaveUI.instance.notify('创建拉取任务失败: ' + any, 'error', 5000)
	}
	finally
	{
		isRequestingPulling.value = false
		await refreshDataset()
	}
}

const isDisplayUploadDatasetModel = ref(false)
const isUploadingDataset = ref(false)
const inUploadDatasetFile = ref(null)
const inUploadDatasetName = ref('')
const inUploadDatasetDesc = ref('')
async function uploadDataset()
{
	if(isUploadingDataset.value) return
	isUploadingDataset.value = true

	const name = inUploadDatasetName.value ?? ''
	const desc = inUploadDatasetDesc.value ?? ''
	const fileSelected = inUploadDatasetFile.value

	try
	{
		if(name === '') throw '数据集名称不可为空'
		if(fileSelected == null || fileSelected[0] == null) throw '未选中文件'
		const { size, file } = fileSelected[0]
		if(size <= 0) throw '文件大小不可为空'

		const form = new FormData()
		form.set('name', name)
		form.set('desc', desc)
		form.set('file', file)
		WaveUI.instance.notify('开始上传和处理数据. 如果数据集容量较大则可能耗时较长, 请稍候', 'info', 10000)
		await post({
			url: '/dataset/upload',
			data: form,
		})

		isDisplayUploadDatasetModel.value = false
		WaveUI.instance.notify('上传成功', 'success', 3000)

		await refreshDataset(name, ['Pulling', 'Ready'])
	}
	catch (any)
	{
		WaveUI.instance.notify('上传数据集出错: ' + any, 'error', 5000)
	}
	finally
	{
		isUploadingDataset.value = false
	}
}

async function deleteDataset(item)
{
	try
	{
		await get({
			url: '/dataset/delete',
			params: { id: item.id },
		})
		WaveUI.instance.notify('删除成功', 'success', 3000)
	}
	catch (any)
	{
		WaveUI.instance.notify('删除失败', 'error', 5000)
	}
	finally
	{
		await refreshDataset()
	}

}

onMounted(() => {
	const page = props.initParams?.page
	if(page == null)
	{
		callAnyway(refreshDataset)
	}
	else
	{
		replace(tableModel.value.items, page.records)
		inFilterDatasetName.value = page.filterName
		replace(inFilterDatasetStatus.value, page.filterStatus)
		tableModel.value.page = page
	}

	callAnyway(refreshDataSource)
})


</script>
