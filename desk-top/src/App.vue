<!--suppress JSConstantReassignment -->
<style>
body
{
	margin: 0;
	padding: 0;
}
</style>
<style scoped>


</style>

<template>
	<w-app :block="true">
		<header>
			<w-toolbar shadow class="mt6 py1 no-margin">
				<w-button text lg class="ml3" @click="isLeftPanelOpen = true">
					<span class="material-icons">menu</span>
				</w-button>
				<div class="title3 mr3" style="margin-left: 12px">Alloy Desk</div>
				<div class="spacer"></div>

				<w-button @click="isRightPanelOpen = true" outline style="margin-right: 16px">
					<span class="material-icons" v-if="countPalletTotal === 0">inbox</span>

					<w-badge top right bg-color="lime-light1" color="green-dark5" v-else>
						<template #badge>
							{{ countPalletTotal <= 99 ? countPalletTotal : 99 }}
						</template>
						<span class="material-icons">inbox</span>
					</w-badge>
				</w-button>

			</w-toolbar>
		</header>

		<main class="grow">
			<div class="responsive">
<!--				<TabDataSourceManagement ref="refTab" v-if="currentTab === Tabs.DataSourceManagement"/>-->
<!--				<TabDatasetManagement ref="refTab" v-if="currentTab === Tabs.DatasetManagement"/>-->
<!--				<TabTrainTaskManagement ref="refTab" v-if="currentTab === Tabs.TrainTaskManagement"/>-->

<!--				<TabModelManagement ref="refTab" v-else-if="currentTab === Tabs.ModelManagement"/>-->
				<component :is="currentTabClass" ref="refTab"
				           :list-dataset="listPalletDataset"
				           :list-model="listPalletModel"
				           @pop-pallet="popPallet"
				           @show-right-panel="isRightPanelOpen = true"/>
			</div>
		</main>

		<LeftPanel :is-open="isLeftPanelOpen"
		           @close-panel="isLeftPanelOpen = false"
		           @open-tab="openTab($event)"/>

		<RightPanel ref="refRightPanel"
		            :is-open="isRightPanelOpen"
		            :selectable-pallet="currentSelectablePallet"
		            @clear-pallet="clearPallet"
		            @select-pallet="selectPallet"
		            @close-panel="isRightPanelOpen = false"
		            :list-dataset="listPalletDataset"
		            :list-model="listPalletModel"/>
	</w-app>
</template>

<script setup>
import {computed, ref} from 'vue'

import LeftPanel from "@/components/LeftPanel.vue"
import RightPanel from "@/components/RightPanel.vue"
import Tabs from "@/components/tabs"
import TabModelManagement from "@/components/TabModelManagement.vue"
import TabDataSourceManagement from "@/components/TabDataSourceManagement.vue"
import TabDatasetManagement from "@/components/TabDatasetManagement.vue"
import TabTrainTaskManagement from "@/components/TabTrainTaskManagement.vue"
import TabIndex from "@/components/TabIndex.vue"
import WaveUI from "wave-ui"

const isLeftPanelOpen = ref(false)
const currentTab = ref(Tabs.ModelManagement)
const currentTabClass = computed(() => {
	switch(currentTab.value)
	{
		case Tabs.Index: return TabIndex
		case Tabs.DataSourceManagement: return TabDataSourceManagement
		case Tabs.DatasetManagement: return TabDatasetManagement
		case Tabs.TrainTaskManagement: return TabTrainTaskManagement
		case Tabs.ModelManagement: return TabModelManagement
		default: return null
	}
})

const refTab = ref()
function openTab(tab)
{
	isLeftPanelOpen.value = false
	if(currentTab.value === tab) return
	currentTab.value = tab
}
// const isCurrentTabAcceptPallet = computed(() => {
// 	const value = refTab.value?.isAcceptPallet ?? false
// 	return typeof(value) === 'boolean' ? value : (value.value ?? false)
// })
const currentSelectablePallet = computed(() => refTab.value?.selectablePallet)
function popPallet(bean)
{
	const palletType = bean['palletType'] ?? null
	for(let listPallet of [
		palletType === 'model' ? listPalletModel.value : null,
		palletType === 'dataset' ? listPalletDataset.value : null,
	]) {
		if(listPallet === null) continue

		for(let beanPallet of listPallet)
		{
			if(beanPallet['id'] === bean['id'])
			{
				WaveUI.instance.notify('已在暂存区', 'information', 3000)
				return
			}
		}

		listPallet.push(bean)
		WaveUI.instance.notify('已暂存', 'success', 3000)
		return
	}
}
function clearPallet(area)
{
	for(let list of [
		(area ?? 'model') === 'model' ? listPalletModel.value : null,
		(area ?? 'dataset') === 'dataset' ? listPalletDataset.value : null,
	]) list?.splice(0, list?.length)
}
function selectPallet(bean)
{
	refTab.value?.selectPallet(bean)
}

const refRightPanel = ref()
const isRightPanelOpen = ref(false)
const listPalletDataset = ref(
	[{"palletType":"dataset","id":"1aaab2fb-b77f-4305-890b-676db5716a78","createTimestamp":"2023-02-24T06:59:44.000+00:00","createUserId":"","nameDisplay":"3","description":"3","status":"Ready","pullSourceId":"246e40a2-18e9-4961-af6c-52196bc10565","pullSourceProjectId":6,"pullSourceName":"local","pullSourceProjectName":"manual-combine-1-2","pullTimestamp":"2023-02-24T06:59:44.000+00:00","pictureCount":4,"annotationCount":6,"_uid":"1aaab2fb-b77f-4305-890b-676db5716a78"},{"palletType":"dataset","id":"a346d712-a6ef-4485-8126-cd8bece3f30a","createTimestamp":"2023-02-24T06:59:17.000+00:00","createUserId":"","nameDisplay":"1","description":"1","status":"Ready","pullSourceId":"246e40a2-18e9-4961-af6c-52196bc10565","pullSourceProjectId":1,"pullSourceName":"local","pullSourceProjectName":"project-1","pullTimestamp":"2023-02-24T06:59:17.000+00:00","pictureCount":3,"annotationCount":4,"_uid":"a346d712-a6ef-4485-8126-cd8bece3f30a"}]
)
// const listPalletDataSource = ref([])
const listPalletModel = ref(
	[{"palletType":"model","id":"1629814265044996097","createTimestamp":"2023-02-26T12:02:56.000+00:00","createUserId":"","displayName":"1","modelType":"Mmdetection","sourceTaskId":null,"tags":["1"],"_uid":"1629814265044996097"}]
)
const countPalletTotal = computed(() => {
	return listPalletDataset.value.length + listPalletModel.value.length
		// + listPalletDataSource.value.length
})

</script>
