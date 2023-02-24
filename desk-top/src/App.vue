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
			</w-toolbar>
		</header>

		<main class="grow">
			<div class="responsive">
				<TabDataSourceManagement v-if="currentTab === Tabs.DataSourceManagement"/>
				<TabDatasetManagement v-if="currentTab === Tabs.DatasetManagement"/>

				<TabModelManagement v-else-if="currentTab === Tabs.ModelManagement"/>
			</div>
		</main>

		<LeftPanel :is-open="isLeftPanelOpen" @close-panel="isLeftPanelOpen = false" @open-tab="openTab($event)"/>
	</w-app>
</template>

<script setup>
import { ref } from 'vue'

import LeftPanel from "@/components/LeftPanel.vue"
import Tabs from "@/components/tabs"
import TabModelManagement from "@/components/TabModelManagement.vue"
import TabDataSourceManagement from "@/components/TabDataSourceManagement.vue"
import TabDatasetManagement from "@/components/TabDatasetManagement.vue";

const isLeftPanelOpen = ref(false)
const currentTab = ref(Tabs.ModelManagement)
function openTab(tab)
{
	isLeftPanelOpen.value = false
	if(currentTab.value === tab) return
	currentTab.value = tab
}

const contextPageInit = ref({})

</script>
