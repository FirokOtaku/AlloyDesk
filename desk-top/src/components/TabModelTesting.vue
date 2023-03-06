<!--suppress ExceptionCaughtLocallyJS -->
<style scoped>

</style>

<template>
<div>
	<w-card>
		<w-flex wrap>
			<div class="xs12 sm6 md6 lg6 xl6 small-padding">
				<w-select no-unselect
				          :items="listModel"
				          :disabled="isRunningTest"
				          :readonly="isRunningTest"
				          item-label-key="displayName"
				          item-value-key="id"
				          v-model="inModelId">
					模型
				</w-select>
			</div>

			<div class="xs12 sm6 md6 lg6 xl6 small-padding">
				<w-select no-unselect
				          :items="listDataset"
				          :disabled="isRunningTest"
				          :readonly="isRunningTest"
				          item-label-key="nameDisplay"
				          item-value-key="id"
				          v-model="inDatasetId">
					数据集
				</w-select>
			</div>

			<div class="xs12 sm12 md9 lg10 xl10 small-padding">
				<w-input type="file"
				         :multiple="true"
				         :disabled="isRunningTest"
				         :readonly="isRunningTest"
				         label="测试图片"
				         v-model="inImageFiles"/>
			</div>

			<div class="xs12 sm12 md3 lg2 xl2 padding right-align">
				<w-button @click="startTest" :disabled="isRunningTest">
					启动测试
				</w-button>
			</div>
		</w-flex>
	</w-card>

	<div class="space"></div>

	<w-card>
		<div v-if="listResult == null" class="grey-dark">
			等待测试
		</div>
		<div v-else>
			<div v-for="result of listResult">
				<img :src="result" alt="output-image">
			</div>
		</div>
	</w-card>

</div>
</template>

<script setup>
import { ref } from 'vue'
import {get, post, postBlob} from './networks'
import {debounce, replace} from '@/components/util'
import WaveUI from 'wave-ui'
import JSZip from 'jszip'

const props = defineProps({
	listModel: Array,
	listDataset: Array,
})

const inModelId = ref('')
const inDatasetId = ref('')
const inImageFiles = ref([])
const isRunningTest = ref(false)
async function startTest()
{
	if(isRunningTest.value) return
	isRunningTest.value = true
	try
	{
		const modelId = inModelId.value
		const datasetId = inDatasetId.value
		const files = inImageFiles.value
		if(modelId === '') throw '未选中模型'
		if(datasetId === '') throw '未选中数据集'
		if(files.length === 0) throw '未选中文件'

		const form = new FormData()
		form.set('modelId', modelId)
		form.set('datasetId', datasetId)
		for(let step = 0; step < files.length; step++)
			form.append('files', files[step].file)

		WaveUI.instance.notify('正在启动测试, 这需要一段时间', 'information', 3000)

		const listTestResult = await postBlob({
			url: '/model/test',
			data: form,
		})
		// const listTestImage = []
		// for(const testResult of listTestResult)
		// {
		// 	const blob = new Blob(testResult, { type: 'text/plain' })
		// 	const url = URL.createObjectURL(blob)
		// 	listTestImage.push(url)
		// }
		// console.log('listTestImage', listTestImage)
		// replace(listResult.value, listTestImage)

		WaveUI.instance.notify('测试完成', 'success', 3000)
	}
	catch(any)
	{
		WaveUI.instance.notify('测试失败: ' + any, 'error', 5000)
	}
	finally
	{
		isRunningTest.value = false
	}
}

const listResult = ref(null)
</script>
