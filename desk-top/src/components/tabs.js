import {shallowRef} from "vue"

import TabUnknown from '@/components/TabUnknown.vue'
import TabModelManagement from '@/components/TabModelManagement.vue'
import TabDataSourceManagement from '@/components/TabDataSourceManagement.vue'
import TabDatasetManagement from '@/components/TabDatasetManagement.vue'
import TabTrainTaskManagement from '@/components/TabTrainTaskManagement.vue'
import TabIndex from '@/components/TabIndex.vue'
import TabModelTesting from '@/components/TabModelTesting.vue'
import TabDatasetView from '@/components/TabDatasetView.vue'

const tabUnknown = shallowRef(TabUnknown)
const tabModelManagement = shallowRef(TabModelManagement)
const tabDataSourceManagement = shallowRef(TabDataSourceManagement)
const tabDatasetManagement = shallowRef(TabDatasetManagement)
const tabTrainTaskManagement = shallowRef(TabTrainTaskManagement)
const tabIndex = shallowRef(TabIndex)
const tabModelTesting = shallowRef(TabModelTesting)
const tabDatasetView = shallowRef(TabDatasetView)

export const Tabs = {
    Unknown: { key: 'unknown', label: '未知', hidden: true, component: tabUnknown },
    DataSourceManagement: { key: 'data-source-management', label: '数据源管理', hidden: false, component: tabDataSourceManagement },
    DatasetManagement: { key: 'dataset-management', label: '数据集管理', hidden: false, component: tabDatasetManagement },

    DatasetView: { key: 'dataset-view', label: '数据集查看', hidden: true, component: tabDatasetView },

    ModelManagement: { key: 'model-management', label: '模型管理', hidden: false, component: tabModelManagement },
    TrainTaskManagement: { key: 'train-task-management', label: '训练任务管理', hidden: false, component: tabTrainTaskManagement },
    ModelTesting: { key: 'model-testing', label: '模型测试', hidden: false, component: tabModelTesting },
    InferenceInterfaceManagement: { key: 'inference-interface-management', label: '推理接口管理', hidden: false, component: tabUnknown },

    UserManagement: { key: 'user-management', label: '用户管理', hidden: false, component: tabUnknown },
    SystemLog: { key: 'system-log', label: '系统日志', hidden: false, component: tabUnknown },
    UserManual: { key: 'user-manual', label: '用户手册', hidden: false, component: tabUnknown },
    Index: { key: 'index', label: '首页', hidden: false, component: tabIndex },
}

export const Groups = {
    Sys: {
        key: 'sys',
        label: '系统',
        tabs: [ Tabs.Index, Tabs.UserManagement, Tabs.SystemLog, Tabs.UserManual ]
    },
    Data: {
        key: 'data',
        label: '数据',
        tabs: [ Tabs.DataSourceManagement, Tabs.DatasetManagement ]
    },
    Model: {
        key: 'model',
        label: '模型',
        tabs: [ Tabs.ModelManagement, Tabs.TrainTaskManagement, Tabs.ModelTesting, Tabs.InferenceInterfaceManagement ]
    },
}
