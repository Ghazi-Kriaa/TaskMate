package com.example.taskmate;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class UpdateProjectActivity extends AppCompatActivity {
    private EditText editTextTitle, editTextDescription;
    private TextInputEditText editTextMembers;
    private Button buttonUpdateProject;
    private String projectId;
    private FirebaseFirestore db;

    private List<DocumentSnapshot> allUsers; // Liste des utilisateurs sous forme de DocumentSnapshot
    private List<String> selectedMembersIds; // Liste des IDs des membres sélectionnés
    private String ownerId;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_project);

        // Initialiser les vues
        editTextTitle = findViewById(R.id.editTextTitle);
        editTextDescription = findViewById(R.id.editTextDescription);
        editTextMembers = findViewById(R.id.editTextMembers);
        buttonUpdateProject = findViewById(R.id.buttonUpdateProject);

        db = FirebaseFirestore.getInstance();
        allUsers = new ArrayList<>();
        selectedMembersIds = new ArrayList<>();

        // Récupérer l'ID du projet
        projectId = getIntent().getStringExtra("PROJECT_ID");

        // Charger les détails existants du projet
        loadProjectDetails(projectId);

        // Charger tous les utilisateurs
        loadAllUsers();

        // Ouvrir la boîte de dialogue pour sélectionner les membres
        editTextMembers.setOnClickListener(v -> showMembersSelectionDialog());

        // Écouter le bouton de mise à jour
        buttonUpdateProject.setOnClickListener(v -> {
            String updatedTitle = editTextTitle.getText().toString();
            String updatedDescription = editTextDescription.getText().toString();
            updateProject(updatedTitle, updatedDescription, selectedMembersIds);
        });
    }

    private void loadProjectDetails(String projectId) {
        db.collection("projects").document(projectId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Project project = documentSnapshot.toObject(Project.class);
                        editTextTitle.setText(project.getTitle());
                        editTextDescription.setText(project.getDescription());
                        ownerId = project.getOwnerId();
                        // Récupérer les IDs des membres du projet existant
                        selectedMembersIds.addAll(project.getMembers()); // Assume project.getMembers() contient les IDs

                        // Charger tous les utilisateurs et afficher les membres du projet
                        loadUserNames(selectedMembersIds); // Charger les noms des membres
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(UpdateProjectActivity.this, "Failed to load project", Toast.LENGTH_SHORT).show();
                });
    }

    private void showMembersSelectionDialog() {
        boolean[] checkedItems = new boolean[allUsers.size()];
        String[] usersArray = new String[allUsers.size()];

        // Initialiser les cases déjà sélectionnées
        for (int i = 0; i < allUsers.size(); i++) {
            usersArray[i] = allUsers.get(i).getString("name"); // Utiliser les noms des utilisateurs
            if (selectedMembersIds.contains(allUsers.get(i).getString("id"))||selectedMembersIds.contains(allUsers.get(i).getString("name"))) {
                checkedItems[i] = true; // Marquer comme sélectionné si l'ID est dans selectedMembersIds
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Members");
        builder.setMultiChoiceItems(usersArray, checkedItems, (dialog, which, isChecked) -> {
            String selectedUserId = allUsers.get(which).getString("id");
            String selectdUserName=allUsers.get(which).getString("name");
            if (isChecked) {
                // Ajouter l'ID si il n'est pas déjà dans la liste
                if (!selectedMembersIds.contains(selectedUserId)) {
                    selectedMembersIds.add(selectedUserId); // Ajouter l'ID
                }
            } else {
                selectedMembersIds.remove(selectedUserId);
                selectedMembersIds.remove(selectdUserName);
                // Supprimer l'ID si décoché
            }
        });
        builder.setPositiveButton("OK", (dialog, which) -> loadUserNames(selectedMembersIds));
        builder.setNegativeButton("Cancel", null);
        builder.create().show();
    }

    private void loadUserNames(List<String> memberIds) {
        db.collection("users").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> memberNames = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        String userId = document.getString("id"); // Récupère l'ID de l'utilisateur
                        String userName = document.getString("name"); // Récupère le nom de l'utilisateur

                        // Si l'utilisateur fait partie des membres du projet, ajoute son nom
                        if (memberIds.contains(userId)) {
                            memberNames.add(userName);
                        }else if(memberIds.contains(userName)){
                            memberNames.add(userName);
                        }
                    }
                    // Afficher les noms des membres dans editTextMembers
                    editTextMembers.setText(String.join(", ", memberNames));
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(UpdateProjectActivity.this, "Failed to load user names", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadAllUsers() {
        db.collection("users").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allUsers.clear(); // Effacer les anciens utilisateurs
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        allUsers.add(document); // Ajouter le DocumentSnapshot
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(UpdateProjectActivity.this, "Failed to load users", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateProject(String updatedTitle, String updatedDescription, List<String> updatedMembersIds) {
        if (updatedTitle.isEmpty() || updatedDescription.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Créer un objet Project avec les nouvelles valeurs
        Project updatedProject = new Project(
                projectId,  // Utiliser l'ID du projet existant
                updatedTitle,
                updatedDescription,
                ownerId,  // Garder l'ownerId intact
                updatedMembersIds,  // Liste mise à jour des IDs des membres
                Timestamp.now() // Vous pouvez mettre à jour la date si nécessaire
        );

        // Mettre à jour le projet dans Firestore
        db.collection("projects").document(projectId).set(updatedProject)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Project updated successfully", Toast.LENGTH_SHORT).show();

                    // Passer l'ID du projet à la ProjectDetailActivity
                    Intent intent = new Intent(UpdateProjectActivity.this, ProjectDetailActivity.class);
                    intent.putExtra("projectId", projectId); // Passez l'ID du projet
                    startActivity(intent); // Démarrer la nouvelle activité
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update project: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("UpdateProjectActivity", "Error updating project", e);
                });
    }


}
