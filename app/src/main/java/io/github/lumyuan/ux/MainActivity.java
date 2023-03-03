package io.github.lumyuan.ux;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import io.github.lumyuan.ux.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}