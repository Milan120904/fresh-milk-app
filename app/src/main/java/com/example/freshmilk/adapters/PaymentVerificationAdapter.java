package com.example.freshmilk.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.freshmilk.R;
import com.example.freshmilk.activities.ScreenshotViewerActivity;
import com.example.freshmilk.models.Payment;
import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.Locale;

public class PaymentVerificationAdapter extends RecyclerView.Adapter<PaymentVerificationAdapter.VerificationViewHolder> {

    private final List<Payment> payments;
    private final OnVerificationActionListener listener;

    public interface OnVerificationActionListener {
        void onApprove(Payment payment);
        void onReject(Payment payment);
    }

    public PaymentVerificationAdapter(List<Payment> payments, OnVerificationActionListener listener) {
        this.payments = payments;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VerificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_payment_verification, parent, false);
        return new VerificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VerificationViewHolder holder, int position) {
        Payment payment = payments.get(position);
        holder.bind(payment);
    }

    @Override
    public int getItemCount() {
        return payments.size();
    }

    class VerificationViewHolder extends RecyclerView.ViewHolder {
        TextView tvCustomerName, tvMonthYear, tvAmount;
        ImageView ivScreenshot;
        ProgressBar progressImage;
        MaterialButton btnViewScreenshot, btnApprove, btnReject;

        VerificationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
            tvMonthYear = itemView.findViewById(R.id.tvMonthYear);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            ivScreenshot = itemView.findViewById(R.id.ivScreenshot);
            progressImage = itemView.findViewById(R.id.progressImage);
            btnViewScreenshot = itemView.findViewById(R.id.btnViewScreenshot);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnReject = itemView.findViewById(R.id.btnReject);
        }

        void bind(Payment payment) {
            Context context = itemView.getContext();
            tvCustomerName.setText(payment.getCustomerName());
            tvMonthYear.setText(payment.getMonthYearString());
            tvAmount.setText(String.format(Locale.getDefault(), "₹%.0f", payment.getTotalAmount()));

            // Load screenshot thumbnail
            if (payment.getScreenshotUrl() != null && !payment.getScreenshotUrl().isEmpty()) {
                progressImage.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(payment.getScreenshotUrl())
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .centerCrop()
                        .placeholder(R.drawable.ic_gallery)
                        .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                            @Override
                            public boolean onLoadFailed(com.bumptech.glide.load.engine.GlideException e,
                                    Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,
                                    boolean isFirstResource) {
                                progressImage.setVisibility(View.GONE);
                                return false;
                            }
                            @Override
                            public boolean onResourceReady(android.graphics.drawable.Drawable resource,
                                    Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,
                                    com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                                progressImage.setVisibility(View.GONE);
                                return false;
                            }
                        })
                        .into(ivScreenshot);
            }

            // View screenshot fullscreen
            btnViewScreenshot.setOnClickListener(v -> {
                if (payment.getScreenshotUrl() != null) {
                    Intent intent = new Intent(context, ScreenshotViewerActivity.class);
                    intent.putExtra("screenshotUrl", payment.getScreenshotUrl());
                    intent.putExtra("customerName", payment.getCustomerName());
                    context.startActivity(intent);
                }
            });

            // Approve
            btnApprove.setOnClickListener(v -> {
                if (listener != null) listener.onApprove(payment);
            });

            // Reject
            btnReject.setOnClickListener(v -> {
                if (listener != null) listener.onReject(payment);
            });
        }
    }
}
