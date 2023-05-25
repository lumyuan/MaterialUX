package io.github.lumyuan.ux.core.ui.adapter

import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import io.github.lumyuan.ux.core.ui.base.BaseRecyclerViewAdapter

class FastRecyclerViewAdapter<T>(
    @LayoutRes private val layoutId: Int,
    private val onBindViewHolderListener: ViewAdapters.OnBindViewHolderListener<T>
) : BaseRecyclerViewAdapter<T, FastRecyclerViewAdapter.FastRecyclerViewAdapterViewHolder>() {

    class FastRecyclerViewAdapterViewHolder(val rootView: View) : ViewHolder(rootView)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): FastRecyclerViewAdapterViewHolder =
        FastRecyclerViewAdapterViewHolder(
            View.inflate(parent.context, layoutId, null)
        )

    override fun getItemCount(): Int = this.list.size

    override fun onBindViewHolder(holder: FastRecyclerViewAdapterViewHolder, position: Int) {
        onBindViewHolderListener.onBindViewHolder(this, holder.rootView, this.list[position], position)
    }

}