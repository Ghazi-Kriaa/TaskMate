package com.example.taskmate;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class TaskListActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_TASK_DETAIL = 1001;
    private RecyclerView taskRecyclerView;
    private TaskAdapter taskAdapter;
    private FirebaseFirestore db;
    private List<Task> taskList = new ArrayList<>();
    private String projectId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_list);

        // Initialisation des vues et variables
        taskRecyclerView = findViewById(R.id.taskRecyclerView);
        taskRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        taskAdapter = new TaskAdapter(this, taskList);
        taskRecyclerView.setAdapter(taskAdapter);

        db = FirebaseFirestore.getInstance();
        projectId = getIntent().getStringExtra("PROJECT_ID");

        // Vérification de la validité de l'ID du projet
        if (projectId == null || projectId.isEmpty()) {
            Toast.makeText(this, "Invalid Project ID", Toast.LENGTH_SHORT).show();
            finish();
            return; // Quitte immédiatement si l'ID est invalide
        }

        // Charger les tâches associées au projet
        fetchTasksForProject(projectId);

        // Gestion du clic sur une tâche dans la liste
        taskAdapter.setOnTaskClickListener(task -> {
            // Action lors du clic sur une tâche
            Intent intent = new Intent(TaskListActivity.this, TaskDetailActivity.class);
            intent.putExtra("TASK_ID", task.getId());
            intent.putExtra("PROJECT_ID", projectId); // Passer l'ID du projet
            startActivityForResult(intent, REQUEST_CODE_TASK_DETAIL);
        });
    }

    /**
     * Méthode pour charger les tâches d'un projet spécifique.
     *
     * @param projectId L'ID du projet dont on charge les tâches.
     */
    private void fetchTasksForProject(String projectId) {
        db.collection("tasks")
                .whereEqualTo("projectId", projectId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    taskList.clear(); // Effacer la liste avant de la remplir
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Task task = doc.toObject(Task.class);
                        task.setId(doc.getId()); // Définir l'ID de la tâche
                        taskList.add(task);
                    }
                    taskAdapter.notifyDataSetChanged(); // Notifier l'adaptateur pour rafraîchir la liste
                })
                .addOnFailureListener(e -> {
                    Log.e("TaskListActivity", "Error fetching tasks", e);
                    Toast.makeText(this, "Error fetching tasks", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Rafraîchir les tâches chaque fois que l'activité est reprise.
     */
    @Override
    protected void onResume() {
        super.onResume();
        fetchTasksForProject(projectId); // Recharger les tâches pour afficher les données actualisées
    }

    /**
     * Gérer les résultats retournés par des activités lancées via startActivityForResult.
     *
     * @param requestCode Le code de la requête utilisée pour démarrer l'activité.
     * @param resultCode  Le code de résultat retourné par l'activité.
     * @param data        Les données retournées par l'activité.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_TASK_DETAIL && resultCode == RESULT_OK) {
            boolean taskDeleted = data != null && data.getBooleanExtra("task_deleted", false);
            if (taskDeleted) {
                fetchTasksForProject(projectId); // Rafraîchir la liste après suppression
            }
        }
    }
}
