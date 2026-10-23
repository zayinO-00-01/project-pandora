package com.projectpandora.api.user;

import com.projectpandora.api.common.ApiException;
import com.projectpandora.api.security.AccessService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserRepository users;
    private final AccessService access;
    private final PasswordEncoder passwords;
    public UserController(UserRepository users,AccessService access,PasswordEncoder passwords) {
        this.users=users;this.access=access;this.passwords=passwords;
    }
    @GetMapping
    public List<UserSummary> list() {
        return access.downwardUsers().stream().sorted(Comparator.comparing(UserEntity::getId)).map(UserSummary::from).toList();
    }
    @GetMapping("/me")
    public ProfileResponse me() {
        var principal=access.currentUser();
        var user=users.findById(principal.getId())
            .orElseThrow(()->new ApiException(404,"用户不存在"));
        String managerName=user.getManagerId()==null ? null
            : users.findById(user.getManagerId()).map(UserEntity::getDisplayName).orElse(null);
        return ProfileResponse.from(user,managerName);
    }
    @PutMapping("/me/password")
    @Transactional
    public ChangePasswordResult changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        var principal=access.currentUser();
        var user=users.findById(principal.getId())
            .orElseThrow(()->new ApiException(404,"用户不存在"));
        if(!passwords.matches(request.currentPassword(),user.getPasswordHash()))
            throw new ApiException(400,"原密码不正确");
        if(request.currentPassword().equals(request.newPassword()))
            throw new ApiException(400,"新密码不能与原密码相同");
        user.setPasswordHash(passwords.encode(request.newPassword()));
        users.save(user);
        return new ChangePasswordResult(true);
    }
    @PutMapping("/me/avatar")
    @Transactional
    public ProfileResponse updateAvatar(@Valid @RequestBody AvatarRequest request) {
        var principal=access.currentUser();
        var user=users.findById(principal.getId())
            .orElseThrow(()->new ApiException(404,"用户不存在"));
        String value=request.avatar().trim();
        if(value.startsWith("data:image/")) {
            if(!value.matches("^data:image/(png|jpe?g|webp);base64,[A-Za-z0-9+/=]+$"))
                throw new ApiException(400,"头像图片格式不支持");
            if(value.length()>200000)
                throw new ApiException(400,"头像图片过大，请重新选择");
            user.setAvatarImage(value);
            user.setAvatar(null);
        } else {
            if(value.codePointCount(0,value.length())>8)
                throw new ApiException(400,"头像名称过长");
            user.setAvatar(value);
            user.setAvatarImage(null);
        }
        users.saveAndFlush(user);
        String managerName=user.getManagerId()==null ? null
            : users.findById(user.getManagerId()).map(UserEntity::getDisplayName).orElse(null);
        return ProfileResponse.from(user,managerName);
    }
    public record ChangePasswordRequest(@NotBlank String currentPassword,
        @NotBlank @Size(min=6,max=72) String newPassword) {}
    public record ChangePasswordResult(boolean ok) {}
    public record AvatarRequest(@NotBlank @Size(max=200000) String avatar) {}
    public record ProfileResponse(Long id,String username,String role,String displayName,Long managerId,String managerName,
        String avatar,String avatarImage,String phone,String idCard,LocalDate birthDate,String gender,LocalDate hireDate,
        String emergencyContact,String emergencyPhone,String address) {
        static ProfileResponse from(UserEntity u,String managerName) {
            return new ProfileResponse(u.getId(),u.getUsername(),u.getRole().name(),u.getDisplayName(),u.getManagerId(),managerName,
                u.getAvatar(),u.getAvatarImage(),u.getPhone(),u.getIdCard(),u.getBirthDate(),u.getGender(),u.getHireDate(),
                u.getEmergencyContact(),u.getEmergencyPhone(),u.getAddress());
        }
    }
    public record Organization(List<UserSummary> members,UserSummary manager,List<UserSummary> assignees) {}
    @GetMapping("/organization")
    public Organization organization() {
        var me=access.assertBusiness();
        var manager=me.getManagerId()==null ? null : users.findById(me.getManagerId()).filter(access::isDirectManager).map(UserSummary::from).orElse(null);
        var members=access.downwardUsers().stream().filter(u->!u.getId().equals(me.getId()))
            .sorted(Comparator.comparingInt((UserEntity u)->AccessService.rank(u.getRole())).reversed().thenComparing(UserEntity::getId))
            .map(UserSummary::from).toList();
        var assignees=access.dispatchAssignees().stream()
            .sorted(Comparator.comparingInt((UserEntity u)->AccessService.rank(u.getRole())).reversed().thenComparing(UserEntity::getId))
            .map(UserSummary::from).toList();
        return new Organization(members,manager,assignees);
    }
}
