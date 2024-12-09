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

import org.apache.commons.logging.LogFactory;

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

public class CreateTaskActivity extends AppCompatActivity {

    private static final org.apache.commons.logging.Log log = LogFactory.getLog(CreateTaskActivity.class);
    private EditText editTextTaskTitle, editTextTaskDescription;
    private Button buttonCreateTask, buttonAssignUser;
    private FirebaseFirestore db;
    private String projectId; // ID du projet à associer
    private String assignedUserId = null; // ID de l'utilisateur assigné
    private List<User> usersList = new ArrayList<>();
    private String selectedUserEmail = null; // Ajoutez cette ligne en haut de la classe
    private String assignedUserName = null;
    private String projectTitle = null;
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
                                                String userEmail = document.getString("email"); // Ajoutez cette ligne pour récupérer l'email

                                                if (userId != null && projectMembers.contains(userId) || userName!=null && projectMembers.contains(userName) ) {
                                                    filteredUserList.add(new User(userId, userName, userEmail)); // Passez l'email à l'objet User
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
                                                            // Mettre à jour assignedUserId et selectedUserEmail avec l'utilisateur sélectionné
                                                            assignedUserId = filteredUserList.get(selectedUserIndex[0]).getId();
                                                            selectedUserEmail = filteredUserList.get(selectedUserIndex[0]).getEmail(); // Récupérer l'email
                                                            assignedUserName = filteredUserList.get(selectedUserIndex[0]).getName();
                                                            projectTitle = documentSnapshot.getString("title");

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
        Task task = new Task(taskId, taskTitle, taskDescription, assignedUserName, projectId, "To Do");

        // Enregistrer la tâche dans Firestore
        db.collection("tasks").document(taskId)
                .set(task)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Task created successfully", Toast.LENGTH_SHORT).show();

                    // Redirection vers TaskListActivity
                    Intent intent = new Intent(CreateTaskActivity.this, TaskDetailActivity.class);
                    intent.putExtra("PROJECT_ID", projectId);
                    intent.putExtra("TASK_ID", taskId); // Passer l'ID du projet
                    startActivity(intent);
                    sendEmailToAssignedUser(taskTitle, taskDescription);

                    finish(); // Fermer l'activité actuelle
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to create task", Toast.LENGTH_SHORT).show();
                    Log.e("CreateTaskActivity", "Error creating task", e);
                });
    }
    private void sendEmailToAssignedUser(String taskTitle, String taskDescription) {
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

                if (selectedUserEmail != null) {
                    Message message = new MimeMessage(session);
                    message.setFrom(new InternetAddress(senderEmail));
                    message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(selectedUserEmail)); // Utiliser selectedUserEmail
                    message.setSubject("You have been added to a new task");
                    message.setText("Dear " + assignedUserName + ",\n\n" +
                            "You have been added to a new task in the project: " + projectTitle + "\n" +
                            "Task Title: " + taskTitle + "\n" +
                            "Task Description: " + taskDescription + "\n\n" +
                            "Best regards,\nTaskMate Team");

                    Transport.send(message);
                }

                runOnUiThread(() -> Toast.makeText(this, "Email sent successfully", Toast.LENGTH_SHORT).show());
            } catch (MessagingException e) {
                Log.e("EmailError", "Failed to send email", e);
                runOnUiThread(() -> Toast.makeText(this, "Failed to send email", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }


}
