package com.projectpandora.api.user;

import com.projectpandora.api.common.ApiException;
import com.projectpandora.api.security.AccessService;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserRepository users;
    private final AccessService access;
    public UserController(UserRepository users,AccessService access) {this.users=users;this.access=access;}
    @GetMapping
    public List<UserSummary> list() {
        return access.downwardUsers().stream().sorted(Comparator.comparing(UserEntity::getId)).map(UserSummary::from).toList();
    }
    @GetMapping("/me")
    public ProfileResponse me() {
        var principal=access.currentUser();
        var user=users.findById(principal.getId())
            .orElseThrow(()->new ApiException(404,"用户不存在"));
        return ProfileResponse.from(user);
    }
    public record ProfileResponse(Long id,String username,String role,String displayName,Long managerId,
        String phone,String idCard,LocalDate birthDate,String gender,LocalDate hireDate,
        String emergencyContact,String emergencyPhone,String address) {
        static ProfileResponse from(UserEntity u) {
            return new ProfileResponse(u.getId(),u.getUsername(),u.getRole().name(),u.getDisplayName(),u.getManagerId(),
                u.getPhone(),u.getIdCard(),u.getBirthDate(),u.getGender(),u.getHireDate(),
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
