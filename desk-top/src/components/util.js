
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

export { debounce, isAsync, callAnyway }
