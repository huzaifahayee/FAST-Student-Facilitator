package com.fast.fsf.controller;

import com.fast.fsf.model.User;
import com.fast.fsf.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * UserController
 * 
 * What is a Controller?
 * A Controller is the "gatekeeper" of our API. It listens for HTTP requests 
 * from the frontend (Vite) and decides what to do with them.
 * 
 * SOLID Note: Open-Closed Principle
 * Because we use UserRepository (an interface), we can swap out the database 
 * or change how data is stored later without ever changing this Controller's code.
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:5173")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    /**
     * GET /api/users
     */
    @GetMapping
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * GET /api/users/count
     */
    @GetMapping("/count")
    public long getUserCount() {
        return userRepository.count();
    }

    /**
     * POST /api/users
     */
    @PostMapping
    public User createUser(@RequestBody User user) {
        return userRepository.save(user);
    }

    /**
     * PUT /api/users/{id}/ban
     * Toggles the banned status of a user.
     */
    @PutMapping("/{id}/ban")
    public User toggleBan(@PathVariable Long id) {
        return userRepository.findById(id)
            .map(user -> {
                user.setBanned(!user.isBanned());
                return userRepository.save(user);
            })
            .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
