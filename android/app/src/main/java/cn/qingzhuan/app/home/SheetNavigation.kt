package cn.qingzhuan.app

import androidx.compose.runtime.*

/** View-local panel history; never owns files or conversion state. */
@Stable
class SheetNavigation {
    var current by mutableStateOf<String?>(null)
        private set
    private var parents by mutableStateOf(emptyList<String>())
    val canGoBack get() = parents.isNotEmpty()
    var backwards by mutableStateOf(false)
        private set

    fun open(name: String) {
        parents = emptyList()
        backwards = false
        current = name
    }
    fun navigate(name: String) {
        if (name == current) return
        current?.let { parents = parents + it }
        backwards = false
        current = name
    }
    fun back() {
        if (parents.isEmpty()) close() else {
            backwards = true
            current = parents.last()
            parents = parents.dropLast(1)
        }
    }
    fun close() {
        parents = emptyList()
        current = null
    }
}
