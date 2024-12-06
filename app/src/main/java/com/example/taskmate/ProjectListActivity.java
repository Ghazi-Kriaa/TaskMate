package com.example.taskmate;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class ProjectListActivity extends AppCompatActivity {
    private RecyclerView projectRecyclerView;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ArrayList<Project> projects = new ArrayList<>();
    private ProjectAdapter projectAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_list);

        projectRecyclerView = findViewById(R.id.projectRecyclerView);
        projectRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        String userId = auth.getCurrentUser().getUid();
        String userName = auth.getCurrentUser().getDisplayName();  // Nom de l'utilisateur

        // Initialiser l'adaptateur avant de charger les données
        projectAdapter = new ProjectAdapter(projects);
        projectRecyclerView.setAdapter(projectAdapter);

        // Charger les projets associés à l'utilisateur par ID et nom
        loadProjects(userId, userName);
    }

    private void loadProjects(String userId, String userName) {
        db.collection("projects")
                .get()  // On récupère tous les projets
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        projects.clear();  // Vider la liste avant de la remplir

                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            Project project = doc.toObject(Project.class);

                            // Vérifier si l'ID ou le nom de l'utilisateur est dans la liste des membres
                            if (project.getMembers().contains(userId) || project.getMembers().contains(userName)) {
                                projects.add(project);
                            }
                        }

                        // Ajouter un log pour vérifier le nombre de projets récupérés
                        Log.d("ProjectListActivity", "Projets récupérés : " + projects.size());

                        // Notifier l'adaptateur que les données ont changé
                        projectAdapter.notifyDataSetChanged();

                        // Définir un clic sur un projet
                        projectAdapter.setOnProjectClickListener(project -> {
                            // Action lors du clic sur un projet
                            Intent intent = new Intent(this, ProjectDetailActivity.class);
                            intent.putExtra("projectId", project.getId()); // Si vous avez l'ID
                            intent.putExtra("projectTitle", project.getTitle()); // Ou si vous préférez le titre
                            startActivity(intent);
                        });

                    } else {
                        Toast.makeText(this, "Aucun projet trouvé.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Inflater le menu avec le bouton de logout et d'ajout de projet
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_project_list, menu);  // Assurez-vous que le bon fichier XML est utilisé
        return true;
    }

    // Gérer l'action de déconnexion et l'ajout de projet
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            // Déconnexion
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(this, "Déconnexion réussie", Toast.LENGTH_SHORT).show();

            // Rediriger l'utilisateur vers l'écran de connexion
            Intent intent = new Intent(this, login.class); // Remplacez par l'activité de connexion de votre choix
            startActivity(intent);
            finish();  // Ferme l'activité actuelle pour ne pas revenir sur cette page après la déconnexion

            return true;
        } else if (item.getItemId() == R.id.action_add_project) {
            // Rediriger vers l'activité AddProjectActivity pour ajouter un projet
            Intent intent = new Intent(this, AddProjectActivity.class);  // Assurez-vous que cette activité existe
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
