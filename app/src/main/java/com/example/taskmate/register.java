package com.example.taskmate;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.material.textfield.TextInputEditText;

public class register extends AppCompatActivity {

    TextInputEditText editTextEmail, editTextPassword, editTextName;
    Button buttonReg;
    FirebaseAuth mAuth;
    FirebaseFirestore db;
    ProgressBar progressBar;
    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialisation de FirebaseAuth et Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Récupération des éléments de l'interface utilisateur
        textView = findViewById(R.id.btn_login);
        editTextEmail = findViewById(R.id.email);
        editTextPassword = findViewById(R.id.password);
        editTextName = findViewById(R.id.name);
        buttonReg = findViewById(R.id.btn_register);
        progressBar = findViewById(R.id.progressBar);

        // Redirection vers la page de connexion
        textView.setOnClickListener(view -> {
            Intent intent = new Intent(getApplicationContext(), login.class);
            startActivity(intent);
            finish();
        });

        // Gestion du bouton d'inscription
        buttonReg.setOnClickListener(view -> {
            progressBar.setVisibility(View.VISIBLE);
            String email = String.valueOf(editTextEmail.getText()).trim();
            String password = String.valueOf(editTextPassword.getText()).trim();
            String name = String.valueOf(editTextName.getText()).trim();

            // Validation des champs
            if (TextUtils.isEmpty(name)) {
                Toast.makeText(register.this, "Enter your name", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
                return;
            }
            if (TextUtils.isEmpty(email)) {
                Toast.makeText(register.this, "Enter email", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
                return;
            }
            if (TextUtils.isEmpty(password) || password.length() < 6) {
                Toast.makeText(register.this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
                return;
            }

            // Création d'un utilisateur avec Firebase Auth
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        progressBar.setVisibility(View.GONE);

                        if (task.isSuccessful()) {
                            // L'utilisateur est créé avec succès
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                // Mise à jour du profil de l'utilisateur (nom)
                                user.updateProfile(new UserProfileChangeRequest.Builder()
                                                .setDisplayName(name) // Mise à jour du nom
                                                .build())
                                        .addOnCompleteListener(profileTask -> {
                                            if (profileTask.isSuccessful()) {
                                                Log.d("Register", "User profile updated.");
                                            }
                                        });

                                // Récupération de l'UID
                                String uid = user.getUid();

                                // Création de l'objet utilisateur pour Firestore
                                User newUser = new User(name, uid, email, System.currentTimeMillis()); // Assurez-vous que name est bien défini ici

                                // Enregistrement dans Firestore
                                db.collection("users")
                                        .document(uid)
                                        .set(newUser)
                                        .addOnSuccessListener(aVoid -> {
                                            // Succès
                                            Toast.makeText(register.this, "Account created successfully.", Toast.LENGTH_SHORT).show();
                                            Intent intent = new Intent(getApplicationContext(), login.class);
                                            startActivity(intent);
                                            finish();
                                        })
                                        .addOnFailureListener(e -> {
                                            // Échec
                                            Toast.makeText(register.this, "Failed to save user in Firestore", Toast.LENGTH_SHORT).show();
                                            Log.e("Firestore", "Error adding user", e);
                                        });
                            }
                        } else {
                            // Échec de l'authentification
                            Toast.makeText(register.this, "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });

        });
    }

    // Classe interne représentant l'utilisateur pour Firestore
}
