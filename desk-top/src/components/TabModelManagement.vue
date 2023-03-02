<style scoped>

</style>

<template>
<div>

	<div class="right-align">
		<w-input class="d-inline text-left small-margin"
		         v-model="inFilterName"
		         @update:model-value="triggerRefreshModel">
			过滤名称
		</w-input>
		<w-input class="d-inline text-left small-margin"
		         v-model="inFilterTag"
		         @update:model-value="triggerRefreshModel">
			过滤标签
		</w-input>
		<w-button @click="triggerRefreshModel">
			刷新
		</w-button>

		<w-button bg-color="success"
		          class="small-margin"
		          @click="isDisplayUploadModelModal = true">
			上传模型
		</w-button>
	</div>

	<w-table :headers="tableModel.headers"
	         :items="tableModel.items" fixed-headers
	         :loading="isRefreshingModel"
	         style="min-height: 250px">
		<template #item-cell.tags="{ item, label, header, index }">
			<template v-for="tag in item.tags">
				<w-tag
					:model-value="true"
					:outline="true"
					color="primary" style="margin-right: 6px">
					{{ tag }}
				</w-tag>
			</template>
		</template>

		<template #item-cell.op="{ item, label, header, index }">
			<w-button class="small-margin" @click="$emit('pop-pallet', Object.assign({ palletType: 'model' }, item))">
				暂存
			</w-button>

			<w-confirm class="d-inline-block"
			           bg-color="error"
			           question="确认删除?"
			           cancel="删除" confirm="取消" @cancel="deleteModel(item)">
				删除
			</w-confirm>
		</template>
	</w-table>

	<div class="space"></div>

	<div class="center-align">
		<Pagination :disabled="isRefreshingModel" :page="tableModel.page" @go-page="goPage"/>
	</div>

	<w-overlay :model-value="isDisplayUploadModelModal" :persistent="true">
		<w-card title="上传模型"
		        title-class="primary--bg"
		        content-class="white--bg" style="width: 400px">

			<div class="space"></div>

			<w-input
				class="mb4"
				label="模型名称"
				v-model="inUploadModelName"
				:disabled="isUploadingModel"/>


			<div class="tiny-space"></div>

			<w-input type="file"
			         :preview="false"
			         :disabled="isUploadingModel"
			         v-model="inUploadModelFile">
				模型文件
			</w-input>

			<div class="space"></div>

			<w-radio :model-value="true">MMDetection</w-radio>

			<div class="space"></div>

			<w-input
				class="mb4"
				label="模型标签"
				placeholder="回车以新增"
				v-model="inUploadModelTagTemp"
				:disabled="isUploadingModel"
				@keypress.enter.prevent="addNewTag"/>

			<div>
				<template v-for="tag in inUploadModelTag">
					<w-tag
						:model-value="true"
						@click="deleteTag(tag)"
						:outline="true"
						color="primary" style="margin-right: 6px">
						{{ tag }}
					</w-tag>
				</template>
			</div>

			<div class="right-align">
				<w-button
					bg-color="primary"
					class="small-margin"
					:disabled="false" @click="uploadModalConfirm">
					上传
				</w-button>

				<w-button
					bg-color="secondary"
					class="small-margin"
					:disabled="false"
					@click="isDisplayUploadModelModal = false">
					关闭
				</w-button>
			</div>
		</w-card>
	</w-overlay>

</div>
</template>

<script setup>
import {onMounted, ref} from 'vue'
import {debounce} from "@/components/util";
import {get, post} from "@/components/networks";
import WaveUI from "wave-ui";
import Pagination from "@/components/Pagination.vue";

const tableModel = ref({
	headers: [
		{ label: '名称', key: 'displayName' },
		{ label: '标签', key: 'tags' },
		{ label: '类型', key: 'modelType' },
		{ label: '操作', key: 'op' },
	],
	items: [],
	page: null,
})

const inFilterName = ref('')
const inFilterTag = ref('')
const isRefreshingModel = ref(false)
async function refreshModel(name, tag, pageIndex = 1, pageSize = 10)
{
	if(isRefreshingModel.value) return
	isRefreshingModel.value = true

	try
	{
		const name = inFilterName.value
		const tag = inFilterTag.value
		let raw = await get({
			url: '/model/search',
			params: {
				keywordName: name,
				keywordTag: tag,
				pageIndex,
				pageSize,
			},
		})
		let list = raw?.page?.records ?? []
		for(const model of list)
		{
			const id = model.id
			if(id == null) continue
			model.tags = raw?.mapModelTags[id] ?? []
		}
		tableModel.value.items.splice(0, tableModel.value.items.length)
		tableModel.value.items.push(...list)
		raw.page.name = name
		raw.page.tag = tag
		tableModel.value.page = raw.page
	}
	catch (any)
	{
		WaveUI.instance.notify('获取模型列表出错', 'error', 5000)
		tableModel.value.items.splice(0, tableModel.value.items.length)
		tableModel.value.page = null
	}
	finally
	{
		isRefreshingModel.value = false
	}
}
function triggerRefreshModel()
{
	if(isRefreshingModel.value) return
	debounce(refreshModel, 1000)()
}
function goPage(page)
{
	const name = tableModel.value.page.name, tag = tableModel.value.page.tag
	isRefreshingModel.value = true
	inFilterName.value = name
	inFilterTag.value = tag
	isRefreshingModel.value = false
	refreshModel(name, tag, page)
}

const isDisplayUploadModelModal = ref(false)
const inUploadModelFile = ref(null)
const inUploadModelName = ref('')
const inUploadModelTagTemp = ref('')
const inUploadModelTag = ref(new Set())
const isUploadingModel = ref(false)
function addNewTag()
{
	if(isUploadingModel.value) return
	let value = inUploadModelTagTemp.value
	if(value.length <= 0 || value.length >= 32)
		return
	inUploadModelTag.value.add(value)
}
function deleteTag(value)
{
	if(isUploadingModel.value) return
	inUploadModelTag.value.delete(value)
}
async function uploadModalConfirm()
{
	if(isUploadingModel.value) return
	isUploadingModel.value = true

	const file = inUploadModelFile.value[0]
	const name = inUploadModelName.value
	const tags = [...inUploadModelTag.value]
	try
	{
		const form = new FormData()
		form.append('file', file.file)
		await post({
			url: '/model/upload',
			params: { name, tags, },
			data: form,
		})
		WaveUI.instance.notify('上传成功', 'success', 3000)
		isDisplayUploadModelModal.value = false
	}
	catch (any)
	{
		WaveUI.instance.notify('上传失败: ' + any, 'error', 5000)
	}
	finally
	{
		isUploadingModel.value = false
		await refreshModel()
	}
}

async function deleteModel(model)
{
	try
	{
		await get({
			url: '/model/delete',
			params: { id: model.id },
		})
		WaveUI.instance.notify('删除成功', 'success', 3000)
	}
	catch (any)
	{
		WaveUI.instance.notify('删除失败: ' + any, 'error', 5000)
	}
	finally
	{
		await refreshModel()
	}
}

onMounted(() => refreshModel().finally(() => {}))

</script>
