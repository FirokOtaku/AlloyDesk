
function isAsync(fn)
{
    return fn.constructor.name === 'AsyncFunction'
}

function callAnyway(fn)
{
    if(isAsync(fn)) fn().finally(() => {})
    else fn()
}

let timer = null
function debounce (fn, delay = 300)
{
    return function () {
        clearTimeout(timer)
        timer = setTimeout(() => callAnyway(fn), delay);
    }
}

function replace (array = [], content = null)
{
    if(array == null || !(array instanceof Array)) return
    array.splice(0, array.length)

    if(content == null || content[Symbol.iterator] === undefined) return
    array.push(...content)
}

export { debounce, isAsync, callAnyway, replace }
