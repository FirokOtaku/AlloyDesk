<style scoped>

</style>

<template>
	<template v-if="tooltip !== ''">
		<w-tooltip top>
			<template #activator="{ on }">
				<span class="material-icons d-inline-block"
				      v-on="on"
				      @click="copy"
				      :style="style">{{icon}}</span>
			</template>
			{{tooltip}}
		</w-tooltip>
	</template>
	<template v-else>
		<span class="material-icons d-inline-block"
		      @click="copy"
		      :style="style">{{icon}}</span>
	</template>

</template>

<script setup>
import WaveUI from "wave-ui"
import {computed} from "vue";

const props = defineProps({
	icon: { type: String, default: '' },
	value: { type: String, default: '' },
	color: { type: String, default: '#000' },
	enableCopy: { type: Boolean, default: true },
	size: { type: String, default: '1' },
	tooltip: { type: String, default: '' },
})

function copy()
{
	if(!props.enableCopy) return
	navigator.clipboard.writeText(props.value)
		.then(() => WaveUI.instance.notify('复制到剪切板', 'success', 2000))
		.catch(() => WaveUI.instance.notify('无法操作剪切板', 'success', 4000))
}

const sizePx = computed(() => {
	return parseInt(parseFloat(props.size) * 16)
})
const style = computed(() => {
	const px = sizePx.value + 'px'
	return {
		fontSize: px,
		width: px,
		lineHeight: px,
		color: props.color,
		cursor: props.enableCopy ? 'pointer' : undefined,
  }
})
</script>
