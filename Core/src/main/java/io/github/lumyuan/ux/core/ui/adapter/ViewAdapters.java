package io.github.lumyuan.ux.core.ui.adapter;

import android.view.View;

import androidx.annotation.NonNull;

public class ViewAdapters {
    @FunctionalInterface
    public interface OnBindViewHolderListener<T> {
        void onBindViewHolder(FastRecyclerViewAdapter<T> adapter, @NonNull View rootView, T data, int position);
    }
}
