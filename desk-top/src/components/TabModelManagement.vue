<style scoped>

</style>

<template>
<div>

	<div class="right-align">
		<w-input class="d-inline text-left small-margin"
		         @change="triggerRefreshModel">
			过滤名称
		</w-input>
		<w-input class="d-inline text-left small-margin"
		         @change="triggerRefreshModel">
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
	         style="height: 250px">
	</w-table>

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
import { ref } from 'vue'
import {debounce} from "@/components/debounce";
import {get, post} from "@/components/networks";
import WaveUI from "wave-ui";

const tableModel = ref({
	headers: [
		{ label: '名称', key: '' },
		{ label: '标签', key: '' },
		{ label: '类型', key: '' },
		{ label: '源', key: '' },
		{ label: '操作', key: '' },
	],
	items: [],
})

const inFilterName = ref('')
const inFilterTag = ref('')
const isRefreshingModel = ref(false)
async function refreshModel()
{
	if(isRefreshingModel.value) return
	isRefreshingModel.value = true

	const storage = tableModel.value.items
	const temp = []
	try
	{
		let result = await get({
			url: '/model/search',
			params: { keywordName: inFilterName.value, keywordTag: inFilterTag.value },
		})
		temp.push(...(result?.page?.records ?? []))
	}
	catch (any)
	{
		WaveUI.instance.notify('获取模型列表出错', 'error', 5000)
		tableModel.value.items.splice(0, )
	}
	finally
	{
		storage.splice(0, storage.length)
		storage.push(...temp)
		isRefreshingModel.value = false
	}
}
function triggerRefreshModel()
{
	debounce(refreshModel, 1000)()
}

const isDisplayUploadModelModal = ref(true)
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

	const file = inUploadModelFile.value
	const name = inUploadModelName.value
	const tags = inUploadModelTag.value

	try
	{
		await post({
			url: '/model/upload',
			params: { name, tags, },
			data: { file, },
		})
		WaveUI.instance.notify('上传成功', 'success', 3000)
		isDisplayUploadModelModal.value = false
	}
	catch (any)
	{
		WaveUI.instance.notify('上传失败', 'error', 5000)
	}
	finally
	{
		isUploadingModel.value = false
		await refreshModel()
	}
}

</script>
