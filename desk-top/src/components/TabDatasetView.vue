<style scoped>

</style>

<template>
<div>
	<w-card>
		<div v-if="dataset != null">
			<div class="text-bold">
				{{ dataset.nameDisplay }}
			</div>
			<div class="tiny-space"></div>
			<div class="small-text">
				{{ dataset.description }}
			</div>
		</div>
		<div class="right-align">
			<w-button @click="back">
				返回
			</w-button>
		</div>
	</w-card>

	<w-card>

	</w-card>

</div>
</template>

<script setup>

import {computed, onMounted, ref} from 'vue'
import WaveUI from 'wave-ui'
import {Tabs} from '@/components/tabs'

const props = defineProps({
	initParams: { type: Object, required: false }
})

const emits = defineEmits([
	'open-tab'
])

const dataset = computed(() => props.initParams?.dataset)

function back()
{
	emits('open-tab', {
		tab: Tabs.DatasetManagement,
		params: { page: props.initParams?.page },
	})
}

const isLoadingDataset = ref(false)
async function loadDataset()
{
	if(isLoadingDataset.value) return
	isLoadingDataset.value = true

	try
	{
		;
	}
	catch(any)
	{
		WaveUI.instance.notify('加载数据集出错: ' + any, 'error', 5000)
	}
	finally
	{
		isLoadingDataset.value = false
	}
}

onMounted(() => {
	console.log('组件加载', props.initParams)
})

</script>
