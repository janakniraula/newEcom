package com.example.newEcom.activities;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import java.util.Arrays;
import java.util.List;
import com.airbnb.lottie.LottieAnimationView;
import com.example.newEcom.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {
    private static final long SPLASH_DELAY = 3000; // 3 seconds
    private static final List<String> ADMIN_EMAILS = Arrays.asList(
            "Sthalotus11@gmail.com",
            "niraulajanak2019@gmail.com"
    );

    private LottieAnimationView lottieAnimation;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        initializeViews();
        initializeFirebase();
        startSplashAnimation();
    }

    private void initializeViews() {
        lottieAnimation = findViewById(R.id.lottieAnimationView);
    }

    private void initializeFirebase() {
        firebaseAuth = FirebaseAuth.getInstance();
    }

    private void startSplashAnimation() {
        if (lottieAnimation != null) {
            lottieAnimation.playAnimation();
        }

        new Handler().postDelayed(this::navigateToappropriateScreen, SPLASH_DELAY);
    }

    private void navigateToappropriateScreen() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        Intent intent;

        if (currentUser == null) {
            intent = new Intent(this, LoginActivity.class);
        } else {
            String userEmail = currentUser.getEmail();
            if (userEmail != null && ADMIN_EMAILS.contains(userEmail)) {
                intent = new Intent(this, AdminActivity.class);
            } else {
                intent = new Intent(this, MainActivity.class);
            }
        }

        startActivity(intent);
        finish();
    }
}