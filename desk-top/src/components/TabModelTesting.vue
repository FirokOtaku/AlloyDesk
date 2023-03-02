<style scoped>

</style>

<template>
<div>
	<w-card>
		<w-flex wrap>

			<div class="xs12 sm12 md12 lg12 xl12 small-padding">
				<w-input type="file"
				         :multiple="true"
				         label="测试图片"
				         v-model="inImageFiles"/>
			</div>

			<div class="xs12 sm6 md6 lg4 xl4 small-padding">
				<w-select no-unselect
				          :items="listModel"
				          item-label-key="displayName"
				          item-value-key="id"
				          v-model="inModelId">
					模型
				</w-select>
			</div>

			<div class="xs12 sm6 md6 lg4 xl4 padding">
				<w-button @click="startTest" :disabled="isRunningTest">启动测试</w-button>
			</div>
		</w-flex>
	</w-card>

	<div class="space"></div>

	<w-card>
		<div v-if="listResult.length === 0" class="grey-dark">
			等待测试
		</div>
		<div v-else>
			测试结果
		</div>
	</w-card>

</div>
</template>

<script setup>
import { ref } from 'vue'
import {get, post} from './networks'
import {debounce} from '@/components/util'
import WaveUI from "wave-ui";

const props = defineProps({
	listModel: Array
})

const inModelId = ref('')
const inImageFiles = ref([])
const isRunningTest = ref(false)
async function startTest()
{
	if(isRunningTest.value) return
	isRunningTest.value = true
	try
	{
		const modelId = inModelId.value
		const files = inImageFiles.value
		const form = new FormData()
		form.set('modelId', modelId)
		form.set('fileCount', '' + files.length)
		for(let step = 0; step < files.length; step++)
			form.set('file' + step, files[step].file)

		const result = await post({
			url: '/model/test-one/',
			data: form,
		})
		console.log(result)
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

const listResult = ref([])
</script>
