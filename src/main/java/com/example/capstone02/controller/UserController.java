package com.example.capstone02.controller;

import com.example.capstone02.model.User;
import com.example.capstone02.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/user")
    public ResponseEntity<?> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() ||
                !(authentication.getPrincipal() instanceof OAuth2User)) {
            return ResponseEntity.ok(Collections.singletonMap("authenticated", false));
        }

        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        String socialId = oauth2User.getAttribute("id").toString();

        Optional<User> userOptional = userRepository.findBySocialId(socialId);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            Map<String, Object> userInfo = Map.of(
                    "id", user.getId(),
                    "name", user.getName(),
                    "email", user.getEmail(),
                    "profileImage", user.getProfileImage(),
                    "authenticated", true
            );

            return ResponseEntity.ok(userInfo);
        }

        return ResponseEntity.ok(Collections.singletonMap("authenticated", false));
    }
}