<!--suppress UnnecessaryLocalVariableJS -->
<style>
</style>

<template>
<div>
	<w-breadcrumbs class="my4" :items="docs" xl>
		<template #item="{item, index, isLast}">
			<span v-if="!isLast"
			      style="font-weight: bold; text-decoration: #0b72c4 underline 1px; cursor: pointer; color: #0c85e5"
			      onclick="loadDrawer('/index.md')">
				{{item.label}}
			</span>
			<span v-else
			      style="font-weight: bold;"
			      @click="void(0)">
				{{item.label}}
			</span>
		</template>
	</w-breadcrumbs>

	<div class="space"></div>

	<div class="center-align" v-show="isLoadingDoc">
		<div>加载文档</div>
		<div class="space"></div>
		<w-spinner />
	</div>
	<div v-show="domDoc?.length > 0">
		<div class="markdown-body"
		     id="markdown-body"
		     ref="refMarkdownBody"
		     v-html="domDoc"></div>
	</div>
	<div v-show="!isLoadingDoc && +(domDoc?.length) <= 0" class="center-align">
		<div class="red-dark1 small-text">
			无法加载文档资源
		</div>
	</div>
</div>
</template>

<script setup>
import {getDrawer} from '@/components/networks'
import {computed, onMounted, onUnmounted, ref} from 'vue'
import {marked} from 'marked'
import 'github-markdown-css'
import WaveUI from 'wave-ui'

const renderer = {
	heading(text, level, raw, slugger) {
		if(level === 1)
		{
			return `<h1 style="display: none">${text}</h1>`
		}

		if (this.options.headerIds) {
			const id = this.options.headerPrefix + slugger.slug(raw);
			return `<h${level} id="${id}">${text}</h${level}>\n`;
		}

		// ignore IDs
		return `<h${level}>${text}</h${level}>\n`;
	},

	link(href, title, text)
	{
		try
		{
			const ignored = new URL(href)
			return `<a href="${href}" target="_blank">${text}</a>`
		}
		catch (err)
		{
			return `<span style="text-decoration: #0b72c4 underline 1px; cursor: pointer; color: #0c85e5" onclick="loadDrawer('${href}')">${text}</span>`
		}
	}
}

marked.use({ renderer });

const pathDoc = ref('/index.md')
const nameDoc = ref('')
const domDoc = ref('')
const docs = computed(() => {
	const ret = [{ label: '手册目录', }]
	if(pathDoc.value !== '/index.md')
		ret.push({ label: nameDoc.value })
	return ret
})

const isLoadingDoc = ref(false)
async function loadDrawer(path = '/index.md')
{
	if(isLoadingDoc.value) return
	isLoadingDoc.value = true
	pathDoc.value = path
	nameDoc.value = ''
	try
	{
		const textDoc = await getDrawer(path)
		const rawDoc = marked(textDoc)
		// const hlDoc = hljs.highlightAuto(rawDoc).value
		domDoc.value = rawDoc
	}
	catch (any)
	{
		WaveUI.instance.notify('加载文档出错: ' + any, 'error', 5000)
	}
	finally
	{
		isLoadingDoc.value = false
	}
}
function loadName()
{
	const domTree = document.getElementById('markdown-body')
	for(const domChild of domTree.childNodes)
	{
		const domType = domChild.nodeName
		if(domType === 'H1')
		{
			nameDoc.value = domChild.textContent
			break
		}
	}
}
const mo = new MutationObserver(loadName)
const refMarkdownBody = ref()

onMounted(() => {
	loadDrawer().finally(() => {})
	mo.observe(refMarkdownBody.value, { childList: true })
	window.loadDrawer = loadDrawer
})
onUnmounted(() => {
	mo.disconnect()
	window.loadDrawer = undefined
})

</script>
