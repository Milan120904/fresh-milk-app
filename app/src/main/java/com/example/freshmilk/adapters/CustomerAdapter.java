package com.example.freshmilk.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.freshmilk.R;
import com.example.freshmilk.models.Customer;

import java.util.List;
import java.util.Locale;

public class CustomerAdapter extends RecyclerView.Adapter<CustomerAdapter.CustomerViewHolder> {

    private final List<Customer> customers;
    private final OnCustomerClickListener listener;

    public interface OnCustomerClickListener {
        void onCustomerClick(Customer customer);
    }

    public CustomerAdapter(List<Customer> customers, OnCustomerClickListener listener) {
        this.customers = customers;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CustomerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_customer, parent, false);
        return new CustomerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CustomerViewHolder holder, int position) {
        Customer customer = customers.get(position);
        holder.bind(customer);
    }

    @Override
    public int getItemCount() {
        return customers.size();
    }

    class CustomerViewHolder extends RecyclerView.ViewHolder {
        TextView tvInitials, tvName, tvMobile, tvMilkType, tvQuantity;
        android.widget.ImageView btnEdit, btnCall;

        CustomerViewHolder(@NonNull View itemView) {
            super(itemView);
            tvInitials = itemView.findViewById(R.id.tvInitials);
            tvName = itemView.findViewById(R.id.tvName);
            tvMobile = itemView.findViewById(R.id.tvMobile);
            tvMilkType = itemView.findViewById(R.id.tvMilkType);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnCall = itemView.findViewById(R.id.btnCall);
        }

        void bind(Customer customer) {
            if (customer.getName() != null && !customer.getName().isEmpty()) {
                String initial = customer.getName().substring(0, 1).toUpperCase();
                tvInitials.setText(initial);
                tvName.setText(customer.getName());
            }
            
            tvMobile.setText(customer.getMobile());
            tvMilkType.setText(customer.getMilkType());
            tvQuantity.setText(String.format(Locale.getDefault(), "%.1f L (₹%.0f/L)", customer.getDefaultQuantity(), customer.getRatePerLiter()));

            itemView.setOnClickListener(v -> {
                if (listener != null)
                    listener.onCustomerClick(customer);
            });
            
            if (btnEdit != null) {
                btnEdit.setOnClickListener(v -> {
                    if (listener != null)
                        listener.onCustomerClick(customer);
                });
            }
            
            if (btnCall != null) {
                btnCall.setOnClickListener(v -> {
                    android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_DIAL);
                    intent.setData(android.net.Uri.parse("tel:" + customer.getMobile()));
                    itemView.getContext().startActivity(intent);
                });
            }
        }
    }
}
