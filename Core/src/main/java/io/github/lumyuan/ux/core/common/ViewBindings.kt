package io.github.lumyuan.ux.core.common

import android.app.Activity
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding

inline fun <VB: ViewBinding> AppCompatActivity.bind(
    crossinline inflater: (LayoutInflater) -> VB,
    crossinline onStart: (Activity) -> Unit = {}
) = lazy {
    onStart(this)
    inflater(layoutInflater).apply {
        setContentView(this.root)
    }
}

inline fun <VB: ViewBinding> Fragment.bind(
    crossinline inflater: (LayoutInflater) -> VB,
    crossinline onStart: (Activity) -> Unit = {}
) = lazy {
    activity?.apply {
        onStart(this)
    }
    inflater(layoutInflater)
}