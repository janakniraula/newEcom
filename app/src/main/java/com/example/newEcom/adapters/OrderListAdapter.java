package com.example.newEcom.adapters;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.newEcom.R;
import com.example.newEcom.fragments.OrderDetailsFragment;
import com.example.newEcom.model.OrderItemModel;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.Timestamp;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;

public class OrderListAdapter extends FirestoreRecyclerAdapter<OrderItemModel, OrderListAdapter.OrderListViewHolder> {
    private Context context;
    private AppCompatActivity activity;
    private OnItemLongClickListener longClickListener;

    public interface OnItemLongClickListener {
        void onItemLongClick(OrderItemModel order, int position);
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }

    public OrderListAdapter(@NonNull FirestoreRecyclerOptions<OrderItemModel> options, Context context) {
        super(options);
        this.context = context;
    }

    @NonNull
    @Override
    public OrderListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_order_adapter, parent, false);
        activity = (AppCompatActivity) view.getContext();
        return new OrderListViewHolder(view);
    }

    @Override
    protected void onBindViewHolder(@NonNull OrderListViewHolder holder, int position, @NonNull OrderItemModel model) {
        holder.productName.setText(model.getName());
        Timestamp timestamp = model.getTimestamp();
        String time = new SimpleDateFormat("dd MMM yyyy").format(timestamp.toDate());
        holder.orderDate.setText(time);
        Picasso.get().load(model.getImage()).into(holder.productImage);

        Bundle bundle = new Bundle();
        bundle.putInt("orderId", model.getOrderId());
        OrderDetailsFragment fragment = new OrderDetailsFragment();
        fragment.setArguments(bundle);

        holder.itemView.setOnClickListener(v -> {
            if (!fragment.isAdded())
                activity.getSupportFragmentManager().beginTransaction()
                        .replace(R.id.main_frame_layout, fragment)
                        .addToBackStack(null)
                        .commit();
        });

        // Add long-click logic to delete the order
        holder.itemView.setOnLongClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Delete Order")
                    .setMessage("Are you sure you want to delete this order?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        // Delete the document from Firestore
                        getSnapshots().getSnapshot(holder.getBindingAdapterPosition()).getReference()
                                .delete()
                                .addOnSuccessListener(aVoid ->
                                        Toast.makeText(context, "Order deleted successfully!", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e ->
                                        Toast.makeText(context, "Failed to delete order!", Toast.LENGTH_SHORT).show());
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        });
    }


    public class OrderListViewHolder extends RecyclerView.ViewHolder {
        ImageView productImage;
        TextView productName, orderDate;

        public OrderListViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.productImageOrder);
            productName = itemView.findViewById(R.id.nameTextView);
            orderDate = itemView.findViewById(R.id.dateTextView);
        }
    }
}
