package com.example.newEcom.fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.newEcom.R;
import com.example.newEcom.activities.MainActivity;
import com.example.newEcom.activities.SplashActivity;
import com.example.newEcom.adapters.OrderListAdapter;
import com.example.newEcom.model.OrderItemModel;
import com.example.newEcom.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.Query;

public class ProfileFragment extends Fragment {
    RecyclerView orderRecyclerView;
    OrderListAdapter orderAdapter;
    LinearLayout logoutBtn;
    TextView userNameTextView;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        orderRecyclerView = view.findViewById(R.id.orderRecyclerView);
        logoutBtn = view.findViewById(R.id.logoutBtn);
        userNameTextView = view.findViewById(R.id.userNameTextView);

        userNameTextView.setText("Hello, " + FirebaseAuth.getInstance().getCurrentUser().getDisplayName());
        logoutBtn.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(getActivity(), SplashActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
        getOrderProducts();

        MainActivity activity = (MainActivity) getActivity();
        activity.hideSearchBar();

        return view;
    }

    private void getOrderProducts() {
        Query query = FirebaseUtil.getOrderItems().orderBy("timestamp", Query.Direction.DESCENDING);
        FirestoreRecyclerOptions<OrderItemModel> options = new FirestoreRecyclerOptions.Builder<OrderItemModel>()
                .setQuery(query, OrderItemModel.class)
                .build();

        orderAdapter = new OrderListAdapter(options, getActivity());

        // Handle long-click for delete confirmation
        orderAdapter.setOnItemLongClickListener((order, position) -> showDeleteConfirmationDialog(order, position));

        LinearLayoutManager manager = new LinearLayoutManager(getActivity());
        orderRecyclerView.setLayoutManager(manager);
        orderRecyclerView.setAdapter(orderAdapter);
        orderAdapter.startListening();
    }

    private void showDeleteConfirmationDialog(OrderItemModel order, int position) {
        new AlertDialog.Builder(getActivity())
                .setTitle("Cancel Order")
                .setMessage("Are you sure you want to cancel this order?")
                .setPositiveButton("Delete", (dialog, which) -> deleteOrder(order, position))
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void deleteOrder(OrderItemModel order, int position) {
        FirebaseUtil.getOrderItems()
                .document(String.valueOf(order.getOrderId())) // Ensure this matches your Firestore document ID
                .delete()
                .addOnSuccessListener(aVoid -> Toast.makeText(getActivity(), "Order canceled successfully", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(getActivity(), "Failed to cancel order: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
