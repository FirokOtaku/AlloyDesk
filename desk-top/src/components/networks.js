
import Axios from 'axios'
import qs from 'qs'

const isDev = import.meta.env.DEV
const axiosGeneral = Axios.create({
    baseURL: isDev ? 'http://localhost:29118/' : '/',
})
const axiosDrawer = Axios.create({
    baseURL: isDev ? 'http://localhost:29118/drawer' : '/drawer/'
})

axiosGeneral.interceptors.request.use(
    config => {
        if (config != null && config.params != null)
        {
            config.url += '?' + qs.stringify(config.params, {indices: false})
            config.params = undefined
        }
        return config
    },
    error => Promise.reject(error)
);

function handleJavaRet(config)
{
    return new Promise((resolve, reject) => {
        axiosGeneral(config)
            .then(({ data }) => {
                if(data.success) resolve(data.data)
                else reject(data.msg)
            })
            .catch(err => {
                reject(err)
            })
    })
}


const DefaultGetConfig = { method: 'get' }
function get(config = {})
{
    return handleJavaRet(Object.assign(config, DefaultGetConfig))
}
const DefaultPostConfig = { method: 'post' }
function post(config = {})
{
    return handleJavaRet(Object.assign(config, DefaultPostConfig))
}

function postBlob(config = {})
{
    return new Promise((resolve, reject) => {
        axiosGeneral(Object.assign(config,  {
            method: 'post',
            responseType: 'blob',
        }))
        .then(res => resolve(res.data))
        .catch(err => reject(err))
    })
}

function getDrawer(path)
{
    return new Promise((resolve, reject) => {
        axiosDrawer({
            method: 'get',
            responseType: 'text',
            url: path,
        })
        .then(res => resolve(res.data))
        .catch(reject)
    })
}


export { get, post, postBlob, getDrawer }
