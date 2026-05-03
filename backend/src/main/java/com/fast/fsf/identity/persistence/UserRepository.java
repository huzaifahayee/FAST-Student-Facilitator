package com.fast.fsf.identity.persistence;

import com.fast.fsf.identity.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * UserRepository Interface
 * 
 * What is a Repository?
 * In Spring Data JPA, a Repository is an interface that handles all 
 * interactions with the database for a specific Entity (User).
 * 
 * Why is it an interface and where is the code?
 * This is the 'magic' of Spring. By extending JpaRepository, Spring automatically
 * generates the code for Save, Delete, FindAll, and FindById at runtime.
 * You don't have to write any SQL!
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * Custom Query Method:
     * Simply by naming this method 'findByEmail', Spring is smart enough 
     * to automatically write the SQL: "SELECT * FROM users WHERE email = ..."
     */
    User findByEmail(String email);
}
