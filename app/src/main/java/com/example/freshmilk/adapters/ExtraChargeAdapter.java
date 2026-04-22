package com.example.freshmilk.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.freshmilk.R;
import com.example.freshmilk.models.ExtraCharge;

import java.util.List;
import java.util.Locale;

public class ExtraChargeAdapter extends RecyclerView.Adapter<ExtraChargeAdapter.ViewHolder> {

    private final List<ExtraCharge> chargeList;
    private final OnExtraChargeActionListener listener;
    private final boolean isVendor;

    public interface OnExtraChargeActionListener {
        void onEditCharge(ExtraCharge charge);
        void onDeleteCharge(ExtraCharge charge);
    }

    public ExtraChargeAdapter(List<ExtraCharge> chargeList, boolean isVendor, OnExtraChargeActionListener listener) {
        this.chargeList = chargeList;
        this.isVendor = isVendor;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_extra_charge, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ExtraCharge charge = chargeList.get(position);

        holder.tvDescription.setText(charge.getDescription());
        holder.tvDate.setText(charge.getDate());
        holder.tvAmount.setText(String.format(Locale.getDefault(), "₹%.2f", charge.getAmount()));

        if (isVendor && listener != null) {
            holder.llVendorActions.setVisibility(View.VISIBLE);
            holder.ivEdit.setOnClickListener(v -> listener.onEditCharge(charge));
            holder.ivDelete.setOnClickListener(v -> listener.onDeleteCharge(charge));
        } else {
            holder.llVendorActions.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return chargeList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDescription, tvDate, tvAmount;
        LinearLayout llVendorActions;
        ImageView ivEdit, ivDelete;

        ViewHolder(View itemView) {
            super(itemView);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            llVendorActions = itemView.findViewById(R.id.llVendorActions);
            ivEdit = itemView.findViewById(R.id.ivEdit);
            ivDelete = itemView.findViewById(R.id.ivDelete);
        }
    }
}
