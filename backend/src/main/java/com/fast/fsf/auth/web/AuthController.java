package com.fast.fsf.auth.web;

import com.fast.fsf.identity.domain.Role;
import com.fast.fsf.identity.domain.User;
import com.fast.fsf.admin.persistence.AdminRepository;
import com.fast.fsf.identity.persistence.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * AuthController
 *
 * Handles Google OAuth token verification and role-based user upsert.
 *
 * Flow:
 *  1. React gets an access_token via useGoogleLogin (implicit flow)
 *  2. React POSTs { credential: access_token, loginAs } here
 *  3. We call Google's /oauth2/v3/userinfo with the access_token
 *  4. We enforce *.nu.edu.pk domain
 *  5. If loginAs=ADMIN → check the 'admins' DB table for this email
 *  6. Upsert user in PostgreSQL with the chosen role
 *  7. Return { id, name, email, role, picture } to React
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminRepository adminRepository;   // ← DB table, not a hardcoded list

    public record GoogleLoginRequest(String credential, String loginAs) {}
    public record UserResponse(Long id, String name, String email, String role, String picture) {}

    /**
     * POST /api/auth/google
     */
    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@RequestBody GoogleLoginRequest body) {

        if (body.credential() == null || body.credential().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Missing credential"));
        }

        // --- Step 1: Verify token & get user info from Google ---
        Map<String, Object> userInfo = getUserInfoFromGoogle(body.credential());
        if (userInfo == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "INVALID_TOKEN",
                                 "message", "Could not verify your Google account. Please try again."));
        }

        String email   = (String) userInfo.get("email");
        String name    = (String) userInfo.getOrDefault("name", "");
        String picture = (String) userInfo.getOrDefault("picture", "");

        if (email == null || email.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "NO_EMAIL",
                                 "message", "Could not retrieve your email from Google."));
        }

        // --- Step 2: Enforce *.nu.edu.pk domain ---
        // Covers @lhr.nu.edu.pk, @khi.nu.edu.pk, @nu.edu.pk, etc.
        String emailLower = email.toLowerCase();
        if (!emailLower.endsWith(".nu.edu.pk")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "NOT_NUCES",
                                 "message", "Sorry, this system is only for FAST-NUCES students."));
        }

        // --- Step 3: Role determination ---
        String requestedRole = (body.loginAs() != null) ? body.loginAs().toUpperCase() : "STUDENT";

        if ("ADMIN".equals(requestedRole)) {
            System.out.println("DEBUG AUTH: Admin login attempt — email='" + emailLower + "'");
            boolean isAuthorizedAdmin = adminRepository.existsByEmail(emailLower);
            System.out.println("DEBUG AUTH: DB lookup result=" + isAuthorizedAdmin);
            if (!isAuthorizedAdmin) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "NOT_ADMIN",
                                     "message", "Access denied. Your account is not registered as a Portal Admin."));
            }
        }

        Role assignedRole = "ADMIN".equals(requestedRole) ? Role.ADMIN : Role.STUDENT;

        // --- Step 4: Upsert user in PostgreSQL ---
        User user = userRepository.findByEmail(email);
        if (user == null) {
            user = new User(name.isBlank() ? email : name, email, assignedRole);
        } else {
            if (!name.isBlank()) user.setName(name);
            user.setRole(assignedRole);
        }

        if (user.isBanned()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "BANNED",
                                 "message", "Your account has been suspended. Contact an admin."));
        }

        user = userRepository.save(user);

        // --- Step 5: Return to React ---
        return ResponseEntity.ok(new UserResponse(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getRole().name(),
            picture
        ));
    }

    /**
     * Calls Google's userinfo endpoint with the access_token as Bearer.
     * Returns null if the token is invalid or expired.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Map<String, Object> getUserInfoFromGoogle(String accessToken) {
        try {
            RestTemplate rest = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            ResponseEntity<Map> resp = rest.exchange(
                "https://www.googleapis.com/oauth2/v3/userinfo",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
            );
            if (resp.getStatusCode().is2xxSuccessful()) {
                return resp.getBody();
            }
        } catch (Exception ignored) {}
        return null;
    }
}
