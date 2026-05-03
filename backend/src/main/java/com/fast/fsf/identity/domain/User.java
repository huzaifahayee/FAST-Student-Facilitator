package com.fast.fsf.identity.domain;

import jakarta.persistence.*;

/**
 * User Entity
 * 
 * What is an Entity?
 * In Spring Boot / JPA, an 'Entity' is a Java class that is mapped 
 * directly to a table in your PostgreSQL database. 
 * Every instance of this class represents a row in the "users" table.
 * 
 * SOLID Note: Single Responsibility Principle
 * This class has one job: To represent the data structure of a User.
 * It doesn't handle business logic or database queries; it just holds data.
 */
@Entity
@Table(name = "users") // Tells PostgreSQL to name the table "users"
public class User {

    @Id // Marks this field as the Primary Key
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Automatically increments the ID (1, 2, 3...)
    private Long id;

    @Column(nullable = false) // Ensures this field cannot be empty
    private String name;

    @Column(unique = true, nullable = false) // Ensures no two students have the same email
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.STUDENT;

    @Column(nullable = false)
    private boolean banned = false;

    // --- CONSTRUCTORS ---
    public User() {}

    public User(String name, String email) {
        this.name = name;
        this.email = email;
        this.role = Role.STUDENT;
        this.banned = false;
    }

    public User(String name, String email, Role role) {
        this.name = name;
        this.email = email;
        this.role = role;
        this.banned = false;
    }

    // --- GETTERS AND SETTERS ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public boolean isBanned() { return banned; }
    public void setBanned(boolean banned) { this.banned = banned; }
}
