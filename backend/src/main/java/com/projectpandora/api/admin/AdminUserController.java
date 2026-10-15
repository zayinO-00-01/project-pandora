package com.projectpandora.api.admin;

import com.projectpandora.api.common.ApiException;
import com.projectpandora.api.security.AccessService;
import com.projectpandora.api.user.Role;
import com.projectpandora.api.user.UserEntity;
import com.projectpandora.api.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccessService accessService;

    public AdminUserController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AccessService accessService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.accessService = accessService;
    }

    @GetMapping
    public List<AdminUserResponse> list() {
        accessService.assertAdmin();
        return userRepository.findAll().stream().map(AdminUserResponse::from).toList();
    }

    @PostMapping
    @Transactional
    public ResponseEntity<AdminUserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        accessService.assertAdmin();
        String username = request.username().trim();
        if (username.length() < 3) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "用户名至少 3 个字符");
        }
        if (userRepository.findByUsername(username).isPresent()) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "用户名已存在");
        }
        validateOrganization(null, request.role(), request.managerId());
        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        user.setDisplayName(
                request.displayName() == null || request.displayName().isBlank()
                        ? request.username().trim()
                        : request.displayName().trim());
        user.setManagerId(request.managerId());
        user.setDisabled(Boolean.TRUE.equals(request.disabled()));
        user.setCreatedAt(Instant.now());
        userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(AdminUserResponse.from(user));
    }

    @PatchMapping("/{id}")
    @Transactional
    public AdminUserResponse update(
            @PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        accessService.assertAdmin();
        UserEntity user =
                userRepository
                        .findById(id)
                        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND.value(), "用户不存在"));
        Role role = request.role() == null ? user.getRole() : request.role();
        Long managerId =
                Boolean.TRUE.equals(request.clearManager())
                        ? null
                        : request.managerId() == null ? user.getManagerId() : request.managerId();
        validateOrganization(id, role, managerId);
        user.setRole(role);
        user.setManagerId(managerId);
        if (request.displayName() != null) {
            user.setDisplayName(request.displayName().trim());
        }
        if (request.disabled() != null) {
            user.setDisabled(request.disabled());
        }
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        userRepository.save(user);
        return AdminUserResponse.from(user);
    }

    private void validateOrganization(Long userId, Role role, Long managerId) {
        if (role == Role.ADMIN) {
            if (managerId != null
                    || (userId != null && !userRepository.findByManagerId(userId).isEmpty())) {
                throw new ApiException(HttpStatus.BAD_REQUEST.value(), "管理员不能参与业务上下级关系，请先解除关联");
            }
        }
        Long currentId = managerId;
        Set<Long> seen = new HashSet<>();
        while (currentId != null) {
            if (currentId.equals(userId) || !seen.add(currentId)) {
                throw new ApiException(HttpStatus.BAD_REQUEST.value(), "组织关系不能形成环路");
            }
            UserEntity manager =
                    userRepository
                            .findById(currentId)
                            .orElseThrow(
                                    () -> new ApiException(HttpStatus.BAD_REQUEST.value(), "上级用户不存在"));
            if (manager.getRole() == Role.ADMIN) {
                throw new ApiException(HttpStatus.BAD_REQUEST.value(), "管理员不能作为业务用户上级");
            }
            currentId = manager.getManagerId();
        }
    }

    public record CreateUserRequest(
            @NotBlank @Size(min = 3, max = 64) String username,
            @NotBlank @Size(min = 6, max = 72) String password,
            @NotNull Role role,
            @Size(max = 64) String displayName,
            Long managerId,
            Boolean disabled) {}

    public record UpdateUserRequest(
            Role role,
            @Size(max = 64) String displayName,
            Long managerId,
            Boolean clearManager,
            Boolean disabled,
            @Size(min = 6, max = 72) String password) {}

    public record AdminUserResponse(
            Long id,
            String username,
            String role,
            String displayName,
            Long managerId,
            boolean disabled) {
        static AdminUserResponse from(UserEntity u) {
            return new AdminUserResponse(
                    u.getId(),
                    u.getUsername(),
                    u.getRole().name(),
                    u.getDisplayName(),
                    u.getManagerId(),
                    u.isDisabled());
        }
    }
}
