package com.example.csit228capstone.model;

public class AdminProfile {
    private final String id;
    private final String username;
    private String firstName;
    private String lastName;

    public AdminProfile(String id, String username, String firstName, String lastName) {
        this.id = id;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public String getId()        { return id; }
    public String getUsername()  { return username; }
    public String getFirstName() { return firstName; }
    public String getLastName()  { return lastName; }
}