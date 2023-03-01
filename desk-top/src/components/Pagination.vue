<!--suppress JSCheckFunctionSignatures, JSUnresolvedVariable -->
<style scoped>
.btn-page { width: 24px }
</style>

<template>
<div>
	<w-button class="btn-page tiny-margin"
	          v-if="hasPrevious"
	          :disabled="disabled"
	          @click="$emit('go-page', pagePrevious)">
		<span class="material-icons">navigate_before</span>
	</w-button>

	<template v-for="page in listPages">
		<span class="select-none tiny-margin small-text">
			<span v-if="isNaN(page)">...</span>
			<w-button class="btn-page"
			          :disabled="disabled"
			          @click="$emit('go-page', page)"
			          v-else-if="page !== null && page !== pageIndex">{{ page }}</w-button>
			<w-button class="btn-page"
			          v-else :disabled="true">{{ page }}</w-button>
		</span>
	</template>

	<w-button class="btn-page tiny-margin"
	          v-if="hasNext"
	          :disabled="disabled"
	          @click="$emit('go-page', pageNext)">
		<span class="material-icons">navigate_next</span>
	</w-button>

</div>
</template>

<script setup>
import {computed} from "vue";

const props = defineProps({
	page: { type: Object, required: false },
	disabled: { type: Boolean, default: false },
})
const emits = defineEmits([
	'go-page', // 用户点击跳转页面
])
const pageIndex = computed(() => props.page?.pageIndex ?? NaN)
const pageSize = computed(() => props.page?.pageSize ?? NaN)
const pageCount = computed(() => props.page?.pageCount ?? NaN)
const count = computed(() => props.page?.count ?? NaN)

const hasPrevious = computed(() => pageIndex.value > 1)
const pagePrevious = computed(() => pageIndex.value - 1)
const hasNext = computed(() => pageIndex.value < pageCount.value)
const pageNext = computed(() => pageIndex.value + 1)

const listPages = computed(() => {
	const index = pageIndex.value, ret = [], pages = pageCount.value
	if(isNaN(index)) return ret

	for(let step = 1; step <= 3 && step < index; step++)
		ret.push(step)
	if(index > 6)
		ret.push(NaN)
	for(let step = Math.max(index - 2, 4); step < index; step++)
		ret.push(step)

	ret.push(index)

	for(let step = index + 1; step <= index + 2 && step < pages - 2; step++)
		ret.push(step)
	if(index < pages - 5)
		ret.push(NaN)
	for(let step = Math.max(pages - 2, index + 1); step <= pages; step++)
		ret.push(step)

	return ret
})

</script>
