package com.example.taskmate;
public class Task {
    private String id;
    private String taskTitle;
    private String description;
    private String assignedUser;
    private String projectId;
    private String status; // Nouveau champ pour représenter l'état de la tâche

    public Task() {
        // Constructeur par défaut requis pour Firestore
    }

    public Task(String id, String taskTitle, String description, String assignedUser, String projectId, String status) {
        this.id = id;
        this.taskTitle = taskTitle;
        this.description = description;
        this.assignedUser = assignedUser;
        this.projectId = projectId;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTaskTitle() {
        return taskTitle;
    }

    public void setTaskTitle(String taskTitle) {
        this.taskTitle = taskTitle;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAssignedUser() {
        return assignedUser;
    }

    public void setAssignedUser(String assignedUser) {
        this.assignedUser = assignedUser;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
