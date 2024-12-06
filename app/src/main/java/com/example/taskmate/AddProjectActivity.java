package com.example.taskmate;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AddProjectActivity extends AppCompatActivity {

    private EditText editTextTitle, editTextDescription;
    private Button buttonSubmitProject, buttonSelectUsers;
    private List<User> usersList;
    private FirebaseFirestore db;
    private String currentUserId;

    // Liste pour stocker uniquement les ids des utilisateurs sélectionnés
    private List<String> selectedUserIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_project);

        // Initialisation des vues
        editTextTitle = findViewById(R.id.editTextTitle);
        editTextDescription = findViewById(R.id.editTextDescription);
        buttonSubmitProject = findViewById(R.id.buttonSubmitProject);
        buttonSelectUsers = findViewById(R.id.buttonSelectUsers);

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Initialisation de la liste d'utilisateurs
        usersList = new ArrayList<>();

        // Charger les utilisateurs
        loadUsers();

        // Bouton pour afficher la boîte de dialogue de sélection d'utilisateurs
        buttonSelectUsers.setOnClickListener(v -> showUserSelectionDialog());

        // Bouton pour soumettre le projet
        buttonSubmitProject.setOnClickListener(v -> addProject());
    }

    private void loadUsers() {
        db.collection("users") // Remplacez par le nom de votre collection d'utilisateurs
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null) {
                            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                                String userId = document.getString("id");
                                String userName = document.getString("name");

                                // Exclure l'utilisateur connecté
                                if (userId != null && !userId.equals(currentUserId)) {
                                    usersList.add(new User(userId, userName));
                                }
                            }
                        }
                    } else {
                        Log.e("AddProjectActivity", "Failed to load users", task.getException());
                    }
                });
    }

    private void showUserSelectionDialog() {
        db.collection("users")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        List<User> userList = new ArrayList<>();
                        List<String> userNames = new ArrayList<>();

                        // Filtrer les utilisateurs pour exclure l'utilisateur connecté
                        for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                            String userId = document.getId();  // ID Firestore
                            String userName = document.getString("name");

                            if (userId != null && !userId.equals(currentUserId)) {
                                userList.add(new User(userId, userName)); // Liste des utilisateurs (IDs et noms)
                                userNames.add(userName); // Liste des noms pour l'affichage
                            }
                        }

                        // Convertir la liste des utilisateurs en un tableau de noms
                        CharSequence[] items = userNames.toArray(new CharSequence[0]);

                        // Créer un tableau pour les utilisateurs sélectionnés
                        boolean[] checkedItems = new boolean[items.length];

                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle("Select Users")
                                .setMultiChoiceItems(items, checkedItems, (dialog, which, isChecked) -> {
                                    // Action lors de la sélection
                                })
                                .setPositiveButton("OK", (dialog, which) -> {
                                    // Réinitialiser la liste des IDs sélectionnés
                                    selectedUserIds.clear();

                                    for (int i = 0; i < checkedItems.length; i++) {
                                        if (checkedItems[i]) {
                                            // Ajouter l'ID de l'utilisateur sélectionné (pas le nom)
                                            selectedUserIds.add(userList.get(i).getId());
                                        }
                                    }

                                    // Ajouter l'utilisateur connecté automatiquement
                                    selectedUserIds.add(currentUserId);

                                    Toast.makeText(this, "Users selected: " + selectedUserIds.size(), Toast.LENGTH_SHORT).show();
                                })
                                .setNegativeButton("Cancel", (dialog, which) -> {
                                    // Annuler la sélection
                                })
                                .show();
                    } else {
                        Toast.makeText(this, "Failed to load users", Toast.LENGTH_SHORT).show();
                    }
                });
    }



    private void addProject() {
        String projectTitle = editTextTitle.getText().toString().trim();
        String projectDescription = editTextDescription.getText().toString().trim();


        if (projectTitle.isEmpty() || projectDescription.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        // Vérification que des utilisateurs ont été sélectionnés
        if (selectedUserIds.isEmpty()) {
            Toast.makeText(this, "Please select at least one user", Toast.LENGTH_SHORT).show();
            return;
        }

        // Générer un ID unique pour le projet
        String projectId = db.collection("projects").document().getId();

        // Créer un objet Project avec les IDs des membres sélectionnés
        Project project = new Project(
                projectId,
                projectTitle,
                projectDescription,
                currentUserId,
                selectedUserIds,  // Seuls les IDs des utilisateurs
                Timestamp.now()
        );

        // Ajouter le projet à Firestore
        db.collection("projects").document(projectId).set(project)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Project updated successfully", Toast.LENGTH_SHORT).show();

                    // Passer l'ID du projet à la ProjectDetailActivity
                    Intent intent = new Intent(AddProjectActivity.this, ProjectDetailActivity.class);
                    intent.putExtra("projectId", projectId); // Passez l'ID du projet
                    startActivity(intent); // Démarrer la nouvelle activité
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update project: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("UpdateProjectActivity", "Error updating project", e);
                });
    }
}