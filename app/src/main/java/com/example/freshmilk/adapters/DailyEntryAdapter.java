package com.example.freshmilk.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.freshmilk.R;
import com.example.freshmilk.models.DailyEntry;

import java.util.List;
import java.util.Locale;

public class DailyEntryAdapter extends RecyclerView.Adapter<DailyEntryAdapter.EntryViewHolder> {

    private final List<DailyEntry> entries;
    private OnEntryActionListener actionListener;

    /**
     * Listener for edit/delete actions on entries (vendor side only).
     * Pass null for customer-side (read-only) usage.
     */
    public interface OnEntryActionListener {
        void onEditEntry(DailyEntry entry);
        void onDeleteEntry(DailyEntry entry);
    }

    /** Read-only constructor (customer side) */
    public DailyEntryAdapter(List<DailyEntry> entries) {
        this.entries = entries;
        this.actionListener = null;
    }

    /** Constructor with edit/delete actions (vendor side) */
    public DailyEntryAdapter(List<DailyEntry> entries, OnEntryActionListener listener) {
        this.entries = entries;
        this.actionListener = listener;
    }

    @NonNull
    @Override
    public EntryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_daily_entry, parent, false);
        return new EntryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EntryViewHolder holder, int position) {
        DailyEntry entry = entries.get(position);
        holder.tvDate.setText(entry.getDate());
        holder.tvMorning.setText(String.format(Locale.getDefault(), "%.1f", entry.getMorningQty()));
        holder.tvEvening.setText(String.format(Locale.getDefault(), "%.1f", entry.getEveningQty()));
        holder.tvTotal.setText(String.format(Locale.getDefault(), "%.1f", entry.getTotalQty()));
        holder.tvAmount.setText(String.format(Locale.getDefault(), "₹%.0f", entry.getDailyAmount()));

        // Show/hide action buttons based on listener
        if (actionListener != null) {
            holder.ivEdit.setVisibility(View.VISIBLE);
            holder.ivDelete.setVisibility(View.VISIBLE);
            holder.ivEdit.setOnClickListener(v -> actionListener.onEditEntry(entry));
            holder.ivDelete.setOnClickListener(v -> actionListener.onDeleteEntry(entry));
        } else {
            holder.ivEdit.setVisibility(View.GONE);
            holder.ivDelete.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    static class EntryViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvMorning, tvEvening, tvTotal, tvAmount;
        ImageView ivEdit, ivDelete;

        EntryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvMorning = itemView.findViewById(R.id.tvMorning);
            tvEvening = itemView.findViewById(R.id.tvEvening);
            tvTotal = itemView.findViewById(R.id.tvTotal);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            ivEdit = itemView.findViewById(R.id.ivEdit);
            ivDelete = itemView.findViewById(R.id.ivDelete);
        }
    }
}
