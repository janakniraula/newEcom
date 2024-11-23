package com.example.newEcom.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.newEcom.R;
import com.example.newEcom.model.OrderItemModel;
import com.example.newEcom.utils.EmailSender;
import com.example.newEcom.utils.FirebaseUtil;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class CheckoutActivity extends AppCompatActivity {
    private static final String TAG = "CheckoutActivity";
    private static final int MINIMUM_FREE_DELIVERY_AMOUNT = 5000;
    private static final int DELIVERY_CHARGE = 50;

    // UI Components
    private TextView subtotalTextView, deliveryTextView, totalTextView, stockErrorTextView;
    private Button checkoutBtn;
    private ImageView backBtn;
    private EditText nameEditText, emailEditText, phoneEditText, addressEditText, commentEditText;
    private SweetAlertDialog progressDialog;

    // Order Data
    private int subTotal;
    private String name, email, phone, address, comment;
    private volatile boolean adequateStock = true;
    private int orderCount = 0;

    // Lists for order processing
    private List<String> productDocIds = new ArrayList<>();
    private List<Integer> stockLevels = new ArrayList<>();
    private List<Integer> quantities = new ArrayList<>();
    private List<String> insufficientStockProducts = new ArrayList<>();
    private List<String> cartDocuments = new ArrayList<>();
    private List<String> productNames = new ArrayList<>();
    private List<Integer> productPrices = new ArrayList<>();
    private List<Integer> productQuantities = new ArrayList<>();

    // Order details
    private int prevOrderId;
    private int totalOrderedItems;
    private int totalOrderValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        initializeViews();
        setupInitialData();
        setupListeners();
        setupProgressDialog();
    }

    private void initializeViews() {
        subtotalTextView = findViewById(R.id.subtotalTextView);
        deliveryTextView = findViewById(R.id.deliveryTextView);
        totalTextView = findViewById(R.id.totalTextView);
        stockErrorTextView = findViewById(R.id.stockErrorTextView);
        checkoutBtn = findViewById(R.id.checkoutBtn);
        backBtn = findViewById(R.id.backBtn);

        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        addressEditText = findViewById(R.id.addressEditText);
        commentEditText = findViewById(R.id.commentEditText);
    }

    private void setupInitialData() {
        subTotal = getIntent().getIntExtra("price", 0);
        updatePriceDisplay();
    }

    private void updatePriceDisplay() {
        subtotalTextView.setText(getString(R.string.currency_format, subTotal));

        if (subTotal >= MINIMUM_FREE_DELIVERY_AMOUNT) {
            deliveryTextView.setText(getString(R.string.currency_format, 0));
            totalTextView.setText(getString(R.string.currency_format, subTotal));
        } else {
            deliveryTextView.setText(getString(R.string.currency_format, DELIVERY_CHARGE));
            totalTextView.setText(getString(R.string.currency_format, subTotal + DELIVERY_CHARGE));
        }
    }

    private void setupListeners() {
        checkoutBtn.setOnClickListener(v -> {
            if (validateInputs()) {
                collectUserInput();
                fetchOrderDetails();
            }
        });
        backBtn.setOnClickListener(v -> onBackPressed());
    }

    private boolean validateInputs() {
        boolean isValid = true;

        if (nameEditText.getText().toString().trim().isEmpty()) {
            nameEditText.setError(getString(R.string.name_required));
            isValid = false;
        }

        String emailText = emailEditText.getText().toString().trim();
        if (emailText.isEmpty()) {
            emailEditText.setError(getString(R.string.email_required));
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(emailText).matches()) {
            emailEditText.setError(getString(R.string.invalid_email));
            isValid = false;
        }

        String phoneText = phoneEditText.getText().toString().trim();
        if (phoneText.isEmpty()) {
            phoneEditText.setError(getString(R.string.phone_required));
            isValid = false;
        } else if (phoneText.length() != 10) {
            phoneEditText.setError(getString(R.string.invalid_phone));
            isValid = false;
        }

        if (addressEditText.getText().toString().trim().isEmpty()) {
            addressEditText.setError(getString(R.string.address_required));
            isValid = false;
        }

        return isValid;
    }

    private void collectUserInput() {
        name = nameEditText.getText().toString().trim();
        email = emailEditText.getText().toString().trim();
        phone = phoneEditText.getText().toString().trim();
        address = addressEditText.getText().toString().trim();
        comment = commentEditText.getText().toString().trim();
    }

    private void fetchOrderDetails() {
        progressDialog.show();

        FirebaseUtil.getDetails().get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();
                        Number lastOrderIdNumber = document.getLong("lastOrderId");
                        Number countOfOrderedItemsNumber = document.getLong("countOfOrderedItems");
                        Number priceOfOrdersNumber = document.getLong("priceOfOrders");

                        prevOrderId = lastOrderIdNumber != null ? lastOrderIdNumber.intValue() : 0;
                        totalOrderedItems = countOfOrderedItemsNumber != null ? countOfOrderedItemsNumber.intValue() : 0;
                        totalOrderValue = priceOfOrdersNumber != null ? priceOfOrdersNumber.intValue() : 0;

                        processCartItems();
                    } else {
                        handleError("Failed to fetch order details");
                    }
                });
    }

    private void processCartItems() {
        FirebaseUtil.getCartItems().get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        handleCartItems(task.getResult());
                    } else {
                        handleError("Failed to fetch cart items");
                    }
                });
    }

    private void handleCartItems(QuerySnapshot result) {
        for (QueryDocumentSnapshot document : result) {
            orderCount++;
            cartDocuments.add(document.getId());
            productNames.add(document.getString("name"));
            productPrices.add(document.getLong("price").intValue());
            productQuantities.add(document.getLong("quantity").intValue());

            createOrderItem(document);
            checkProductStock(document);
        }

        new Handler().postDelayed(() -> {
            progressDialog.dismiss();
            if (adequateStock) {
                completeOrder();
            } else {
                showStockError();
            }
        }, 2000);
    }

    private void createOrderItem(QueryDocumentSnapshot document) {
        OrderItemModel item = new OrderItemModel(
                prevOrderId + 1,
                document.getLong("productId").intValue(),
                document.getString("name"),
                document.getString("image"),
                document.getLong("price").intValue(),
                document.getLong("quantity").intValue(),
                Timestamp.now(),
                name, email, phone, address, comment
        );

        FirebaseUtil.getOrderItems().add(item)
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to create order item", e));
    }

    private void checkProductStock(QueryDocumentSnapshot document) {
        FirebaseUtil.getProducts()
                .whereEqualTo("productId", document.getLong("productId"))
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        DocumentSnapshot productDoc = task.getResult().getDocuments().get(0);
                        processStockCheck(productDoc, document);
                    }
                });
    }

    private void processStockCheck(DocumentSnapshot productDoc, QueryDocumentSnapshot cartItem) {
        String docId = productDoc.getId();
        int currentStock = productDoc.getLong("stock").intValue();
        int requestedQuantity = cartItem.getLong("quantity").intValue();

        productDocIds.add(docId);
        stockLevels.add(currentStock);
        quantities.add(requestedQuantity);

        if (currentStock < requestedQuantity) {
            adequateStock = false;
            insufficientStockProducts.add(cartItem.getString("name"));
        }
    }

    private void showStockError() {
        StringBuilder errorMessage = new StringBuilder("*The following product(s) have less stock left:");
        for (int i = 0; i < insufficientStockProducts.size(); i++) {
            errorMessage.append(String.format("\n\t\t\t• %s has only %d stock left",
                    insufficientStockProducts.get(i), stockLevels.get(i)));
        }

        stockErrorTextView.setText(errorMessage.toString());
        stockErrorTextView.setVisibility(View.VISIBLE);
        Toast.makeText(this, R.string.insufficient_stock_message, Toast.LENGTH_LONG).show();
    }

    private void completeOrder() {
        updateOrderDetails();
        updateProductStock();
        clearCart();
        sendConfirmationEmail();
        showSuccessDialog();
    }

    private void updateOrderDetails() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("lastOrderId", prevOrderId + 1);
        updates.put("countOfOrderedItems", totalOrderedItems + orderCount);
        updates.put("priceOfOrders", totalOrderValue + subTotal);

        FirebaseUtil.getDetails().update(updates)
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update order details", e));
    }

    private void updateProductStock() {
        for (int i = 0; i < productDocIds.size(); i++) {
            int newStock = stockLevels.get(i) - quantities.get(i);
            FirebaseUtil.getProducts()
                    .document(productDocIds.get(i))
                    .update("stock", newStock)
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Failed to update product stock", e));
        }
    }

    private void clearCart() {
        for (String docId : cartDocuments) {
            FirebaseUtil.getCartItems()
                    .document(docId)
                    .delete()
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Failed to clear cart item", e));
        }
    }

    private void sendConfirmationEmail() {
        String subject = getString(R.string.order_confirmation_subject);
        String messageBody = createEmailBody();

        new EmailSender(subject, messageBody, email).sendEmail();
    }

    private String createEmailBody() {
        StringBuilder body = new StringBuilder();
        body.append(String.format("Dear %s,\n\n", name));
        body.append(getString(R.string.order_confirmation_intro));
        body.append("\n\nOrder Details:\n");
        body.append("-----------------------------------------------------------------------------------\n");
        body.append(String.format("%-50s %-10s %-10s\n", "Product Name", "Quantity", "Price"));
        body.append("-----------------------------------------------------------------------------------\n");

        for (int i = 0; i < productNames.size(); i++) {
            body.append(String.format("%-50s %-10s ₹%-10d\n",
                    productNames.get(i),
                    productQuantities.get(i),
                    productPrices.get(i)));
        }

        body.append("-----------------------------------------------------------------------------\n");
        body.append(String.format("%-73s ₹%-10d\n", "Total:", subTotal));
        body.append("-----------------------------------------------------------------------------\n\n");
        body.append(getString(R.string.order_confirmation_footer));

        return body.toString();
    }

    private void setupProgressDialog() {
        progressDialog = new SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE);
        progressDialog.getProgressHelper().setBarColor(Color.parseColor("#A5DC86"));
        progressDialog.setTitleText(getString(R.string.loading));
        progressDialog.setCancelable(false);
    }

    private void showSuccessDialog() {
        new SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                .setTitleText(getString(R.string.order_success_title))
                .setContentText(getString(R.string.order_success_message))
                .setConfirmClickListener(sweetAlertDialog -> {
                    Intent intent = new Intent(CheckoutActivity.this, MainActivity.class);
                    intent.putExtra("orderPlaced", true);
                    startActivity(intent);
                    finish();
                }).show();
    }

    private void handleError(String message) {
        progressDialog.dismiss();
        new SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                .setTitleText(getString(R.string.order_failed_title))
                .setContentText(getString(R.string.order_failed_message))
                .setConfirmClickListener(sweetAlertDialog -> {
                    Intent intent = new Intent(this, MainActivity.class);
                    startActivity(intent);
                    finish();
                }).show();
        Log.e(TAG, message);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }
}