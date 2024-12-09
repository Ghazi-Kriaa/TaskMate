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
import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class AddProjectActivity extends AppCompatActivity {

    private EditText editTextTitle, editTextDescription;
    private Button buttonSubmitProject, buttonSelectUsers;
    private List<User> usersList;
    private FirebaseFirestore db;
    private String currentUserId;

    // Liste pour stocker uniquement les ids des utilisateurs sélectionnés
    private List<String> selectedUserIds = new ArrayList<>();
    private List<String> selectedUserEmails = new ArrayList<>();

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
                                String userEmail = document.getString("email");

                                // Exclure l'utilisateur connecté
                                if (userId != null && !userId.equals(currentUserId)) {
                                    usersList.add(new User(userId, userName));
                                    selectedUserEmails.add(userEmail);
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
        if (selectedUserIds.isEmpty()) {
            Toast.makeText(this, "Please select at least one user", Toast.LENGTH_SHORT).show();
            return;
        }

        String projectId = db.collection("projects").document().getId();
        Project project = new Project(projectId, projectTitle, projectDescription, currentUserId, selectedUserIds, Timestamp.now());

        db.collection("projects").document(projectId).set(project)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Project created successfully", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(AddProjectActivity.this, ProjectDetailActivity.class);
                    intent.putExtra("projectId", projectId); // Passez l'ID du projet
                    startActivity(intent); // Démarrer la nouvelle activité
                    sendEmailsToUsers(projectTitle, projectDescription);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to create project: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("AddProjectActivity", "Error creating project", e);
                });
    }

    private void sendEmailsToUsers(String projectTitle, String projectDescription) {
        new Thread(() -> {
            try {
                final String senderEmail = "bonsoincentre.info@gmail.com"; // Replace with your email
                final String senderAppPassword = "ijfblvadxezpluiz";
                Properties props = new Properties();
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.host", "smtp.gmail.com");
                props.put("mail.smtp.port", "587");

                Session session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(senderEmail, senderAppPassword);
                    }
                });

                for (String recipient : selectedUserEmails) {
                    Message message = new MimeMessage(session);
                    message.setFrom(new InternetAddress(senderEmail));
                    message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
                    message.setSubject("You have been added to a new project");
                    message.setText("Bonjour ,\n\n" +
                            "You have been added to a new project:\n" +
                            "Title: " + projectTitle + "\n" +
                            "Description: " + projectDescription + "\n\n" +
                            "Best regards,\nTaskMate Team");

                    Transport.send(message);
                }

                runOnUiThread(() -> Toast.makeText(this, "Emails sent successfully", Toast.LENGTH_SHORT).show());
            } catch (MessagingException e) {
                Log.e("EmailError", "Failed to send emails", e);
                runOnUiThread(() -> Toast.makeText(this, "Failed to send emails", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}
