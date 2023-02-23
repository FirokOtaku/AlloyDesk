
import axios from "axios"

axios.defaults.baseURL = import.meta.env.DEV ?
    'http://localhost:29116/' :
    '/'

function handleJavaRet(config)
{
    return new Promise((resolve, reject) => {
        axios(config)
            .then(({ data }) => {
                if(data.success) resolve(data.data)
                else throw data.message
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


export { get, post }
