package io.github.lumyuan.ux.bottomnavigationview

class LiveData<LD> {

    constructor(data: LD? = null){
        this.value = data
    }

    var `value`: LD?
        @Synchronized
        set(value) {
            field = value
            observerPool.onEach {
                it(value)
            }
        }
        @Synchronized
        get

    private val observerPool by lazy {
        ArrayList<(LD?) -> Unit>()
    }

    fun observe(observer: (field: LD?) -> Unit){
        this.observerPool.add(observer)
    }

    fun removeObserver(index: Int){
        this.observerPool.removeAt(index)
    }

    fun removeObserver(observer: (field: LD?) -> Unit){
        this.observerPool.remove(observer)
    }

    fun clearObservers(){
        this.observerPool.clear()
    }

}