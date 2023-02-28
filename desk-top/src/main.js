import { createApp } from 'vue'
import App from './App.vue'

import WaveUI from 'wave-ui'
import '@/assets/beer.part.css'
import 'wave-ui/dist/wave-ui.css'
import 'material-design-icons/iconfont/material-icons.css'
import Chart from 'chart.js/auto'

const app = createApp(App)

new WaveUI(app, {
    css: {
        grid: 12
    }
    // Some Wave UI options.
    // iconsLigature: 'material-icons',
})

app.mount('#app')
