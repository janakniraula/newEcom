package com.example.newEcom.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import com.example.newEcom.R;
import com.example.newEcom.fragments.CartFragment;
import com.example.newEcom.fragments.HomeFragment;
import com.example.newEcom.fragments.ProductFragment;
import com.example.newEcom.fragments.ProfileFragment;
import com.example.newEcom.fragments.SearchFragment;
import com.example.newEcom.fragments.WishlistFragment;
import com.example.newEcom.utils.FirebaseUtil;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.firestore.QuerySnapshot;
import com.mancj.materialsearchbar.MaterialSearchBar;
import com.mancj.materialsearchbar.SimpleOnSearchActionListener;

public class MainActivity extends AppCompatActivity {
    BottomNavigationView bottomNavigationView;
    HomeFragment homeFragment;
    CartFragment cartFragment;
    SearchFragment searchFragment;
    WishlistFragment wishlistFragment;
    ProfileFragment profileFragment;
    LinearLayout searchLinearLayout;
    MaterialSearchBar searchBar;

    FragmentManager fm;
    FragmentTransaction transaction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupFragments();
        setupBottomNavigation();
        setupSearchBar();
        handleOrderPlacedIntent();
    }

    private void initializeViews() {
        searchLinearLayout = findViewById(R.id.linearLayout);
        searchBar = findViewById(R.id.searchBar);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
    }

    private void setupFragments() {
        homeFragment = new HomeFragment();
        cartFragment = new CartFragment();
        wishlistFragment = new WishlistFragment();
        profileFragment = new ProfileFragment();
        searchFragment = new SearchFragment();
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                fm = getSupportFragmentManager();
                transaction = fm.beginTransaction();

                if (item.getItemId() == R.id.home) {
                    fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    transaction.replace(R.id.main_frame_layout, homeFragment, "home");
                } else if (item.getItemId() == R.id.cart) {
                    if (!cartFragment.isAdded()) {
                        transaction.replace(R.id.main_frame_layout, cartFragment, "cart");
                        transaction.addToBackStack(null);
                    }
                } else if (item.getItemId() == R.id.wishlist) {
                    if (!wishlistFragment.isAdded()) {
                        transaction.replace(R.id.main_frame_layout, wishlistFragment, "wishlist");
                        transaction.addToBackStack(null);
                    }
                } else if (item.getItemId() == R.id.profile) {
                    if (!profileFragment.isAdded()) {
                        transaction.replace(R.id.main_frame_layout, profileFragment, "profile");
                        transaction.addToBackStack(null);
                    }
                }
                transaction.commit();
                return true;
            }
        });

        bottomNavigationView.setSelectedItemId(R.id.home);
        addOrRemoveBadge();

        getSupportFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                updateBottomNavigationSelectedItem();
            }
        });
    }

    private void setupSearchBar() {
        searchBar.setOnSearchActionListener(new SimpleOnSearchActionListener() {
            @Override
            public void onSearchStateChanged(boolean enabled) {
                super.onSearchStateChanged(enabled);
            }

            @Override
            public void onSearchConfirmed(CharSequence text) {
                if (!searchFragment.isAdded()) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.main_frame_layout, searchFragment, "search")
                            .addToBackStack(null)
                            .commit();
                }
                super.onSearchConfirmed(text);
            }

            @Override
            public void onButtonClicked(int buttonCode) {
                super.onButtonClicked(buttonCode);
            }
        });
    }

    private void handleOrderPlacedIntent() {
        if (getIntent().getBooleanExtra("orderPlaced", false)) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_frame_layout, profileFragment, "profile")
                    .addToBackStack(null)
                    .commit();
            bottomNavigationView.setSelectedItemId(R.id.profile);
        }
    }

    public void showSearchBar() {
        searchLinearLayout.setVisibility(View.VISIBLE);
    }

    public void hideSearchBar() {
        searchLinearLayout.setVisibility(View.GONE);
    }

    @Override
    public void onBackPressed() {
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    private void updateBottomNavigationSelectedItem() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.main_frame_layout);

        if (currentFragment instanceof HomeFragment) {
            bottomNavigationView.setSelectedItemId(R.id.home);
        } else if (currentFragment instanceof CartFragment) {
            bottomNavigationView.setSelectedItemId(R.id.cart);
        } else if (currentFragment instanceof WishlistFragment) {
            bottomNavigationView.setSelectedItemId(R.id.wishlist);
        } else if (currentFragment instanceof ProfileFragment) {
            bottomNavigationView.setSelectedItemId(R.id.profile);
        }
    }

    public void addOrRemoveBadge() {
        FirebaseUtil.getCartItems().get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            int n = task.getResult().size();
                            BadgeDrawable badge = bottomNavigationView.getOrCreateBadge(R.id.cart);
                            badge.setBackgroundColor(Color.parseColor("#FFF44336"));
                            if (n > 0) {
                                badge.setVisible(true);
                                badge.setNumber(n);
                            } else {
                                badge.setVisible(false);
                                badge.clearNumber();
                            }
                        }
                    }
                });
    }
}