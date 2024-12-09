package com.example.taskmate;

public class User {
    private String name;
    private String id;
    private String email;
    private long createdAt;

    // Constructeur existant avec 4 arguments
    public User(String name, String id, String email, long createdAt) {
        this.name = name;
        this.id = id;
        this.email = email;
        this.createdAt = createdAt;
    }
    public User(String id,String name,  String email) {
        this.name = name;
        this.id = id;
        this.email = email;
    }

    // Nouveau constructeur avec 2 arguments
    public User(String name, String id) {
        this.name = name;
        this.id = id;
        // Par défaut, vous pouvez initialiser email et createdAt à null ou une valeur par défaut
        this.email = null;
        this.createdAt = System.currentTimeMillis(); // Ou une valeur par défaut
    }
    // Getters et setters pour les propriétés
    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}