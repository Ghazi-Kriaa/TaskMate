package com.example.taskmate;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class TaskDetailActivity extends AppCompatActivity {

    private TextView taskTitleTextView, taskDescriptionTextView, assignedUserTextView, taskStatusTextView;
    private FirebaseFirestore db;
    private String taskId;
    private String projectId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_detail);

        // Initialiser les vues
        taskTitleTextView = findViewById(R.id.taskTitleTextView);
        taskDescriptionTextView = findViewById(R.id.taskDescriptionTextView);
        assignedUserTextView = findViewById(R.id.assignedUserTextView);
        taskStatusTextView = findViewById(R.id.taskStatusTextView);
        ImageButton buttonUpdateTask = findViewById(R.id.buttonUpdateTask);
        ImageButton buttonDeleteTask = findViewById(R.id.buttonDeleteTask);

        taskId = getIntent().getStringExtra("TASK_ID");
        projectId = getIntent().getStringExtra("PROJECT_ID");

        // Charger les détails de la tâche
        fetchTaskDetails(taskId);

        // Logique pour supprimer la tâche
        buttonDeleteTask.setOnClickListener(v -> {
            if (taskId != null && !taskId.isEmpty()) {
                // Récupérer l'ID du projet auquel la tâche appartient
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                db.collection("tasks").document(taskId)
                        .get()
                        .addOnSuccessListener(taskDocument -> {
                            if (taskDocument.exists()) {
                                // Récupérer l'ID du projet de cette tâche
                                String projectId = taskDocument.getString("projectId");

                                // Récupérer les informations du projet
                                db.collection("projects").document(projectId)
                                        .get()
                                        .addOnSuccessListener(projectDocument -> {
                                            if (projectDocument.exists()) {
                                                String ownerId = projectDocument.getString("ownerId");
                                                String currentUserId = FirebaseAuth.getInstance().getUid();
                                                // Vérifier si l'utilisateur connecté est le propriétaire du projet
                                                if (currentUserId != null && currentUserId.equals(ownerId)) {
                                                    // Si l'utilisateur est le propriétaire, afficher l'alerte de suppression
                                                    new AlertDialog.Builder(this)
                                                            .setTitle("Confirm Deletion")
                                                            .setMessage("Are you sure you want to delete this task?")
                                                            .setPositiveButton("Yes", (dialog, which) -> deleteTask(taskId)) // Appel à la méthode de suppression
                                                            .setNegativeButton("No", null)
                                                            .show();
                                                } else {
                                                    // Si l'utilisateur n'est pas le propriétaire
                                                    Toast.makeText(this, "You do not have permission to delete this task", Toast.LENGTH_SHORT).show();
                                                }
                                            } else {
                                                Toast.makeText(this, "Project not found", Toast.LENGTH_SHORT).show();
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(this, "Error retrieving project details", Toast.LENGTH_SHORT).show();
                                            Log.e("ProjectDetailActivity", "Error: " + e.getMessage());
                                        });
                            } else {
                                Toast.makeText(this, "Task not found", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Error retrieving task details", Toast.LENGTH_SHORT).show();
                            Log.e("TaskDetailActivity", "Error: " + e.getMessage());
                        });
            } else {
                Toast.makeText(this, "Task ID is not valid", Toast.LENGTH_SHORT).show();
            }
        });

        // Logique pour mettre à jour la tâche
        buttonUpdateTask.setOnClickListener(v -> {
            if (taskId != null && !taskId.isEmpty()) {
                db = FirebaseFirestore.getInstance();

                // Récupérer la tâche pour vérifier l'utilisateur assigné
                db.collection("tasks").document(taskId)
                        .get()
                        .addOnSuccessListener(taskDocument -> {
                            if (taskDocument.exists()) {
                                String projectId = taskDocument.getString("projectId");
                                String assignedUserName = taskDocument.getString("assignedUser"); // Nom de l'utilisateur assigné

                                // Vérifier que le nom de l'utilisateur assigné n'est pas vide
                                if (assignedUserName != null && !assignedUserName.isEmpty()) {
                                    // Rechercher l'ID de l'utilisateur à partir du nom
                                    db.collection("users")
                                            .whereEqualTo("name", assignedUserName)
                                            .get()
                                            .addOnSuccessListener(userQuery -> {
                                                if (!userQuery.isEmpty()) {
                                                    // Obtenir l'ID Firebase de l'utilisateur assigné
                                                    String assignedUserId = userQuery.getDocuments().get(0).getString("id");

                                                    // Récupérer les informations du projet
                                                    db.collection("projects").document(projectId)
                                                            .get()
                                                            .addOnSuccessListener(projectDocument -> {
                                                                if (projectDocument.exists()) {
                                                                    String ownerId = projectDocument.getString("ownerId"); // ID du propriétaire
                                                                    String currentUserId = FirebaseAuth.getInstance().getUid(); // ID utilisateur connecté

                                                                    if (currentUserId != null) {
                                                                        Intent intent = new Intent(TaskDetailActivity.this, UpdateTaskActivity.class);
                                                                        intent.putExtra("TASK_ID", taskId);
                                                                        intent.putExtra("PROJECT_ID", projectId);

                                                                        if (currentUserId.equals(ownerId)) {
                                                                            // Le propriétaire peut modifier tous les champs
                                                                            startActivity(intent);
                                                                        } else if (currentUserId.equals(assignedUserId)) {
                                                                            // L'utilisateur assigné peut modifier la description et le statut
                                                                            intent.putExtra("CAN_UPDATE_DESCRIPTION_STATUS", true);
                                                                            startActivity(intent);
                                                                        } else {
                                                                            Toast.makeText(this, "You do not have permission to update this task", Toast.LENGTH_SHORT).show();
                                                                        }
                                                                    }
                                                                } else {
                                                                    Toast.makeText(this, "Project not found", Toast.LENGTH_SHORT).show();
                                                                }
                                                            })
                                                            .addOnFailureListener(e -> {
                                                                Toast.makeText(this, "Error retrieving project details", Toast.LENGTH_SHORT).show();
                                                            });
                                                } else {
                                                    Toast.makeText(this, "Assigned user not found", Toast.LENGTH_SHORT).show();
                                                }
                                            })
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(this, "Error retrieving assigned user details", Toast.LENGTH_SHORT).show();
                                            });
                                } else {
                                    Toast.makeText(this, "Assigned user name is empty", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(this, "Task not found", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Error retrieving task details", Toast.LENGTH_SHORT).show();
                        });
            } else {
                Toast.makeText(this, "Task ID is not valid", Toast.LENGTH_SHORT).show();
            }
        });
    }
        @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.project_details_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        String projectId = getIntent().getStringExtra("PROJECT_ID");
        if (itemId == R.id.action_view_tasks) {
            Intent intent = new Intent(TaskDetailActivity.this, TaskListActivity.class);
            intent.putExtra("PROJECT_ID", projectId);
            startActivity(intent);
            return true;
        }else if (itemId == R.id.action_add_tasks) {
            // Récupérer l'ID de l'utilisateur connecté
            String currentUserId = FirebaseAuth.getInstance().getUid();

            // Récupérer le document du projet
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("projects").document(projectId)
                    .get()
                    .addOnSuccessListener(projectDocument -> {
                        if (projectDocument.exists()) {
                            // Récupérer l'ID du propriétaire du projet
                            String ownerId = projectDocument.getString("ownerId");

                            // Vérifier si l'utilisateur connecté est le propriétaire du projet
                            if (currentUserId != null && currentUserId.equals(ownerId)) {
                                // Si l'utilisateur est le propriétaire, autoriser la création de la tâche
                                Intent intent = new Intent(TaskDetailActivity.this, CreateTaskActivity.class);
                                intent.putExtra("PROJECT_ID", projectId);
                                startActivity(intent);
                            } else {
                                // Si l'utilisateur n'est pas le propriétaire, afficher un message
                                Toast.makeText(TaskDetailActivity.this, "You do not have permission to create a task", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(TaskDetailActivity.this, "Project not found", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(TaskDetailActivity.this, "Error retrieving project details", Toast.LENGTH_SHORT).show();
                    });

            return true;
        } else if (itemId == R.id.action_view_Projects) {
            Intent intent = new Intent(TaskDetailActivity.this, ProjectListActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void fetchTaskDetails(String taskId) {
        db = FirebaseFirestore.getInstance();
        db.collection("tasks")
                .document(taskId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Task task = documentSnapshot.toObject(Task.class);
                        if (task != null) {
                            taskTitleTextView.setText("Task Title: " + task.getTaskTitle());
                            taskDescriptionTextView.setText("Description: " + task.getDescription());
                            taskStatusTextView.setText("Status: " + task.getStatus());
                            String assignedUser = task.getAssignedUser();
                            assignedUserTextView.setText("Collaborator: " + (assignedUser != null && !assignedUser.isEmpty() ? assignedUser : "No user assigned"));
                        }
                    } else {
                        Toast.makeText(this, "Task not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load task details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteTask(String taskId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Étape 1: Supprimer la tâche du projet
        db.collection("tasks").document(taskId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Étape 2: Recharger la liste des tâches
                    Toast.makeText(TaskDetailActivity.this, "Task deleted successfully", Toast.LENGTH_SHORT).show();
                    reloadTaskList(); // Recharger la liste des tâches
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(TaskDetailActivity.this, "Error deleting task", Toast.LENGTH_SHORT).show();
                    Log.e("TaskDetailActivity", "Error deleting task: " + e.getMessage());
                });
    }

    private void reloadTaskList() {
        // Créer une nouvelle intention pour afficher la liste des tâches
        Intent intent = new Intent(TaskDetailActivity.this, TaskListActivity.class);

        // Démarrer l'activité TaskListActivity qui affiche la liste des tâches
        startActivity(intent);

        // Fermer l'activité actuelle pour revenir à la liste des tâches
        finish();
    }




}
