package io.github.lumyuan.ux.core.ui.base

import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder

abstract class BaseRecyclerViewAdapter<T, VH : ViewHolder> : Adapter<VH>() {

    val list = ArrayList<T>()

    fun addItem(position: Int, element: T) {
        if (position >= 0) {
            list.add(position, element)
            notifyItemInserted(position)
            notifyItemRangeChanged(position, list.size - position, "addItem")
        }
    }

    fun addItems(position: Int, list: List<T>) {
        if (position >= 0) {
            this.list.addAll(position, list)
            notifyItemRangeInserted(position, list.size)
            notifyItemRangeChanged(position, this.list.size - position, "addItems")
        }
    }

    fun removeItem(position: Int) {
        if (position >= 0 && position < list.size) {
            list.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, list.size - position, "removeItem")
        }
    }

    fun removeItems(list: List<T>, positionStart: Int) {
        val result: Boolean = this.list.removeAll(list.toSet())
        if (result) {
            notifyItemRangeRemoved(positionStart, list.size)
            notifyItemRangeChanged(positionStart, list.size - positionStart, "removeItems")
        }
    }

    fun clearItems() {
        val size = list.size
        this.list.clear()
        notifyItemRangeRemoved(0, size)
        notifyItemRangeChanged(0, size, "clearItems")
    }
}