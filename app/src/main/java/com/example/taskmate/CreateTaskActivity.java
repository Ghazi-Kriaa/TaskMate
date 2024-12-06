package com.example.taskmate;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.List;

public class CreateTaskActivity extends AppCompatActivity {

    private EditText editTextTaskTitle, editTextTaskDescription;
    private Button buttonCreateTask, buttonAssignUser;
    private FirebaseFirestore db;
    private String projectId; // ID du projet à associer
    private String assignedUserId = null; // ID de l'utilisateur assigné
    private List<User> usersList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_task);

        // Initialisation des vues
        editTextTaskTitle = findViewById(R.id.editTextTaskTitle);
        editTextTaskDescription = findViewById(R.id.editTextTaskDescription);
        buttonCreateTask = findViewById(R.id.buttonCreateTask);
        buttonAssignUser = findViewById(R.id.buttonAssignUser);

        db = FirebaseFirestore.getInstance();
        projectId = getIntent().getStringExtra("PROJECT_ID"); // Récupère l'ID du projet

        // Charger les utilisateurs du projet


        // Bouton pour assigner un utilisateur
        buttonAssignUser.setOnClickListener(v -> showUserSelectionDialog());

        // Bouton pour créer la tâche
        buttonCreateTask.setOnClickListener(v -> createTask());
    }



    private void showUserSelectionDialog() {
        // Charger les membres du projet depuis Firestore
        db.collection("projects")
                .document(projectId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Récupérer la liste des membres (IDs)
                        List<String> projectMembers = (List<String>) documentSnapshot.get("members");

                        if (projectMembers != null && !projectMembers.isEmpty()) {
                            // Charger les utilisateurs depuis Firestore
                            db.collection("users")
                                    .get()
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            QuerySnapshot querySnapshot = task.getResult();
                                            List<User> filteredUserList = new ArrayList<>();
                                            List<String> userNames = new ArrayList<>();

                                            // Filtrer les utilisateurs par rapport à la liste des membres
                                            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                                                String userId = document.getId();
                                                String userName = document.getString("name");

                                                if (userId != null && projectMembers.contains(userId)||projectMembers.contains(userName)) {
                                                    filteredUserList.add(new User(userId, userName));
                                                    userNames.add(userName);
                                                }
                                            }

                                            // Vérifier si des utilisateurs sont disponibles
                                            if (filteredUserList.isEmpty()) {
                                                Toast.makeText(this, "No users available for this project", Toast.LENGTH_SHORT).show();
                                                return;
                                            }

                                            // Convertir la liste des utilisateurs en un tableau de noms
                                            CharSequence[] items = userNames.toArray(new CharSequence[0]);

                                            // Créer un tableau pour l'utilisateur sélectionné
                                            final int[] selectedUserIndex = {-1}; // Pour garder une trace de l'index sélectionné

                                            // Afficher un AlertDialog avec les utilisateurs filtrés
                                            AlertDialog.Builder builder = new AlertDialog.Builder(this);
                                            builder.setTitle("Select User")
                                                    .setSingleChoiceItems(items, -1, (dialog, which) -> {
                                                        // Lors de la sélection, mettre à jour l'index de l'utilisateur
                                                        selectedUserIndex[0] = which;
                                                    })
                                                    .setPositiveButton("OK", (dialog, which) -> {
                                                        if (selectedUserIndex[0] != -1) {
                                                            // Mettre à jour assignedUserId avec l'utilisateur sélectionné
                                                            assignedUserId = filteredUserList.get(selectedUserIndex[0]).getId();
                                                            Toast.makeText(this, "User selected: " + items[selectedUserIndex[0]], Toast.LENGTH_SHORT).show();
                                                        } else {
                                                            Toast.makeText(this, "Please select a user", Toast.LENGTH_SHORT).show();
                                                        }
                                                    })
                                                    .setNegativeButton("Cancel", (dialog, which) -> {
                                                        // Annuler la sélection
                                                    })
                                                    .show();
                                        } else {
                                            Toast.makeText(this, "Failed to load users", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            Toast.makeText(this, "No members found for this project", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Project not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load project members", Toast.LENGTH_SHORT).show();
                    Log.e("CreateTaskActivity", "Error loading project members", e);
                });
    }





    private void createTask() {
        String taskTitle = editTextTaskTitle.getText().toString().trim();
        String taskDescription = editTextTaskDescription.getText().toString().trim();

        if (taskTitle.isEmpty() || taskDescription.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (assignedUserId == null) {
            Toast.makeText(this, "Please assign a user to the task", Toast.LENGTH_SHORT).show();
            return;
        }

        // Créer un ID unique pour la tâche
        String taskId = db.collection("tasks").document().getId();

        // Créer l'objet Task
        Task task = new Task(taskId, taskTitle, taskDescription, assignedUserId, projectId, "To Do");

        // Enregistrer la tâche dans Firestore
        db.collection("tasks").document(taskId)
                .set(task)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Task created successfully", Toast.LENGTH_SHORT).show();

                    // Redirection vers TaskListActivity
                    Intent intent = new Intent(CreateTaskActivity.this, TaskListActivity.class);
                    intent.putExtra("PROJECT_ID", projectId); // Passer l'ID du projet
                    startActivity(intent);

                    finish(); // Fermer l'activité actuelle
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to create task", Toast.LENGTH_SHORT).show();
                    Log.e("CreateTaskActivity", "Error creating task", e);
                });
    }


}
