package com.example.freshmilk.activities;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.freshmilk.databinding.ActivityScreenshotViewerBinding;

public class ScreenshotViewerActivity extends AppCompatActivity {

    private ActivityScreenshotViewerBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityScreenshotViewerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String screenshotUrl = getIntent().getStringExtra("screenshotUrl");
        String customerName = getIntent().getStringExtra("customerName");

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        if (customerName != null) {
            binding.toolbar.setTitle("Screenshot — " + customerName);
        }

        if (screenshotUrl != null && !screenshotUrl.isEmpty()) {
            binding.progressBar.setVisibility(View.VISIBLE);

            Glide.with(this)
                    .load(screenshotUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .fitCenter()
                    .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                        @Override
                        public boolean onLoadFailed(com.bumptech.glide.load.engine.GlideException e,
                                Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,
                                boolean isFirstResource) {
                            binding.progressBar.setVisibility(View.GONE);
                            return false;
                        }
                        @Override
                        public boolean onResourceReady(android.graphics.drawable.Drawable resource,
                                Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,
                                com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                            binding.progressBar.setVisibility(View.GONE);
                            return false;
                        }
                    })
                    .into(binding.ivScreenshot);
        } else {
            binding.progressBar.setVisibility(View.GONE);
        }
    }
}
