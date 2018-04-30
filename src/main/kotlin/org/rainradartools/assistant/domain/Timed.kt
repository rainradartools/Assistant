package org.rainradartools.assistant.domain

data class TimeItem(val start: Long = 0, var end: Long = 0)

class Timed(vararg names: String) {

    val items = mutableMapOf<String, TimeItem>()

    fun start(name: String) {
        items.put(name, TimeItem(start = System.nanoTime()))
    }

    fun end(name: String) {
        val item = items[name]

        if(item != null) {
            item.end = System.nanoTime()
        }
    }

    fun elapsed(name: String): Long? {
        val item = items[name]
        if (item != null) return (item.end - item.start) / 1000000
        return null
    }
}
