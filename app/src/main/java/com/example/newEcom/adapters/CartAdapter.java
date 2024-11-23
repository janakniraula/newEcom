package com.example.newEcom.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.newEcom.R;
import com.example.newEcom.activities.MainActivity;
import com.example.newEcom.model.CartItemModel;
import com.example.newEcom.utils.FirebaseUtil;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

public class CartAdapter extends FirestoreRecyclerAdapter<CartItemModel, CartAdapter.CartViewHolder> {
    private static final String TAG = "CartAdapter";
    private static final String PRICE_BROADCAST_ACTION = "price";
    private static final String TOTAL_PRICE_EXTRA = "totalPrice";

    private final Context context;
    private AppCompatActivity activity;
    private int totalPrice = 0;
    private boolean gotSum = false;

    public CartAdapter(@NonNull FirestoreRecyclerOptions<CartItemModel> options, Context context) {
        super(options);
        this.context = context;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_cart_adapter, parent, false);
        activity = (AppCompatActivity) view.getContext();
        return new CartViewHolder(view);
    }

    @Override
    protected void onBindViewHolder(@NonNull CartViewHolder holder, int position, @NonNull CartItemModel model) {
        try {
            bindProductDetails(holder, model);
            loadProductImage(holder, model);
            setupQuantityButtons(holder, model);

            if (position == getItemCount() - 1 && !gotSum) {
                calculateTotalPrice();
            }
        } catch (Exception e) {
            handleBindingError(e);
        }
    }

    private void bindProductDetails(@NonNull CartViewHolder holder, @NonNull CartItemModel model) {
        String currencySymbol = context.getString(R.string.currency_symbol);
        holder.productName.setText(model.getName());
        holder.singleProductPrice.setText(String.format("%s%d", currencySymbol, model.getPrice()));
        holder.productPrice.setText(String.format("%s%d", currencySymbol, model.getPrice() * model.getQuantity()));
        holder.originalPrice.setText(String.format("%s%d", currencySymbol, model.getOriginalPrice()));
        holder.originalPrice.setPaintFlags(holder.originalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        holder.productQuantity.setText(String.valueOf(model.getQuantity()));
    }

    private void loadProductImage(@NonNull CartViewHolder holder, @NonNull CartItemModel model) {
        Picasso.get()
                .load(model.getImage())
                .into(holder.productCartImage, new Callback() {
                    @Override
                    public void onSuccess() {
                        if (holder.getBindingAdapterPosition() == getSnapshots().size() - 1) {
                            hideShimmer();
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        handleImageLoadError(e);
                    }
                });
    }

    private void setupQuantityButtons(@NonNull CartViewHolder holder, @NonNull CartItemModel model) {
        holder.plusBtn.setOnClickListener(v -> changeQuantity(model, true));
        holder.minusBtn.setOnClickListener(v -> changeQuantity(model, false));
    }

    private void hideShimmer() {
        if (activity != null) {
            ShimmerFrameLayout shimmerLayout = activity.findViewById(R.id.shimmerLayout);
            if (shimmerLayout != null) {
                shimmerLayout.stopShimmer();
                shimmerLayout.setVisibility(View.GONE);
            }
            View mainLayout = activity.findViewById(R.id.mainLinearLayout);
            if (mainLayout != null) {
                mainLayout.setVisibility(View.VISIBLE);
            }
        }
    }

    private void calculateTotalPrice() {
        if (gotSum) return;
        gotSum = true;
        totalPrice = 0;

        for (CartItemModel model : getSnapshots()) {
            totalPrice += model.getPrice() * model.getQuantity();
        }

        broadcastTotalPrice();
    }

    private void broadcastTotalPrice() {
        Intent intent = new Intent(PRICE_BROADCAST_ACTION);
        intent.putExtra(TOTAL_PRICE_EXTRA, totalPrice);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    private void changeQuantity(CartItemModel model, boolean increment) {
        FirebaseUtil.getProducts()
                .whereEqualTo("productId", model.getProductId())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int stock = getStockFromQuerySnapshot(querySnapshot);
                    updateCartItemQuantity(model, increment, stock);
                })
                .addOnFailureListener(this::handleFirebaseError);
    }

    private int getStockFromQuerySnapshot(QuerySnapshot querySnapshot) {
        for (QueryDocumentSnapshot document : querySnapshot) {
            return (int) (long) document.getData().get("stock");
        }
        return 0;
    }

    private void updateCartItemQuantity(CartItemModel model, boolean increment, int stock) {
        FirebaseUtil.getCartItems()
                .whereEqualTo("productId", model.getProductId())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        String docId = document.getId();
                        int quantity = (int) (long) document.getData().get("quantity");

                        if (increment) {
                            handleQuantityIncrease(model, stock, docId, quantity);
                        } else {
                            handleQuantityDecrease(model, docId, quantity);
                        }
                    }
                })
                .addOnFailureListener(this::handleFirebaseError);
    }

    private void handleQuantityIncrease(CartItemModel model, int stock, String docId, int quantity) {
        if (quantity < stock) {
            FirebaseUtil.getCartItems().document(docId).update("quantity", quantity + 1);
            totalPrice += model.getPrice();
            updateTotalPrice();
        } else {
            showToast("Max stock available: " + stock);
        }
    }

    private void handleQuantityDecrease(CartItemModel model, String docId, int quantity) {
        totalPrice -= model.getPrice();
        if (quantity > 1) {
            FirebaseUtil.getCartItems().document(docId).update("quantity", quantity - 1);
        } else {
            FirebaseUtil.getCartItems().document(docId).delete();
        }
        updateTotalPrice();
    }

    private void updateTotalPrice() {
        broadcastTotalPrice();
        updateMainActivityBadge();
    }

    private void updateMainActivityBadge() {
        if (context instanceof MainActivity) {
            ((MainActivity) context).addOrRemoveBadge();
        }
    }

    @Override
    public void onDataChanged() {
        super.onDataChanged();
        if (getItemCount() == 0) {
            handleEmptyCart();
        } else {
            hideEmptyCartImage();
        }
    }

    private void handleEmptyCart() {
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            hideShimmerAndShowMainLayout(activity);
            showEmptyCartImage(activity);
        }
    }

    private void hideShimmerAndShowMainLayout(Activity activity) {
        ShimmerFrameLayout shimmerLayout = activity.findViewById(R.id.shimmerLayout);
        if (shimmerLayout != null) {
            shimmerLayout.stopShimmer();
            shimmerLayout.setVisibility(View.GONE);
        }
        View mainLayout = activity.findViewById(R.id.mainLinearLayout);
        if (mainLayout != null) {
            mainLayout.setVisibility(View.VISIBLE);
        }
    }

    private void showEmptyCartImage(Activity activity) {
        View emptyCartImageView = activity.findViewById(R.id.emptyCartImageView);
        if (emptyCartImageView != null) {
            emptyCartImageView.setVisibility(View.VISIBLE);
        }
    }

    private void hideEmptyCartImage() {
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            View emptyCartImageView = activity.findViewById(R.id.emptyCartImageView);
            if (emptyCartImageView != null) {
                emptyCartImageView.setVisibility(View.INVISIBLE);
            }
        }
    }

    private void handleBindingError(Exception e) {
        Log.e(TAG, "Error binding view holder: ", e);
        showToast("Error loading cart item");
    }

    private void handleImageLoadError(Exception e) {
        Log.e(TAG, "Error loading image: ", e);
    }

    private void handleFirebaseError(Exception e) {
        Log.e(TAG, "Firebase operation failed: ", e);
        showToast("Error updating cart");
    }

    private void showToast(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public static class CartViewHolder extends RecyclerView.ViewHolder {
        TextView productName, productPrice, singleProductPrice, productQuantity, minusBtn, plusBtn, originalPrice;
        ImageView productCartImage;

        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            productName = itemView.findViewById(R.id.nameTextView);
            singleProductPrice = itemView.findViewById(R.id.priceTextView1);
            productPrice = itemView.findViewById(R.id.priceTextView);
            originalPrice = itemView.findViewById(R.id.originalPrice);
            productQuantity = itemView.findViewById(R.id.quantityTextView);
            productCartImage = itemView.findViewById(R.id.productImageCart);
            minusBtn = itemView.findViewById(R.id.minusBtn);
            plusBtn = itemView.findViewById(R.id.plusBtn);
        }
    }
}