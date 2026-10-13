package com.projectpandora.api.auth;

import com.projectpandora.api.common.ApiException;
import com.projectpandora.api.security.JwtService;
import com.projectpandora.api.user.Role;
import com.projectpandora.api.user.UserEntity;
import com.projectpandora.api.user.UserRepository;
import jakarta.validation.Valid;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(
            UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "用户名已存在");
        }
        UserEntity user = new UserEntity();
        user.setUsername(request.username().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.STAFF);
        user.setDisplayName(
                request.displayName() == null || request.displayName().isBlank()
                        ? request.username().trim()
                        : request.displayName().trim());
        user.setCreatedAt(Instant.now());
        userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(toLoginResponse(user));
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        UserEntity user =
                userRepository
                        .findByUsername(request.username())
                        .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED.value(), "用户名或密码错误"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED.value(), "用户名或密码错误");
        }
        return toLoginResponse(user);
    }

    private LoginResponse toLoginResponse(UserEntity user) {
        String token = jwtService.createToken(user.getId(), user.getUsername(), user.getRole());
        return new LoginResponse(token, user.getRole().name(), user.getDisplayName(), user.getId());
    }
}
