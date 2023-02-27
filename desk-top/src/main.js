import { createApp } from 'vue'
import App from './App.vue'

import WaveUI from 'wave-ui'
import '@/assets/beer.part.css'
import 'wave-ui/dist/wave-ui.css'
import 'material-design-icons/iconfont/material-icons.css'

const app = createApp(App)

new WaveUI(app, {
    // Some Wave UI options.
    // iconsLigature: 'material-icons',
})

app.mount('#app')
