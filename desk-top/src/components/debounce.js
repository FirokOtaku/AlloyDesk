
let timer = null
function isAsync(fn)
{
    return fn.constructor.name === 'AsyncFunction'
}
function debounce (fn, delay = 300)
{
    return function () {
        clearTimeout(timer)
        timer = setTimeout(()=>{
            if(isAsync(fn))
            {
                fn().finally(() => {})
            }
            else
            {
                fn()
            }
        }, delay);
    }
}

export { debounce }
