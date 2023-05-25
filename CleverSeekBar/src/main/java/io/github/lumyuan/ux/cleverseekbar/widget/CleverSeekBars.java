package io.github.lumyuan.ux.cleverseekbar.widget;

import android.view.View;

public class CleverSeekBars {

    @FunctionalInterface
    public interface OnSeekBarChangeListener {
        void onChanged(View view, float progress);
    }

}
