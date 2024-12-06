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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ProjectDetailActivity extends AppCompatActivity {
    private TextView titleTextView, descriptionTextView, createdAtTextView, ownerIdTextView, membersTextView;
    private ImageButton buttonUpdateProject, buttonDeleteProject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_detail);

        // Lier les vues
        titleTextView = findViewById(R.id.projectDetailTitle);
        descriptionTextView = findViewById(R.id.projectDetailDescription);
        createdAtTextView = findViewById(R.id.projectDetailCreatedAt);
        ownerIdTextView = findViewById(R.id.projectDetailOwnerId);
        membersTextView = findViewById(R.id.projectDetailMembers);
        buttonUpdateProject = findViewById(R.id.buttonUpdateProject);
        buttonDeleteProject = findViewById(R.id.buttonDeleteProject);

        // Récupérer l'ID du projet depuis l'intent
        String projectId = getIntent().getStringExtra("projectId");
        String projectTitle = getIntent().getStringExtra("projectTitle"); // Utiliser un autre paramètre si le titre est passé
        Log.d("ProjectDetailActivity", "ID du projet récupéré : " + projectId);
        Log.d("ProjectDetailActivity", "Titre du projet récupéré : " + projectTitle);

        // Action pour modifier le projet
        buttonUpdateProject.setOnClickListener(v -> {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("projects").document(projectId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String ownerId = documentSnapshot.getString("ownerId");
                            String currentUserId = FirebaseAuth.getInstance().getUid();
                            // Vérifier si l'utilisateur connecté est le propriétaire du projet
                            if (currentUserId.equals(ownerId)) {
                                // Si l'utilisateur est le propriétaire, ouvrir la page de mise à jour
                                Intent intent = new Intent(ProjectDetailActivity.this, UpdateProjectActivity.class);
                                intent.putExtra("PROJECT_ID", projectId); // Passez l'ID du projet
                                startActivity(intent);
                            } else {
                                // Si l'utilisateur n'est pas le propriétaire, afficher un message
                                Toast.makeText(ProjectDetailActivity.this, "You do not have permission to update this project", Toast.LENGTH_SHORT).show();
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(ProjectDetailActivity.this, "Error retrieving project details", Toast.LENGTH_SHORT).show();
                    });
        });

        // Action pour supprimer le projet
        buttonDeleteProject.setOnClickListener(v -> {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("projects").document(projectId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String ownerId = documentSnapshot.getString("ownerId");
                            String currentUserId = FirebaseAuth.getInstance().getUid();
                            // Vérifier si l'utilisateur connecté est le propriétaire du projet
                            if (currentUserId.equals(ownerId)) {
                                // Si l'utilisateur est le propriétaire, afficher l'alerte pour confirmer la suppression
                                new AlertDialog.Builder(this)
                                        .setTitle("Confirm Deletion")
                                        .setMessage("Are you sure you want to delete this project?")
                                        .setPositiveButton("Yes", (dialog, which) -> deleteProject(projectId)) // Appel à la méthode de suppression
                                        .setNegativeButton("No", null)
                                        .show();
                            } else {
                                // Si l'utilisateur n'est pas le propriétaire, afficher un message
                                Toast.makeText(this, "You do not have permission to delete this project", Toast.LENGTH_SHORT).show();
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error retrieving project details", Toast.LENGTH_SHORT).show();
                    });
        });

        if (projectId != null) {
            fetchProjectById(projectId);
        } else if (projectTitle != null) {
            fetchProjectByTitle(projectTitle);
        } else {
            Toast.makeText(this, "ID or Project Title is missing", Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.project_details_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        String projectId = getIntent().getStringExtra("projectId");
        if (itemId == R.id.action_view_tasks) {
            Intent intent = new Intent(ProjectDetailActivity.this, TaskListActivity.class);
            intent.putExtra("PROJECT_ID", projectId);
            startActivity(intent);
            return true;
        } else if (itemId == R.id.action_add_tasks) {
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
                                Intent intent = new Intent(ProjectDetailActivity.this, CreateTaskActivity.class);
                                intent.putExtra("PROJECT_ID", projectId);
                                startActivity(intent);
                            } else {
                                // Si l'utilisateur n'est pas le propriétaire, afficher un message
                                Toast.makeText(ProjectDetailActivity.this, "You do not have permission to create a task", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(ProjectDetailActivity.this, "Project not found", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(ProjectDetailActivity.this, "Error retrieving project details", Toast.LENGTH_SHORT).show();
                    });

            return true;
        }else if (itemId == R.id.action_view_Projects) {
            Intent intent = new Intent(ProjectDetailActivity.this, ProjectListActivity.class);
            intent.putExtra("PROJECT_ID", projectId);
            startActivity(intent);
            return true;}
        return super.onOptionsItemSelected(item);
    }
    private void fetchOwnerName(String ownerId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(ownerId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String ownerName = documentSnapshot.getString("name");
                        if (ownerName != null && !ownerName.isEmpty()) {
                            ownerIdTextView.setText("Owner Name: " + ownerName);
                        } else {
                            ownerIdTextView.setText("Owner name not available");
                        }
                    } else {
                        ownerIdTextView.setText("Owner not found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ProjectDetailActivity", "Error fetching owner name: " + e.getMessage());
                    ownerIdTextView.setText("Error fetching owner name");
                });
    }

    private void fetchProjectById(String projectId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("projects").document(projectId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Project project = documentSnapshot.toObject(Project.class);
                        if (project != null) {
                            updateUIWithProjectDetails(project);
                        } else {
                            Toast.makeText(this, "The project is empty", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Project not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading project details", Toast.LENGTH_SHORT).show();
                    Log.e("ProjectDetailActivity", "Error: " + e.getMessage());
                });
    }

    private void fetchProjectByTitle(String projectTitle) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("projects")
                .whereEqualTo("title", projectTitle)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            Project project = document.toObject(Project.class);
                            if (project != null) {
                                updateUIWithProjectDetails(project);
                                break;
                            }
                        }
                    } else {
                        Toast.makeText(this, "No project found with this title", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading projects", Toast.LENGTH_SHORT).show();
                    Log.e("ProjectDetailActivity", "Error: " + e.getMessage());
                });
    }

    private void updateUIWithProjectDetails(Project project) {
        titleTextView.setText("Title: " + project.getTitle());
        descriptionTextView.setText(project.getDescription() != null && !project.getDescription().isEmpty() ? "Description: " + project.getDescription() : "Description not available");

        createdAtTextView.setText(project.getCreatedAt() != null ? "CreatedAt: " + project.getCreatedAt().toDate().toString() : "Date not available");

        String ownerId = project.getOwnerId();
        if (ownerId != null && !ownerId.isEmpty()) {
            fetchOwnerName(ownerId);
        } else {
            ownerIdTextView.setText("Owner not available");
        }

        List<String> memberIds = project.getMembers();
        if (memberIds != null && !memberIds.isEmpty()) {
            fetchMembersNames(memberIds);
        } else {
            membersTextView.setText("Members: None");
        }
    }

    private void fetchMembersNames(List<String> memberIds) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        StringBuilder membersNames = new StringBuilder();
        final List<String> finalMembersNames = new ArrayList<>();

        for (String memberId : memberIds) {
            db.collection("users").document(memberId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("name");
                            finalMembersNames.add(name != null && !name.isEmpty() ? name : "Name not available");
                        } else {
                            finalMembersNames.add(memberId);
                        }

                        if (finalMembersNames.size() == memberIds.size()) {
                            for (int j = 0; j < finalMembersNames.size(); j++) {
                                membersNames.append(finalMembersNames.get(j));
                                if (j < finalMembersNames.size() - 1) {
                                    membersNames.append(", ");
                                }
                            }
                            membersTextView.setText("Members: " + membersNames.toString());
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("ProjectDetailActivity", "Error fetching member names: " + e.getMessage());
                    });
        }
    }

    private void deleteProject(String projectId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Étape 1: Supprimer les tâches associées au projet
        db.collection("tasks")
                .whereEqualTo("projectId", projectId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Supprimer chaque tâche du projet
                        for (DocumentSnapshot taskDocument : queryDocumentSnapshots) {
                            String taskId = taskDocument.getId();
                            deleteTask(taskId, db); // Supprimer chaque tâche individuellement
                        }
                    }

                    // Étape 2: Supprimer le projet
                    db.collection("projects").document(projectId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(ProjectDetailActivity.this, "Project deleted successfully", Toast.LENGTH_SHORT).show();
                                reloadProjectList(); // Recharger la liste des projets
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(ProjectDetailActivity.this, "Error deleting project", Toast.LENGTH_SHORT).show();
                                Log.e("ProjectDetailActivity", "Error deleting project: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ProjectDetailActivity.this, "Error deleting tasks", Toast.LENGTH_SHORT).show();
                    Log.e("ProjectDetailActivity", "Error deleting tasks: " + e.getMessage());
                });
    }

    private void deleteTask(String taskId, FirebaseFirestore db) {
        db.collection("tasks").document(taskId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Vous pouvez afficher un message pour chaque tâche supprimée, si nécessaire
                    Log.d("TaskDetail", "Task " + taskId + " deleted successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e("TaskDetail", "Error deleting task " + taskId + ": " + e.getMessage());
                });
    }


    private void reloadProjectList() {
        // Créer une nouvelle intention pour afficher la liste des projets
        Intent intent = new Intent(ProjectDetailActivity.this, ProjectListActivity.class);
        startActivity(intent); // Démarrer l'activité ProjectListActivity qui affiche la liste des projets
        finish(); // Fermer l'activité actuelle pour revenir à la liste des projets
    }

}
