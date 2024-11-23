package com.example.newEcom.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.newEcom.R;
import com.example.newEcom.adapters.OrderListAdapter;
import com.example.newEcom.model.OrderItemModel;
import com.example.newEcom.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;

public class ViewOrderActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private OrderListAdapter adapter;
    private ImageView backBtn;
    TextView countOrders, priceOrders;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_order);

        recyclerView = findViewById(R.id.orderRecyclerView);
        backBtn = findViewById(R.id.backBtn);
        countOrders = findViewById(R.id.countOrders);
        priceOrders = findViewById(R.id.priceOrders);

        setupRecyclerView();

        backBtn.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        Query query = FirebaseUtil.allOrdersCollectionReference()
                .orderBy("timestamp", Query.Direction.DESCENDING);

        FirestoreRecyclerOptions<OrderItemModel> options = new FirestoreRecyclerOptions.Builder<OrderItemModel>()
                .setQuery(query, OrderItemModel.class)
                .build();

        adapter = new OrderListAdapter(options, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void getDetails() {
        FirebaseUtil.getDetails().get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()){
                            countOrders.setText(task.getResult().get("countOfOrderedItems").toString());
                            priceOrders.setText(task.getResult().get("priceOfOrders").toString());
                        }
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        adapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        adapter.stopListening();
    }
}