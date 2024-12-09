package com.example.taskmate;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class login extends AppCompatActivity {

    TextInputEditText editTextEmail, editTextPassword;
    Button buttonLogin;
    FirebaseAuth mAuth;
    ProgressBar progressBar;
    TextView textView;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialisation de FirebaseAuth et Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialisation des vues
        textView = findViewById(R.id.registerNow);
        editTextEmail = findViewById(R.id.email);
        editTextPassword = findViewById(R.id.password);
        buttonLogin = findViewById(R.id.btn_login);
        progressBar = findViewById(R.id.progressBar);

        // Redirection vers la page d'inscription
        textView.setOnClickListener(view -> {
            Intent intent = new Intent(getApplicationContext(), register.class);
            startActivity(intent);
            finish();
        });

        // Gestion du bouton de connexion
        buttonLogin.setOnClickListener(view -> {
            progressBar.setVisibility(View.VISIBLE);

            String email = String.valueOf(editTextEmail.getText()).trim();
            String password = String.valueOf(editTextPassword.getText()).trim();

            // Validation des champs
            if (TextUtils.isEmpty(email)) {
                Toast.makeText(login.this, "Enter email", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
                return;
            }
            if (TextUtils.isEmpty(password)) {
                Toast.makeText(login.this, "Enter password", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
                return;
            }

            // Connexion de l'utilisateur
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        progressBar.setVisibility(View.GONE);

                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                // Vérification de l'e-mail
                                if (user.isEmailVerified()) {
                                    // L'utilisateur a vérifié son e-mail, on l'ajoute dans Firestore
                                    User newUser = new User(user.getDisplayName(), user.getUid(), user.getEmail(), System.currentTimeMillis());

                                    // Ajouter l'utilisateur dans la collection 'users' dans Firestore
                                    db.collection("users")
                                            .document(user.getUid())
                                            .set(newUser)
                                            .addOnSuccessListener(aVoid -> {
                                                // Succès
                                                Toast.makeText(login.this, "Login Successful", Toast.LENGTH_SHORT).show();
                                                Intent intent = new Intent(getApplicationContext(), ProjectListActivity.class);
                                                startActivity(intent);
                                                finish();
                                            })
                                            .addOnFailureListener(e -> {
                                                // Échec d'ajout dans Firestore
                                                Toast.makeText(login.this, "Failed to add user to Firestore", Toast.LENGTH_SHORT).show();
                                            });

                                } else {
                                    // Si l'e-mail n'est pas vérifié
                                    Toast.makeText(login.this, "Please verify your email before logging in.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        } else {
                            // Échec de la connexion
                            Toast.makeText(login.this, "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }
}
