<style>
.left-panel-list-group
{
	padding-left: 36px !important;
	font-weight: bold;
}
.left-panel-list-item
{
	padding-left: 44px !important;
	cursor: pointer;
}
</style>

<template>
	<w-drawer :model-value="isOpen" :left="true" @before-close="$emit('close-panel')">
		<div style="width: 100%; height: 100%; overflow: auto">
			<div style="padding: 3px 0 9px 28px">
				<w-button @click="$emit('close-panel')">
					<span class="material-icons">close</span>
				</w-button>
			</div>

			<w-list
				:items="items"
				bg-color="blue-light5"
				color="primary"
				item-class="left-panel-list-item"
				@click="itemClick"
				hover>
			</w-list>
		</div>

	</w-drawer>
</template>

<script setup>
import {computed, ref} from "vue"
import {Tabs, Groups} from './tabs'

const props = defineProps({
	isOpen: Boolean
})

const emits = defineEmits([
	'close-panel',
	'open-tab',
])

const items = computed(() => {
	const ret = []
	for(const keyGroup in Groups)
	{
		const Group = Groups[keyGroup]
		ret.push({
			label: Group.label,
			class: 'left-panel-list-group'
		})

		for(const Tab of Group.tabs)
		{
			if(Tab.hidden) continue

			ret.push({
				label: Tab.label,
				tab: Tab.key
			})
		}
	}
	return ret
})

function itemClick(item)
{
	const title = item?.target?.innerText ?? ''
	for(const keyTab in Tabs)
	{
		const Tab = Tabs[keyTab]
		if(Tab.label === title)
		{
			emits('open-tab', { tab: Tab, params: null })
			break
		}
	}
}

</script>
