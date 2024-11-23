package com.example.newEcom.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.transition.Explode;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.List;

import com.example.newEcom.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {
    private static final List<String> ADMIN_EMAILS = Arrays.asList(
            "Sthalotus11@gmail.com",
            "niraulajanak2019@gmail.com"
    );

    ProgressBar progressBar;
    EditText emailEditText, passEditText;
    ImageView loginBtn;
    TextView signupPageBtn;
    Button googleLoginBtn;

    FirebaseAuth auth;
    GoogleSignInClient googleSignInClient;
    Activity activity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        activity = this;

        progressBar = findViewById(R.id.progress_bar);
        emailEditText = findViewById(R.id.emailEditText);
        passEditText = findViewById(R.id.passEditText);
        loginBtn = findViewById(R.id.loginBtn);
        signupPageBtn = findViewById(R.id.signupPageBtn);
        googleLoginBtn = findViewById(R.id.googleLoginBtn);

        loginBtn.setOnClickListener(v -> loginUser());

        signupPageBtn.setOnClickListener(v -> {
            startActivity(new Intent(this, SignupActivity.class));
        });

        auth = FirebaseAuth.getInstance();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail().build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

        googleLoginBtn.setOnClickListener(v -> googleSigning());

        getWindow().setExitTransition(new Explode());
    }

    private void loginUser() {
        String email = emailEditText.getText().toString();
        String pass = passEditText.getText().toString();
        boolean isValidated = validate(email, pass);
        if (!isValidated)
            return;
        loginAccountInFirebase(email, pass);
    }

    private void loginAccountInFirebase(String email, String pass) {
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        changeInProgress(true);
        firebaseAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                changeInProgress(false);
                if (task.isSuccessful()) {
                    if (firebaseAuth.getCurrentUser().isEmailVerified()) {
                        if (ADMIN_EMAILS.contains(firebaseAuth.getCurrentUser().getEmail()))
                            startActivity(new Intent(LoginActivity.this, AdminActivity.class),
                                    ActivityOptions.makeSceneTransitionAnimation(activity).toBundle());
                        else
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, "Email not verified, please verify your email", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "Something went wrong!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void changeInProgress(boolean inProgress) {
        if (inProgress) {
            progressBar.setVisibility(View.VISIBLE);
            loginBtn.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
            loginBtn.setVisibility(View.VISIBLE);
        }
    }

    private boolean validate(String email, String pass) {
        int flag = 0;
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Email is invalid");
            flag = 1;
        }
        if (pass.length() < 6) {
            passEditText.setError("Password must be of six characters");
            flag = 1;
        }
        return flag == 0;
    }

    private void googleSigning() {
        Intent intent = googleSignInClient.getSignInIntent();
        startActivityForResult(intent, 101);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 101) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuth(account.getIdToken());
            } catch (Exception e) {
                Toast.makeText(this, "Something went wrong!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuth(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            String email = auth.getCurrentUser().getEmail();
                            if (ADMIN_EMAILS.contains(email))  // Admin check
                                startActivity(new Intent(LoginActivity.this, AdminActivity.class));
                            else
                                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        } else
                            Toast.makeText(LoginActivity.this, "Something went wrong!", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}