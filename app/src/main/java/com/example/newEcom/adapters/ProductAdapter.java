package com.example.newEcom.adapters;

import android.content.Context;
import android.graphics.Paint;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.newEcom.R;
import com.example.newEcom.fragments.ProductFragment;
import com.example.newEcom.model.ProductModel;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.text.DecimalFormat;

public class ProductAdapter extends FirestoreRecyclerAdapter<ProductModel, ProductAdapter.ProductViewHolder> {
    private Context context;
    private AppCompatActivity activity;
    private boolean isShimmerStopped = false;

    public ProductAdapter(@NonNull FirestoreRecyclerOptions<ProductModel> options, Context context) {
        super(options);
        this.context = context;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_product_adapter, parent, false);
        activity = (AppCompatActivity) view.getContext();
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position, @NonNull ProductModel product) {
        String imageUrl = product.getImage();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Picasso.get().load(imageUrl).into(holder.productImage, new Callback() {
                @Override
                public void onSuccess() {
                    if (!isShimmerStopped) {
                        ShimmerFrameLayout shimmerLayout = activity.findViewById(R.id.shimmerLayout);
                        shimmerLayout.setVisibility(View.GONE);
                        activity.findViewById(R.id.mainLinearLayout).setVisibility(View.VISIBLE);
                        isShimmerStopped = true; // Ensure it happens only once
                    }
                }

                @Override
                public void onError(Exception e) {
                    // Optionally handle the error here if needed, like logging the error
                }
            });
        }

        holder.productLabel.setText(product.getName());

        int originalPrice = product.getOriginalPrice();
        int price = product.getPrice();

        if (originalPrice > 0 && price < originalPrice) {
            // Show original price with strike-through and discount percentage
            holder.originalPrice.setText(formatPrice(originalPrice));
            holder.originalPrice.setPaintFlags(holder.originalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            int discountPerc = calculateDiscountPercentage(originalPrice, price);
            holder.discountPercentage.setText(discountPerc + "% OFF");
            holder.discountPercentage.setVisibility(View.VISIBLE);  // Show discount
        } else {
            // Hide original price and discount if there's no discount
            holder.originalPrice.setVisibility(View.GONE);
            holder.discountPercentage.setVisibility(View.GONE);
        }

        // Display the current price (sale price)
        holder.productPrice.setText(formatPrice(price));

        holder.itemView.setOnClickListener(v -> {
            Fragment fragment = ProductFragment.newInstance(product);
            activity.getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_frame_layout, fragment)
                    .addToBackStack(null)
                    .commit();
        });
    }

    private int calculateDiscountPercentage(int originalPrice, int price) {
        if (originalPrice > 0 && price < originalPrice) {
            return (int) (((float) (originalPrice - price) / originalPrice) * 100);
        }
        return 0;
    }

    private String formatPrice(int price) {
        if (price <= 0) {
            return "N/A";  // Fallback for invalid prices
        }
        DecimalFormat formatter = new DecimalFormat("###,###,###");
        return "Rs. " + formatter.format(price);
    }

    public class ProductViewHolder extends RecyclerView.ViewHolder {
        TextView productLabel, productPrice, originalPrice, discountPercentage;
        ImageView productImage;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.productListImage);
            productLabel = itemView.findViewById(R.id.productLabel);
            productPrice = itemView.findViewById(R.id.productPrice);
            originalPrice = itemView.findViewById(R.id.originalPrice);
            discountPercentage = itemView.findViewById(R.id.discountPercentage);
        }
    }
}
