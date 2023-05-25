package io.github.lumyuan.ux.core.ui.adapter

import android.app.Activity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.viewbinding.ViewBinding
import io.github.lumyuan.ux.core.ui.base.BaseRecyclerViewAdapter

class FastViewBindingRecyclerViewAdapter<T, VB : ViewBinding>(
    private val inflate: (LayoutInflater) -> VB,
    private val onBindViewHolderListener: ViewBindingAdapters.OnBindViewHolderListener<T, VB>
) : BaseRecyclerViewAdapter<T, FastViewBindingRecyclerViewAdapter.FastViewBindingRecyclerViewAdapterViewHolder<VB>>() {
    class FastViewBindingRecyclerViewAdapterViewHolder<VB : ViewBinding>(val binding: VB) :
        ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): FastViewBindingRecyclerViewAdapterViewHolder<VB> =
        FastViewBindingRecyclerViewAdapterViewHolder(
            inflate((parent.context as Activity).layoutInflater)
        )

    override fun getItemCount(): Int = this.list.size

    override fun onBindViewHolder(
        holder: FastViewBindingRecyclerViewAdapterViewHolder<VB>,
        position: Int
    ) {
        this.onBindViewHolderListener.onBindViewHolder(
            this,
            holder.binding,
            this.list[position],
            position
        )
    }

}