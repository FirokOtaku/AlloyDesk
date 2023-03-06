
import axios from "axios"

axios.defaults.baseURL = import.meta.env.DEV ?
    'http://localhost:29116/' :
    '/'
axios.interceptors.request.use(
    function (config)
    {
        if (config != null && config.params != null)
        {
            config.url += '?' + qs.stringify(config.params, {indices: false})
            config.params = undefined
        }
        return config
    },
    function (error)
    {
        return Promise.reject(error);
    }
);

function handleJavaRet(config)
{
    return new Promise((resolve, reject) => {
        axios(config)
            .then(({ data }) => {
                if(data.success) resolve(data.data)
                else reject(data.msg)
            })
            .catch(err => {
                reject(err)
            })
    })
}
import qs from 'qs'
function paramsSerializer (params)
{
    return qs.stringify(params, { indices: false } )
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
        axios(Object.assign(config, DefaultPostConfig))
            .then(res => {
                console.log(res) // todo
                resolve(res)
            })
            .catch(err => {
                reject(err)
            })
    })
}


export { get, post, postBlob }
