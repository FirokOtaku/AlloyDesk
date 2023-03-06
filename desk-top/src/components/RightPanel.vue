<style scoped>

</style>

<template>
	<w-drawer :model-value="isOpen" @before-close="$emit('close-panel')">
		<div style="width: 100%; height: 100%; overflow: auto">
			<div style="padding: 6px 28px 9px 0" class="right-align">
				<w-button @click="$emit('close-panel')">
					<span class="material-icons">close</span>
				</w-button>
			</div>

			<w-accordion :model-value="[true, true]" :items="items">
<!--				<template #item-content.1>-->
<!--					<div v-if="listDataSource.length === 0" class="grey-light2">-->
<!--						暂存数据源为空-->
<!--					</div>-->
<!--				</template>-->
				<template #item-content.1>
					<div v-if="listDataset.length === 0" class="grey-light2">
						暂存数据集为空
					</div>
					<div v-else>
						<div class="right-align">
							<w-button color="primary" class="small-margin"
							          @click="$emit('clear-pallet', 'dataset')">
								清空暂存
							</w-button>
							<w-button color="error" v-if="false">
								批量删除
								<!-- todo 以后再说 -->
							</w-button>
						</div>
						<w-list  hover :items="listDataset">
							<template #item="{item}">
							<span class="tiny-padding">
								{{ item.nameDisplay }}
							</span>
								<span class="small-text tiny-padding">
								{{ item.pullSourceProjectName }} - {{ item.pullSourceName }}
							</span>
								<span class="spacer"></span>
								<w-button @click="selectPallet(item)" v-if="isDatasetSelectable">选定</w-button>
							</template>
						</w-list>
					</div>
				</template>

				<template #item-content.2>
					<div v-if="listModel.length === 0" class="grey-light2">
						暂存模型为空
					</div>
					<div v-else>
						<div class="right-align">
							<w-button color="primary" class="small-margin"
							          @click="$emit('clear-pallet', 'model')">
								清空暂存
							</w-button>
							<w-button color="error" v-if="false">
								批量删除
								<!-- todo 以后再说 -->
							</w-button>
						</div>
						<w-list hover :items="listModel">
							<template #item="{item}">
							<span class="tiny-padding">
								{{ item.displayName }}
							</span>
								<span class="small-text tiny-padding">
								{{ item.modelType }}
							</span>
								<span class="spacer"></span>
								<w-button @click="selectPallet(item)" v-if="isModelSelectable">选定</w-button>
							</template>
						</w-list>
					</div>
				</template>
			</w-accordion>
		</div>
	</w-drawer>
</template>

<script setup>
import {computed, ref} from "vue"

const props = defineProps({
	isOpen: Boolean,
	selectablePallet: { type: Set, required: false },
	listDataset: Array,
	listModel: Array,
	// listDataSource: Array,
	isDeletingDataset: { type: Boolean, required: false },
	isDeletingModel: { type: Boolean, required: false },
})
const isDatasetSelectable = computed(() => props.selectablePallet?.has('dataset') ?? false)
const isModelSelectable = computed(() => props.selectablePallet?.has('model') ?? false)

const items = ref([
	// { title: '数据源' },
	{ title: '数据集' },
	{ title: '模型' },
])

const emits = defineEmits([
	'close-panel',
	'select-pallet', // 从暂存区选定某个实体
	'clear-pallet', // 清空暂存区
	'pallet-batch-delete', // 删除所有暂存区里的实体
])

function selectPallet(item)
{
	emits('select-pallet', item)
	emits('close-panel')
}

</script>
