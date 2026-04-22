package com.example.freshmilk.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.freshmilk.R;
import com.example.freshmilk.models.Payment;

import java.util.List;
import java.util.Locale;

public class PaymentAdapter extends RecyclerView.Adapter<PaymentAdapter.PaymentViewHolder> {

    private final List<Payment> payments;
    private final OnPaymentClickListener listener;

    public interface OnPaymentClickListener {
        void onPaymentClick(Payment payment);
    }

    public PaymentAdapter(List<Payment> payments, @Nullable OnPaymentClickListener listener) {
        this.payments = payments;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PaymentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_payment, parent, false);
        return new PaymentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PaymentViewHolder holder, int position) {
        Payment payment = payments.get(position);
        holder.bind(payment);
    }

    @Override
    public int getItemCount() {
        return payments.size();
    }

    class PaymentViewHolder extends RecyclerView.ViewHolder {
        TextView tvCustomerName, tvMonthYear, tvAmount, tvStatus, tvPaymentMethod;

        PaymentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
            tvMonthYear = itemView.findViewById(R.id.tvMonthYear);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvPaymentMethod = itemView.findViewById(R.id.tvPaymentMethod);
        }

        void bind(Payment payment) {
            tvCustomerName.setText(payment.getCustomerName());
            tvMonthYear.setText(payment.getMonthYearString());
            tvAmount.setText(String.format(Locale.getDefault(), "₹%.0f", payment.getTotalAmount()));

            tvStatus.setText(payment.getStatus());
            int statusColor;
            switch (payment.getStatus()) {
                case Payment.STATUS_PAID:
                    statusColor = itemView.getContext().getColor(R.color.status_paid);
                    break;
                case Payment.STATUS_PARTIAL:
                    statusColor = itemView.getContext().getColor(R.color.status_partial);
                    break;
                case Payment.STATUS_PROCESSING:
                    statusColor = itemView.getContext().getColor(R.color.status_pending_verification);
                    tvStatus.setText("⏳ Processing");
                    break;
                case Payment.STATUS_REJECTED:
                    statusColor = itemView.getContext().getColor(R.color.status_rejected);
                    tvStatus.setText("❌ Rejected");
                    break;
                default:
                    statusColor = itemView.getContext().getColor(R.color.status_unpaid);
                    break;
            }
            tvStatus.setTextColor(statusColor);

            // Show payment method if available
            if (payment.getPaymentMethod() != null && !payment.getPaymentMethod().isEmpty()) {
                tvPaymentMethod.setVisibility(View.VISIBLE);
                String methodLabel;
                int methodColor;
                switch (payment.getPaymentMethod()) {
                    case Payment.METHOD_UPI:
                        methodLabel = "via UPI";
                        methodColor = itemView.getContext().getColor(R.color.payment_upi);
                        break;
                    case Payment.METHOD_QR:
                        methodLabel = "via QR";
                        methodColor = itemView.getContext().getColor(R.color.payment_qr);
                        break;
                    case Payment.METHOD_CARD:
                        methodLabel = "via Card";
                        methodColor = itemView.getContext().getColor(R.color.payment_card);
                        break;
                    default:
                        methodLabel = payment.getPaymentMethod();
                        methodColor = itemView.getContext().getColor(R.color.on_surface_medium);
                        break;
                }
                tvPaymentMethod.setText(methodLabel);
                tvPaymentMethod.setTextColor(methodColor);
            } else {
                tvPaymentMethod.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null)
                    listener.onPaymentClick(payment);
            });
        }
    }
}
