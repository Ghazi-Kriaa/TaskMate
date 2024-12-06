package com.example.taskmate;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class UpdateTaskActivity extends AppCompatActivity {

    private EditText editTextTaskTitle, editTextDescription, editTextAssignedUser;
    private Spinner spinnerStatus;
    private Button buttonUpdateTask;
    private String taskId;
    private String projectId;
    private String assignedUserId;
    private FirebaseFirestore db;
    private boolean canUpdateDescriptionStatus = false; // Flag pour permissions restreintes

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_task);

        // Initialiser les vues
        editTextTaskTitle = findViewById(R.id.editTextTaskTitle);
        editTextDescription = findViewById(R.id.editTextDescription);
        editTextAssignedUser = findViewById(R.id.editTextAssignedUser);
        spinnerStatus = findViewById(R.id.spinnerStatus);
        buttonUpdateTask = findViewById(R.id.buttonUpdateTask);

        db = FirebaseFirestore.getInstance();

        // Récupérer les informations passées dans l'Intent
        taskId = getIntent().getStringExtra("TASK_ID");
        projectId = getIntent().getStringExtra("PROJECT_ID");
        canUpdateDescriptionStatus = getIntent().getBooleanExtra("CAN_UPDATE_DESCRIPTION_STATUS", false);

        // Appliquer les restrictions basées sur les permissions
        applyFieldRestrictions();

        // Charger les détails de la tâche
        loadTaskDetails(taskId);

        // Ajouter un écouteur sur le champ AssignedUser
        editTextAssignedUser.setOnClickListener(v -> showUserSelectionDialog());

        // Écouter le bouton de mise à jour
        buttonUpdateTask.setOnClickListener(v -> {
            String updatedTitle = editTextTaskTitle.getText().toString();
            String updatedDescription = editTextDescription.getText().toString();
            String updatedAssignedUser = assignedUserId; // Utiliser l'ID sélectionné
            String updatedStatus = spinnerStatus.getSelectedItem().toString();

            if (canUpdateDescriptionStatus) {
                // L'utilisateur assigné ne peut mettre à jour que la description et le statut
                updateTask(null, updatedDescription, null, updatedStatus);
            } else {
                // Le propriétaire peut mettre à jour tous les champs
                updateTask(updatedTitle, updatedDescription, updatedAssignedUser, updatedStatus);
            }
        });
    }

    private void applyFieldRestrictions() {
        if (canUpdateDescriptionStatus) {
            // Désactiver les champs que l'utilisateur assigné ne peut pas modifier
            editTextTaskTitle.setEnabled(false);
            editTextAssignedUser.setEnabled(false);
        }
    }

    private void loadTaskDetails(String taskId) {
        db.collection("tasks").document(taskId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Task task = documentSnapshot.toObject(Task.class);
                        editTextTaskTitle.setText(task.getTaskTitle());
                        editTextDescription.setText(task.getDescription());
                        editTextAssignedUser.setText(task.getAssignedUser());
                        assignedUserId = task.getAssignedUser(); // Charger l'utilisateur assigné actuel

                        // Définir le statut dans le spinner
                        String status = task.getStatus();
                        int statusPosition = getStatusPosition(status);
                        spinnerStatus.setSelection(statusPosition);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(UpdateTaskActivity.this, "Failed to load task", Toast.LENGTH_SHORT).show());
    }

    private void showUserSelectionDialog() {
        // Charger les membres du projet depuis Firestore
        db.collection("projects")
                .document(projectId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
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

                                                if (userId != null && projectMembers.contains(userId)) {
                                                    filteredUserList.add(new User(userId, userName));
                                                    userNames.add(userName);
                                                }
                                            }

                                            // Afficher la boîte de dialogue
                                            if (!filteredUserList.isEmpty()) {
                                                CharSequence[] items = userNames.toArray(new CharSequence[0]);
                                                final int[] selectedUserIndex = {-1};

                                                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                                                builder.setTitle("Select User")
                                                        .setSingleChoiceItems(items, -1, (dialog, which) -> selectedUserIndex[0] = which)
                                                        .setPositiveButton("OK", (dialog, which) -> {
                                                            if (selectedUserIndex[0] != -1) {
                                                                assignedUserId = filteredUserList.get(selectedUserIndex[0]).getId();
                                                                editTextAssignedUser.setText(userNames.get(selectedUserIndex[0]));
                                                            } else {
                                                                Toast.makeText(this, "Please select a user", Toast.LENGTH_SHORT).show();
                                                            }
                                                        })
                                                        .setNegativeButton("Cancel", null)
                                                        .show();
                                            } else {
                                                Toast.makeText(this, "No users available for this project", Toast.LENGTH_SHORT).show();
                                            }
                                        } else {
                                            Toast.makeText(this, "Failed to load users", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            Toast.makeText(this, "No members found for this project", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("UpdateTaskActivity", "Error loading members", e));
    }

    private int getStatusPosition(String status) {
        String[] statusOptions = getResources().getStringArray(R.array.task_status_options);
        for (int i = 0; i < statusOptions.length; i++) {
            if (statusOptions[i].equals(status)) {
                return i;
            }
        }
        return 0;
    }

    private void updateTask(String updatedTitle, String updatedDescription, String updatedAssignedUser, String updatedStatus) {
        if (updatedDescription.isEmpty() || updatedStatus.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Préserver les champs non modifiés
        db.collection("tasks").document(taskId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                // Récupérer les valeurs actuelles
                String currentTaskId = documentSnapshot.getId(); // Toujours récupérer l'ID actuel
                String currentTaskTitle = documentSnapshot.getString("taskTitle");
                String currentAssignedUser = documentSnapshot.getString("assignedUser");
                String currentProjectId = documentSnapshot.getString("projectId");

                // Conserver les valeurs actuelles si elles ne sont pas modifiées
                String finalTaskId = (taskId != null && !taskId.isEmpty()) ? taskId : currentTaskId;
                String finalTaskTitle = (updatedTitle != null && !updatedTitle.isEmpty()) ? updatedTitle : currentTaskTitle;
                String finalAssignedUser = (updatedAssignedUser != null) ? updatedAssignedUser : currentAssignedUser;
                String finalProjectId = (projectId != null && !projectId.isEmpty()) ? projectId : currentProjectId;

                // Préparer les données à mettre à jour
                Task updatedTask = new Task(
                        finalTaskId,
                        finalTaskTitle,
                        updatedDescription,
                        finalAssignedUser,
                        finalProjectId,
                        updatedStatus
                );

                // Mise à jour dans Firestore
                db.collection("tasks").document(finalTaskId).set(updatedTask)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(UpdateTaskActivity.this, "Task updated successfully", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(UpdateTaskActivity.this, TaskDetailActivity.class);
                            intent.putExtra("TASK_ID", finalTaskId);
                            intent.putExtra("PROJECT_ID", finalProjectId);
                            startActivity(intent);
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(UpdateTaskActivity.this, "Error updating task", Toast.LENGTH_SHORT).show();
                            Log.e("UpdateTaskActivity", "Error updating task: " + e.getMessage());
                        });
            } else {
                Toast.makeText(this, "Task not found", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(UpdateTaskActivity.this, "Failed to load task details for update", Toast.LENGTH_SHORT).show();
            Log.e("UpdateTaskActivity", "Error retrieving task: " + e.getMessage());
        });
    }



}
