<style scoped>

</style>

<template>
<div>

	<div class="right-align">
		<w-input class="d-inline text-left small-margin"
		         placeholder="过滤名称"></w-input>
		<w-input class="d-inline text-left small-margin"
		         placeholder="过滤标签"></w-input>

		<w-button bg-color="success"
		          class="small-margin"
		          @click="isDisplayUpdateModal = true">
			上传模型
		</w-button>
	</div>

	<w-table :headers="tableModel.headers"
	         :items="tableModel.items" fixed-headers
	         style="height: 250px">
	</w-table>

</div>
</template>

<script setup>
import { ref } from 'vue'

const isDisplayUpdateModal = ref(false)
const tableModel = ref({
	headers: [
		{ label: 'ID' },
		{ label: '名称' },
		{ label: '标签' },
		{ label: '源' },
	],
	items: [],
})

</script>
