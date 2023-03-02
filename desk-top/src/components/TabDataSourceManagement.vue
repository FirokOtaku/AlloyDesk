<style scoped>

</style>

<template>
	<div>

		<div class="right-align">
<!--			<w-input class="d-inline text-left small-margin"-->
<!--			         placeholder="过滤名称"></w-input>-->

			<w-button class="small-margin" :disabled="isRefreshingSource" @click="refreshSource">
				刷新
			</w-button>

			<w-button bg-color="success"
			          class="small-margin"
			          @click="isDisplayCreateSourceModal = true">
				添加数据源
			</w-button>
		</div>

		<w-table :headers="tableModel.headers"
		         :items="tableModel.items" fixed-headers
		         :loading="isRefreshingSource"
		         style="min-height: 350px">
			<template #item-cell.token="{ item, label, header, index }">
				<CopyIcon icon="vpn_key" :value="item.token" color="#993300"/>
			</template>
			<template #item-cell.op="{ item, label, header, index }">
				<w-button class="small-margin" @click="btnEditSource_click(item)">
					编辑
				</w-button>

				<w-confirm class="d-inline-block"
				           bg-color="error"
				           question="确认删除?"
				           cancel="删除" confirm="取消" @cancel="deleteSource(item)">
					删除
				</w-confirm>
			</template>
		</w-table>

		<w-overlay v-model="isDisplayCreateSourceModal" :persistent="true">

			<w-card title="添加数据源"
			        title-class="primary--bg"
			        content-class="white--bg" style="width: 300px">
				<div>
					<w-input
						class="mb4"
						label="数据源名称"
						:disabled="isCreatingSource"
						v-model="inSourceName"/>
				</div>

				<div>
					<w-input
						class="mb4"
						label="数据源地址 URL"
						:disabled="isCreatingSource"
						v-model="inSourceUrl"/>
				</div>

				<div>
					<w-input
						class="mb4"
						label="数据源鉴权 token"
						:disabled="isCreatingSource"
						v-model="inSourceToken"/>
				</div>

				<div>
					<w-input
						class="mb4"
						label="数据源备注"
						:disabled="isCreatingSource"
						v-model="inSourceDescription"/>
				</div>

				<div class="right-align">
					<w-button
						bg-color="primary"
						class="small-margin"
						:disabled="isCreatingSource"
						@click="btnCreateSource_click">
						添加
					</w-button>

					<w-button
						bg-color="secondary"
						class="small-margin"
						:disabled="isCreatingSource"
						@click="isDisplayCreateSourceModal = false">
						关闭
					</w-button>
				</div>
			</w-card>

		</w-overlay>

		<w-overlay model-value="isDisplayEditSourceModal" v-if="isDisplayEditSourceModal" :persistent="true">
			<w-card title="添加数据源"
			        title-class="primary--bg"
			        content-class="white--bg" style="width: 300px">
				<div>
					<w-input
						class="mb4"
						label="数据源名称"
						:disabled="isEditingSource"
						v-model="objEditingSource.nameDisplay"/>
				</div>

				<div>
					<w-input
						class="mb4"
						label="数据源地址 URL"
						:disabled="isEditingSource"
						v-model="objEditingSource.url"/>
				</div>

				<div>
					<w-input
						class="mb4"
						label="数据源鉴权 token"
						:disabled="isEditingSource"
						v-model="objEditingSource.token"/>
				</div>

				<div>
					<w-input
						class="mb4"
						label="数据源备注"
						:disabled="isEditingSource"
						v-model="objEditingSource.description"/>
				</div>

				<div class="right-align">
					<w-button
						bg-color="primary"
						class="small-margin"
						:disabled="isEditingSource"
						@click="editSourceConfirm">
						保存
					</w-button>

					<w-button
						bg-color="secondary"
						class="small-margin"
						:disabled="isEditingSource"
						@click="editSourceCancel">
						关闭
					</w-button>
				</div>
			</w-card>
		</w-overlay>

	</div>
</template>

<script setup>

import { ref, onMounted } from 'vue'
import { get, post } from './networks'
import { replace } from './util'


import WaveUI from 'wave-ui'
import CopyIcon from "@/components/CopyIcon.vue";

const isDisplayCreateSourceModal = ref(false)

const inSourceName = ref('')
const inSourceUrl = ref('')
const inSourceDescription = ref('')
const inSourceToken = ref('')

const tableModel = ref({
	headers: [
		{ label: '名称', key: 'nameDisplay' },
		{ label: '主机', key: 'url' },
		{ label: '鉴权', key: 'token' },
		{ label: '源', key: 'source' },
		{ label: '备注', key: 'description' },
		{ label: '操作', key: 'op' },
	],
	items: [],
})

const isRefreshingSource = ref(false)
async function refreshSource()
{
	if(isRefreshingSource.value) return
	isRefreshingSource.value = true

	try
	{
		const listSource = await get({ url: '/data-source/list-all' })
		// tableModel.value.items.push(...listSource)
		const items = tableModel.value.items
		const list = []
		for(const source of listSource)
		{
			const {
				createTimestamp,
				createUserId,
				description,
				id,
				token,
				nameDisplay,
				url,

			} = source
			list.push({
				id,
				nameDisplay,
				url,
				token,
				source: createTimestamp + ',' + createUserId,
				description,
			})
		}
		replace(items, list)
	}
	catch (any)
	{
		WaveUI.instance.notify('刷新数据源列表出错', 'error', 5000)
	}
	finally
	{
		isRefreshingSource.value = false
	}
}

const isCreatingSource = ref(false)
async function btnCreateSource_click()
{
	if(isCreatingSource.value) return

	isCreatingSource.value = true
	const sourceName = inSourceName.value
	const sourceUrl = inSourceUrl.value
	const sourceDescription = inSourceDescription.value
	const sourceToken = inSourceToken.value

	try
	{
		await get({
			url: '/data-source/create',
			params: {
				name: sourceName,
				url: sourceUrl,
				desc: sourceDescription,
				token: sourceToken,
			}
		})
		WaveUI.instance.notify('成功添加', 'success', 3000)
		await refreshSource()
		isDisplayCreateSourceModal.value = false

		inSourceName.value = ''
		inSourceUrl.value = ''
		inSourceDescription.value = ''
		inSourceToken.value = ''
	}
	catch (any)
	{
		WaveUI.instance.notify('添加失败: ' + any, 'error', 0)
	}
	finally
	{
		isCreatingSource.value = false
	}

}

const isDeletingSource = ref(false)
async function deleteSource(item)
{
	if(isDeletingSource.value) return
	isDeletingSource.value = true

	try
	{
		await get({
			url: '/data-source/delete',
			params: { id: item.id },
		})
		WaveUI.instance.notify('删除成功', 'success', 3000)
		await refreshSource()
	}
	catch (any)
	{
		WaveUI.instance.notify('删除失败', 'error', 5000)
	}
	finally
	{
		isDeletingSource.value = false
	}
}

const isDisplayEditSourceModal = ref(false)
const objEditingSource = ref(null)
function btnEditSource_click(item)
{
	if(isDisplayEditSourceModal.value || isEditingSource.value) return
	isDisplayEditSourceModal.value = true
	objEditingSource.value = Object.assign({}, item)
}

const isEditingSource = ref(false)
async function editSourceConfirm()
{
	if(isEditingSource.value) return
	isEditingSource.value = true

	try
	{
		await post({
			url: '/data-source/update',
			data: objEditingSource.value,
		})
		WaveUI.instance.notify('编辑成功', 'success', 3000)
		await refreshSource()
		isDisplayEditSourceModal.value = false
		isEditingSource.value = false
		objEditingSource.value = null
	}
	catch (any)
	{
		WaveUI.instance.notify('编辑失败', 'error', 5000)
	}
}
function editSourceCancel()
{
	isDisplayEditSourceModal.value = false
	isEditingSource.value = false
	objEditingSource.value = null
}

onMounted(() => refreshSource().finally(() => {}))

</script>
