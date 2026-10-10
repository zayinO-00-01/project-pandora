package com.projectpandora.api.auth;

import com.projectpandora.api.common.ApiException;
import com.projectpandora.api.security.JwtService;
import com.projectpandora.api.user.UserEntity;
import com.projectpandora.api.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        UserEntity user =
                userRepository
                        .findByUsername(request.username())
                        .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED.value(), "用户名或密码错误"));
        if (user.isDisabled()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED.value(), "账号已停用");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED.value(), "用户名或密码错误");
        }
        String token = jwtService.createToken(user.getId(), user.getUsername(), user.getRole());
        return new LoginResponse(token, user.getRole().name(), user.getDisplayName(), user.getId());
    }
}
