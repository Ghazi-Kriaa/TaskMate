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
import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
public class UpdateProjectActivity extends AppCompatActivity {
    private EditText editTextTitle, editTextDescription;
    private TextInputEditText editTextMembers,editTextOwner;
    private Button buttonUpdateProject;
    private String projectId;
    private FirebaseFirestore db;

    private List<DocumentSnapshot> allUsers; // Liste des utilisateurs sous forme de DocumentSnapshot
    private List<String> selectedMembersIds; // Liste des IDs des membres sélectionnés
    private String ownerId;
    private String newOwnerId;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_project);

        // Initialiser les vues
        editTextTitle = findViewById(R.id.editTextTitle);
        editTextDescription = findViewById(R.id.editTextDescription);
        editTextOwner = findViewById(R.id.editTextOwner);
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
        editTextOwner.setOnClickListener(v -> showOwnerSelectionDialog());

        // Ouvrir la boîte de dialogue pour sélectionner les membres
        editTextMembers.setOnClickListener(v -> showMembersSelectionDialog());

        // Écouter le bouton de mise à jour
        buttonUpdateProject.setOnClickListener(v -> {
            String updatedTitle = editTextTitle.getText().toString();
            String updatedDescription = editTextDescription.getText().toString();
            updateProject(updatedTitle, updatedDescription, selectedMembersIds);
        });
    }
    private void showOwnerSelectionDialog() {
        String[] usersArray = new String[allUsers.size()];
        for (int i = 0; i < allUsers.size(); i++) {
            usersArray[i] = allUsers.get(i).getString("name");
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Project Owner");
        builder.setSingleChoiceItems(usersArray, -1, (dialog, which) -> {
            newOwnerId = allUsers.get(which).getString("id"); // Récupérer l'ID du nouvel owner
            String ownerName = allUsers.get(which).getString("name");
            editTextOwner.setText(ownerName); // Mettre à jour le champ avec le nom sélectionné
            dialog.dismiss();
        });
        builder.setNegativeButton("Cancel", null);
        builder.create().show();
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
                        loadOwnerName(ownerId);
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
    private void loadOwnerName(String ownerId) {
        db.collection("users").document(ownerId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String ownerName = documentSnapshot.getString("name");
                        editTextOwner.setText(ownerName); // Afficher le nom dans le champ dédié
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(UpdateProjectActivity.this, "Failed to load owner name", Toast.LENGTH_SHORT).show();
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

    private void updateProject(String updatedTitle, String updatedDescription, List<String> updatedMembers) {
        if (updatedTitle.isEmpty() || updatedDescription.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Initialisation des listes
        List<String> existingMembersEmails = new ArrayList<>();
        List<String> newMembersEmails = new ArrayList<>();
        String oldOwnerId = ownerId; // Propriétaire actuel du projet
        String finalOwnerId = newOwnerId != null ? newOwnerId : ownerId; // Utiliser le nouveau propriétaire si disponible, sinon conserver l'ancien

        // Obtenez les informations des membres en fonction des IDs et noms
        db.collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> foundMembersIds = new ArrayList<>();

                    for (DocumentSnapshot snapshot : queryDocumentSnapshots) {
                        String userId = snapshot.getString("id");
                        String userName = snapshot.getString("name");
                        String email = snapshot.getString("email");

                        // Vérifier si le membre correspond par ID ou par nom
                        if (updatedMembers.contains(userId) || updatedMembers.contains(userName)) {
                            if (email != null) {
                                existingMembersEmails.add(email);
                            }
                            foundMembersIds.add(userId); // Ajouter l'ID trouvé pour suivi
                        }
                    }

                    // Déterminer les nouveaux membres
                    for (String member : updatedMembers) {
                        if (!selectedMembersIds.contains(member) && !foundMembersIds.contains(member)) {
                            db.collection("users")
                                    .whereEqualTo("id", member)
                                    .get()
                                    .addOnSuccessListener(queryDocumentSnapshots1 -> {
                                        for (DocumentSnapshot snapshot : queryDocumentSnapshots1) {
                                            String email = snapshot.getString("email");
                                            if (email != null) {
                                                newMembersEmails.add(email);
                                            }
                                        }
                                    });
                        }
                    }

                    // Mettre à jour le projet dans Firestore
                    Project updatedProject = new Project(
                            projectId,
                            updatedTitle,
                            updatedDescription,
                            finalOwnerId,
                            updatedMembers,
                            Timestamp.now()
                    );

                    db.collection("projects").document(projectId).set(updatedProject)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Project updated successfully", Toast.LENGTH_SHORT).show();

                                // Envoyer les emails
                                sendEmailsToUsers(
                                        updatedTitle,
                                        updatedDescription,
                                        existingMembersEmails,
                                        newMembersEmails,
                                        oldOwnerId,
                                        finalOwnerId
                                );

                                // Passer à l'activité suivante
                                Intent intent = new Intent(UpdateProjectActivity.this, ProjectDetailActivity.class);
                                intent.putExtra("projectId", projectId);
                                startActivity(intent);
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to update project: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                Log.e("UpdateProjectActivity", "Error updating project", e);
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to fetch existing members", Toast.LENGTH_SHORT).show();
                    Log.e("UpdateProjectActivity", "Error fetching members", e);
                });
    }

    private void sendEmailsToUsers(String projectTitle, String projectDescription, List<String> existingMembersEmails, List<String> newMembersEmails, String oldOwnerId, String newOwnerId) {
        new Thread(() -> {
            try {
                final String senderEmail = "bonsoincentre.info@gmail.com"; // Replace with your email
                final String senderAppPassword = "ijfblvadxezpluiz"; // Replace with your app password
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

                // Email to existing members
                for (String recipient : existingMembersEmails) {
                    Message message = new MimeMessage(session);
                    message.setFrom(new InternetAddress(senderEmail));
                    message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
                    message.setSubject("Project Updated");
                    message.setText("Bonjour,\n\n" +
                            "The project \"" + projectTitle + "\" has been updated.\n" +
                            "Description: " + projectDescription + "\n\n" +
                            "Best regards,\nTaskMate Team");

                    Transport.send(message);
                }

                // Email to new members
                for (String recipient : newMembersEmails) {
                    Message message = new MimeMessage(session);
                    message.setFrom(new InternetAddress(senderEmail));
                    message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
                    message.setSubject("You have been added to a new project");
                    message.setText("Bonjour,\n\n" +
                            "You have been added to a new project:\n" +
                            "Title: " + projectTitle + "\n" +
                            "Description: " + projectDescription + "\n\n" +
                            "Best regards,\nTaskMate Team");

                    Transport.send(message);
                }

                // If the owner changed, send an email to the new owner
                if (!oldOwnerId.equals(newOwnerId)) {
                    db.collection("users").document(newOwnerId)
                            .get()
                            .addOnSuccessListener(documentSnapshot -> {
                                String newOwnerEmail = documentSnapshot.getString("email");
                                if (newOwnerEmail != null) {
                                    try {
                                        Message message = new MimeMessage(session);
                                        message.setFrom(new InternetAddress(senderEmail));
                                        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(newOwnerEmail));
                                        message.setSubject("You are now the project owner");
                                        message.setText("Bonjour,\n\n" +
                                                "You are now the owner of the project \"" + projectTitle + "\".\n" +
                                                "Description: " + projectDescription + "\n\n" +
                                                "Best regards,\nTaskMate Team");

                                        // Send the email to the new owner
                                        Transport.send(message);

                                        // Now that the email is sent, navigate back to the project detail page
                                        runOnUiThread(() -> {
                                            Toast.makeText(this, "Owner email sent successfully", Toast.LENGTH_SHORT).show();

                                            // Transition to the project details screen
                                            Intent intent = new Intent(UpdateProjectActivity.this, ProjectDetailActivity.class);
                                            intent.putExtra("projectId", projectId); // Pass the project ID
                                            startActivity(intent);
                                        });
                                    } catch (MessagingException e) {
                                        Log.e("EmailError", "Failed to send email to new owner", e);
                                        runOnUiThread(() -> Toast.makeText(this, "Failed to send owner email", Toast.LENGTH_SHORT).show());
                                    }
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e("EmailError", "Failed to fetch new owner email", e);
                                runOnUiThread(() -> Toast.makeText(this, "Failed to fetch new owner email", Toast.LENGTH_SHORT).show());
                            });
                } else {
                    // If the owner did not change, proceed to project detail screen
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Project updated successfully", Toast.LENGTH_SHORT).show();

                        // Transition to the project details screen
                        Intent intent = new Intent(UpdateProjectActivity.this, ProjectDetailActivity.class);
                        intent.putExtra("projectId", projectId); // Pass the project ID
                        startActivity(intent);
                    });
                }

            } catch (MessagingException e) {
                Log.e("EmailError", "Failed to send emails", e);
                runOnUiThread(() -> Toast.makeText(this, "Failed to send emails", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }






}
