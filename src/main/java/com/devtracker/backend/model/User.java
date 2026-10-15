package com.devtracker.backend.model;

public class User {

    private int id;
    private String name;
    private String email;
    private String password;

    // Google Login fields
    private String googleId;
    private String authProvider;
    private String profilePicture;


    // ============================================================
    // ID
    // ============================================================

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }


    // ============================================================
    // NAME
    // ============================================================

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    // ============================================================
    // EMAIL
    // ============================================================

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }


    // ============================================================
    // PASSWORD
    // ============================================================

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }


    // ============================================================
    // GOOGLE ID
    // ============================================================

    public String getGoogleId() {
        return googleId;
    }

    public void setGoogleId(String googleId) {
        this.googleId = googleId;
    }


    // ============================================================
    // AUTH PROVIDER
    // ============================================================

    public String getAuthProvider() {
        return authProvider;
    }

    public void setAuthProvider(String authProvider) {
        this.authProvider = authProvider;
    }


    // ============================================================
    // PROFILE PICTURE
    // ============================================================

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }
}