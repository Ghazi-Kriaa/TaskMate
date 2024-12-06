package com.example.taskmate;

import com.google.firebase.Timestamp;
import java.util.List;

public class Project {
    private String id;
    private String title;           // Titre du projet
    private String description;     // Description du projet
    private String ownerId;         // Identifiant de l'utilisateur qui a créé le projet
    private List<String> members;   // Liste des membres associés au projet
    private Timestamp createdAt;    // Date de création

    // Constructeur vide requis pour Firebase
    public Project() {}

    // Constructeur complet
    public Project(String id,String title, String description, String ownerId, List<String> members, Timestamp createdAt) {
        this.id=id;
        this.title = title;
        this.description = description;
        this.ownerId = ownerId;
        this.members = members;
        this.createdAt = createdAt;
    }
    public Project(String id,String title, String description, List<String> members, Timestamp createdAt) {
        this.id=id;
        this.title = title;
        this.description = description;
        this.members = members;
        this.createdAt = createdAt;
    }

    // Getters et setters
    public String getId(){
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public List<String> getMembers() {
        return members;
    }

    public void setMembers(List<String> members) {
        this.members = members;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
