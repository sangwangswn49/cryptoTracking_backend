package com.example.crypto_backend.controller;

import com.example.crypto_backend.jwt.JwtUtil;
import com.example.crypto_backend.DTO.userDTO;
import com.example.crypto_backend.model.User;
import com.example.crypto_backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/auth")
public class AuthController {

    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final AuthenticationManager authenticationManager;

    public AuthController(JwtUtil jwtUtil, UserService userService, AuthenticationManager authenticationManager) {
        this.jwtUtil = jwtUtil;
        this.userService = userService;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid userDTO loginInfo) {
        try {
            // Authenticate the user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginInfo.getUserName(), loginInfo.getPassWord())
            );

            // If authentication is successful, retrieve the user and generate a JWT token
            User user = userService.getUserByUserName(authentication.getName());
            String jwt = jwtUtil.generateToken(user.getUserName(), user.getRole());
            return ResponseEntity.ok(Map.of("token", jwt));

        } catch (AuthenticationException e) {
            // Log the exception and return unauthorized status
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials"));
        } catch (Exception e) {
            // Log unexpected exceptions
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "An error occurred"));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid User user) {
        try {
            if (userService.getUserByUserNameSimply(user.getUserName()) != null) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Username already exists"));
            }
            user.setRole("USER"); // Default role for new users
            User createdUser = userService.createUser(user);

            // If registration is successful, generate a JWT token for the new user
            String jwt = jwtUtil.generateToken(createdUser.getUserName(), createdUser.getRole());
            return ResponseEntity.ok(Map.of("token", jwt));

        } catch (Exception e) {
            // Log the exception and return bad request status
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "User registration failed"));
        }
    }
}