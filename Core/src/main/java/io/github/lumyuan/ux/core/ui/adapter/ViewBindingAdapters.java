package io.github.lumyuan.ux.core.ui.adapter;

import androidx.viewbinding.ViewBinding;

public class ViewBindingAdapters {
    @FunctionalInterface
    public interface OnBindViewHolderListener<T, VB extends ViewBinding> {
        void onBindViewHolder(FastViewBindingRecyclerViewAdapter<T, VB> adapter, VB binding, T data, int position);

    }
}
