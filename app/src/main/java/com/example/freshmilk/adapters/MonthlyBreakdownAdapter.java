package com.example.freshmilk.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.freshmilk.R;

import java.util.List;
import java.util.Locale;

public class MonthlyBreakdownAdapter extends RecyclerView.Adapter<MonthlyBreakdownAdapter.ViewHolder> {

    private final List<MonthData> monthDataList;

    public static class MonthData {
        public final String monthName;
        public final float amount;

        public MonthData(String monthName, float amount) {
            this.monthName = monthName;
            this.amount = amount;
        }
    }

    public MonthlyBreakdownAdapter(List<MonthData> monthDataList) {
        this.monthDataList = monthDataList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_monthly_breakdown, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MonthData data = monthDataList.get(position);
        holder.tvMonth.setText(data.monthName);
        holder.tvAmount.setText(String.format(Locale.getDefault(), "₹%.0f", data.amount));
    }

    @Override
    public int getItemCount() {
        return monthDataList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMonth, tvAmount;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMonth = itemView.findViewById(R.id.tvMonth);
            tvAmount = itemView.findViewById(R.id.tvAmount);
        }
    }
}
