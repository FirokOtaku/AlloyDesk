<style scoped>
</style>

<template>
<div>
	<div class="right-align padding">
		<w-button bg-color="success" @click="isDisplayUploadModal = true">上传图片标注</w-button>
	</div>

	<w-table
		:headers="tableImageAnnotation.headers"
		:items="tableImageAnnotation.items"
		fixed-headers
		style="height: 250px">
	</w-table>

	<w-overlay v-model="isDisplayUploadModal" :persistent="true">
		<w-card shadow title="上传图片标注" title-class="blue-light5--bg" bg-color="white" class="responsive">

			<w-input type="file" label-position="right" v-model="listSelectedFile" style="width: 400px">
				选择文件 <br>
				<w-button sm :disabled="!listSelectedFile.length">开始上传</w-button>
			</w-input>

			<div style="font-size: 80%" class="margin">
				上传 .zip 格式压缩包, 压缩包结构如下
				<div class="small-padding">
					<div>/dataset.zip</div>
					<div>&ensp;&ensp;/annotation.json</div>
					<div>&ensp;&ensp;/images</div>
					<div>&ensp;&ensp;&ensp;&ensp;/img1.png</div>
					<div>&ensp;&ensp;&ensp;&ensp;/img2.png</div>
					<div>&ensp;&ensp;&ensp;&ensp;/img3.png</div>
				</div>
			</div>

			<div class="right-align padding">
				<w-button
					bg-color="primary-light1" dark
					@click="isDisplayUploadModal = false">
					关闭
				</w-button>
			</div>
		</w-card>

	</w-overlay>
</div>
</template>

<script setup>

import { ref } from 'vue'

const isDisplayUploadModal = ref(false)

const listSelectedFile = ref([])

const tableImageAnnotation = ref({
	headers: [
		{ label: 'ID' },
		{ label: '名称' },
		{ label: '标签' },
		{ label: '文件大小' },
		{ label: '源' },
	],
	items: [],
})

</script>
