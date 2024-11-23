package com.example.newEcom.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.newEcom.R;
import com.example.newEcom.model.ProductModel;
import com.example.newEcom.utils.FirebaseUtil;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class AddProductActivity extends AppCompatActivity {
    TextInputEditText idEditText, nameEditText, descEditText, specEditText, stockEditText, priceEditText, discountEditText;
    Button imageBtn, addProductBtn;
    ImageView backBtn, productImageView;
    TextView removeImageBtn;

    AutoCompleteTextView categoryDropDown;
    ArrayAdapter<String> arrayAdapter;
    String[] categories;
    String category, productImage;
    String productName;
    int productId;
    Context context = this;
    boolean imageUploaded = false;

    SweetAlertDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);

        initializeViews();
        setupProgressDialog();
        setupClickListeners();

        // Disable manual editing of ID
        idEditText.setEnabled(false);

        // Get initial product ID
        getNextProductId();
    }

    private void initializeViews() {
        idEditText = findViewById(R.id.idEditText);
        nameEditText = findViewById(R.id.nameEditText);
        categoryDropDown = findViewById(R.id.categoryDropDown);
        descEditText = findViewById(R.id.descriptionEditText);
        specEditText = findViewById(R.id.specificationEditText);
        stockEditText = findViewById(R.id.stockEditText);
        priceEditText = findViewById(R.id.priceEditText);
        discountEditText = findViewById(R.id.discountEditText);
        productImageView = findViewById(R.id.productImageView);

        imageBtn = findViewById(R.id.imageBtn);
        addProductBtn = findViewById(R.id.addProductBtn);
        backBtn = findViewById(R.id.backBtn);
        removeImageBtn = findViewById(R.id.removeImageBtn);
    }

    private void setupProgressDialog() {
        dialog = new SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE);
        dialog.getProgressHelper().setBarColor(Color.parseColor("#A5DC86"));
        dialog.setTitleText("Uploading image...");
        dialog.setCancelable(false);
    }

    private void setupClickListeners() {
        imageBtn.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(intent, 101);
        });

        addProductBtn.setOnClickListener(v -> {
            if (validate()) {
                addToFirebase();
            }
        });

        backBtn.setOnClickListener(v -> onBackPressed());

        removeImageBtn.setOnClickListener(v -> removeImage());
    }

    private void getNextProductId() {
        FirebaseUtil.getDetails().get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists() && document.contains("lastProductId")) {
                            productId = Integer.parseInt(document.get("lastProductId").toString()) + 1;
                            idEditText.setText(String.valueOf(productId));
                        } else {
                            // Initialize the details document if it doesn't exist
                            Map<String, Object> data = new HashMap<>();
                            data.put("lastProductId", 0);
                            FirebaseUtil.getDetails().set(data)
                                    .addOnSuccessListener(aVoid -> {
                                        productId = 1;
                                        idEditText.setText(String.valueOf(productId));
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(AddProductActivity.this,
                                                "Failed to initialize product ID", Toast.LENGTH_SHORT).show();
                                    });
                        }
                    } else {
                        Toast.makeText(AddProductActivity.this,
                                "Failed to get product ID: " +
                                        (task.getException() != null ? task.getException().getMessage() : "Unknown error"),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void getCategories(MyCallback myCallback) {
        int size[] = new int[1];

        FirebaseUtil.getCategories().orderBy("name")
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        size[0] = task.getResult().size();
                        myCallback.onCallback(size);
                    }
                });

        categories = new String[size[0]];

        FirebaseUtil.getCategories().orderBy("name")
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int i = 0;
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            categories[i] = ((String) document.getData().get("name"));
                            Log.i("Category", categories[i]);
                            i++;
                        }
                        myCallback.onCallback(categories);
                    }
                });
    }

    private void addToFirebase() {
        dialog.setTitleText("Adding Product...");
        dialog.show();

        productName = nameEditText.getText().toString();
        List<String> sk = Arrays.asList(productName.trim().toLowerCase().split(" "));
        String desc = descEditText.getText().toString();
        String spec = specEditText.getText().toString();
        int price = Integer.parseInt(priceEditText.getText().toString());
        int discount = Integer.parseInt(discountEditText.getText().toString());
        int stock = Integer.parseInt(stockEditText.getText().toString());

        ProductModel model = new ProductModel(
                productName,
                sk,
                productImage,
                category,
                desc,
                spec,
                price,
                discount,
                price - discount,
                productId,
                stock,
                null,
                0f,
                0
        );

        FirebaseUtil.getProducts().add(model)
                .addOnSuccessListener(documentReference -> {
                    FirebaseUtil.getDetails()
                            .update("lastProductId", productId)
                            .addOnSuccessListener(aVoid -> {
                                dialog.dismiss();
                                Toast.makeText(AddProductActivity.this,
                                        "Product added successfully!", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                dialog.dismiss();
                                Toast.makeText(AddProductActivity.this,
                                        "Failed to update product ID", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    dialog.dismiss();
                    Toast.makeText(AddProductActivity.this,
                            "Failed to add product: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private boolean validate() {
        boolean isValid = true;
        if (nameEditText.getText().toString().trim().isEmpty()) {
            nameEditText.setError("Name is required");
            isValid = false;
        }
        if (categoryDropDown.getText().toString().trim().isEmpty()) {
            categoryDropDown.setError("Category is required");
            isValid = false;
        }
        if (descEditText.getText().toString().trim().isEmpty()) {
            descEditText.setError("Description is required");
            isValid = false;
        }
        if (stockEditText.getText().toString().trim().isEmpty()) {
            stockEditText.setError("Stock is required");
            isValid = false;
        }
        if (priceEditText.getText().toString().trim().isEmpty()) {
            priceEditText.setError("Price is required");
            isValid = false;
        }
        if (!imageUploaded) {
            Toast.makeText(context, "Image is not selected", Toast.LENGTH_SHORT).show();
            isValid = false;
        }
        return isValid;
    }

    private void removeImage() {
        SweetAlertDialog alertDialog = new SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE);
        alertDialog
                .setTitleText("Are you sure?")
                .setContentText("Do you want to remove this image?")
                .setConfirmText("Yes")
                .setCancelText("No")
                .setConfirmClickListener(sweetAlertDialog -> {
                    imageUploaded = false;
                    productImageView.setImageDrawable(null);
                    productImageView.setVisibility(View.GONE);
                    removeImageBtn.setVisibility(View.GONE);

                    FirebaseUtil.getProductImageReference(productId + "").delete();
                    alertDialog.dismiss();
                }).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 101 && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            dialog.show();

            FirebaseUtil.getProductImageReference(productId + "").putFile(imageUri)
                    .addOnCompleteListener(t -> {
                        imageUploaded = true;

                        FirebaseUtil.getProductImageReference(productId + "").getDownloadUrl()
                                .addOnSuccessListener(uri -> {
                                    productImage = uri.toString();

                                    Picasso.get().load(uri).into(productImageView, new Callback() {
                                        @Override
                                        public void onSuccess() {
                                            dialog.dismiss();
                                        }

                                        @Override
                                        public void onError(Exception e) {
                                            dialog.dismiss();
                                            Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                    productImageView.setVisibility(View.VISIBLE);
                                    removeImageBtn.setVisibility(View.VISIBLE);
                                });
                    });
        }
    }

    public interface MyCallback {
        void onCallback(String[] categories);
        void onCallback(int[] size);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (productId > 0) {
            FirebaseUtil.getProductImageReference(productId + "").delete();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        getCategories(new MyCallback() {
            @Override
            public void onCallback(String[] cate) {
                arrayAdapter = new ArrayAdapter<>(context, R.layout.dropdown_item, cate);
                categoryDropDown.setAdapter(arrayAdapter);
                categoryDropDown.setOnItemClickListener((adapterView, view, i, l) -> {
                    category = adapterView.getItemAtPosition(i).toString();
                });
            }

            @Override
            public void onCallback(int[] size) {
                categories = new String[size[0]];
            }
        });
    }
}