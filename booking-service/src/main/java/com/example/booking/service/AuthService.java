package com.example.booking.service;

import com.example.booking.constants.SecurityConstants;
import com.example.booking.dto.LoginRequest;
import com.example.booking.dto.RegisterRequest;
import com.example.booking.dto.TokenResponse;
import com.example.booking.dto.UserResponse;
import com.example.booking.exception.AuthenticationException;
import com.example.booking.model.User;
import com.example.booking.repo.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final SecretKey key;

    public AuthService(UserRepository userRepository, @Value("${security.jwt.secret}") String secret) {
        this.userRepository = userRepository;
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(bytes, 0, padded, 0, bytes.length);
            bytes = padded;
        }
        this.key = Keys.hmacShaKeyFor(bytes);
    }

    public UserResponse register(RegisterRequest request) {
        try {
            User user = new User();
            user.setUsername(request.getUsername());
            user.setPasswordHash(BCrypt.hashpw(request.getPassword(), BCrypt.gensalt()));
            user.setRole(request.isAdmin() ? SecurityConstants.ROLE_ADMIN : SecurityConstants.ROLE_USER);
            
            User savedUser = userRepository.save(user);
            return UserResponse.fromEntity(savedUser);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Username already exists");
        }
    }

    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new AuthenticationException("Invalid username or password"));
        
        if (!BCrypt.checkpw(request.getPassword(), user.getPasswordHash())) {
            throw new AuthenticationException("Invalid username or password");
        }
        
        String token = generateToken(user);
        return new TokenResponse(token, SecurityConstants.TOKEN_TYPE);
    }

    private String generateToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(user.getId().toString())
                .addClaims(Map.of(
                        SecurityConstants.SCOPE_CLAIM, user.getRole(),
                        SecurityConstants.USERNAME_CLAIM, user.getUsername()
                ))
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(SecurityConstants.TOKEN_EXPIRATION_SECONDS)))
                .signWith(key)
                .compact();
    }
}


