package com.projectpandora.api.admin;

import com.projectpandora.api.common.ApiException;
import com.projectpandora.api.security.AccessService;
import com.projectpandora.api.user.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final AccessService access;
    private final OrganizationGuard organization;
    private final ViewRequestRepository requests;
    public AdminUserController(UserRepository users,PasswordEncoder passwords,AccessService access,
            OrganizationGuard organization,ViewRequestRepository requests) {
        this.users=users;this.passwords=passwords;this.access=access;this.organization=organization;this.requests=requests;
    }
    @GetMapping
    public List<AdminUserResponse> list() {
        access.assertAdmin();return users.findAll().stream().sorted(Comparator.comparing(UserEntity::getId)).map(AdminUserResponse::from).toList();
    }
    @PostMapping
    @Transactional
    public ResponseEntity<AdminUserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        access.assertAdmin();organization.lock();access.assertAdmin();
        String username=request.username().trim();
        if(username.length()<3 || username.length()>64) throw new ApiException(400,"用户名须为3至64字");
        if(users.findByUsername(username).isPresent()) throw new ApiException(400,"用户名已存在");
        String phone=ProfileRules.normalizePhone(request.phone());
        if(users.findByPhone(phone).isPresent()) throw new ApiException(400,"手机号已被使用");
        String idCard=ProfileRules.normalizeIdCard(request.idCard());
        if(idCard!=null && users.findByIdCard(idCard).isPresent()) throw new ApiException(400,"身份证号已被使用");
        assertAssignableManager(request.managerId());
        UserEntity user=new UserEntity();user.setUsername(username);user.setPasswordHash(passwords.encode(request.password()));
        user.setRole(request.role());user.setDisplayName(request.displayName()==null || request.displayName().isBlank() ? username : request.displayName().trim());
        user.setManagerId(request.managerId());user.setDisabled(Boolean.TRUE.equals(request.disabled()));user.setCreatedAt(Instant.now());
        applyProfile(user,phone,idCard,request.birthDate(),request.gender(),request.hireDate(),
            request.emergencyContact(),request.emergencyPhone(),request.address());
        var all=new ArrayList<>(users.findAll());all.add(user);validateHierarchy(all);
        return ResponseEntity.status(201).body(AdminUserResponse.from(users.saveAndFlush(user)));
    }
    @PatchMapping("/{id}")
    @Transactional
    public AdminUserResponse update(@PathVariable Long id,@Valid @RequestBody UpdateUserRequest request) {
        access.assertAdmin();organization.lock();access.assertAdmin();
        var user=users.findById(id).orElseThrow(()->new ApiException(404,"用户不存在"));
        Role oldRole=user.getRole();Long oldManager=user.getManagerId();boolean oldDisabled=user.isDisabled();
        if(request.role()!=null) user.setRole(request.role());
        if(request.displayName()!=null) user.setDisplayName(request.displayName().trim());
        if(request.managerId()!=null || Boolean.TRUE.equals(request.clearManager())) {
            Long manager=Boolean.TRUE.equals(request.clearManager()) ? null : request.managerId();
            assertAssignableManager(manager);user.setManagerId(manager);
        }
        if(request.disabled()!=null) user.setDisabled(request.disabled());
        if(request.password()!=null && !request.password().isBlank()) {user.setPasswordHash(passwords.encode(request.password()));user.revokeSessions();}
        applyProfileUpdate(user,request);
        validateHierarchy(users.findAll());
        if(oldRole!=user.getRole() || !Objects.equals(oldManager,user.getManagerId()) || oldDisabled!=user.isDisabled()) {
            for(var r:requests.findByRequesterIdOrTargetIdOrderByUpdatedAtDesc(id,id)) {
                if("pending".equals(r.status) || "approved".equals(r.status)) {
                    r.status="revoked";r.updatedAt=Instant.now();requests.save(r);
                }
            }
        }
        return AdminUserResponse.from(users.saveAndFlush(user));
    }
    private void assertAssignableManager(Long id) {
        if(id==null) return;
        var manager=users.findById(id).orElseThrow(()->new ApiException(400,"上级用户不存在"));
        if(manager.isDisabled() || !AccessService.isLeader(manager.getRole())) throw new ApiException(400,"上级必须是启用的业务领导");
    }
    private void validateHierarchy(List<UserEntity> all) {
        Map<Long,UserEntity> byId=new HashMap<>();all.forEach(u->{if(u.getId()!=null) byId.put(u.getId(),u);});
        for(var user:all) {
            Long managerId=user.getManagerId();
            if(managerId==null) continue;
            var manager=byId.get(managerId);
            if(user.getRole()==Role.ADMIN || user.getRole()==Role.FOUNDER || manager==null
                || !AccessService.isLeader(manager.getRole()) || AccessService.rank(manager.getRole())<=AccessService.rank(user.getRole()))
                throw new ApiException(400,"角色与上下级层级不匹配");
            Set<Long> seen=new HashSet<>();if(user.getId()!=null) seen.add(user.getId());
            Long current=managerId;
            while(current!=null) {
                if(!seen.add(current)) throw new ApiException(400,"上下级不能形成循环");
                var ancestor=byId.get(current);current=ancestor==null ? null : ancestor.getManagerId();
            }
        }
    }
    private void applyProfile(UserEntity user,String phone,String idCard,LocalDate birthDate,String gender,
            LocalDate hireDate,String emergencyContact,String emergencyPhone,String address) {
        user.setPhone(phone);
        user.setIdCard(idCard);
        user.setBirthDate(ProfileRules.validateBirthDate(birthDate));
        user.setGender(ProfileRules.normalizeGender(gender));
        user.setHireDate(hireDate);
        user.setEmergencyContact(trimToNull(emergencyContact));
        user.setEmergencyPhone(trimToNull(emergencyPhone));
        user.setAddress(trimToNull(address));
    }
    private void applyProfileUpdate(UserEntity user,UpdateUserRequest r) {
        if(r.phone()!=null && !r.phone().isBlank()) {
            String phone=ProfileRules.normalizePhone(r.phone());
            users.findByPhone(phone).filter(other->!other.getId().equals(user.getId()))
                .ifPresent(other->{throw new ApiException(400,"手机号已被使用");});
            user.setPhone(phone);
        }
        if(r.idCard()!=null) {
            String idCard=ProfileRules.normalizeIdCard(r.idCard());
            if(idCard!=null) users.findByIdCard(idCard).filter(other->!other.getId().equals(user.getId()))
                .ifPresent(other->{throw new ApiException(400,"身份证号已被使用");});
            user.setIdCard(idCard);
        }
        if(r.birthDate()!=null) user.setBirthDate(ProfileRules.validateBirthDate(r.birthDate()));
        if(r.gender()!=null) user.setGender(ProfileRules.normalizeGender(r.gender()));
        if(r.hireDate()!=null) user.setHireDate(r.hireDate());
        if(r.emergencyContact()!=null) user.setEmergencyContact(trimToNull(r.emergencyContact()));
        if(r.emergencyPhone()!=null) user.setEmergencyPhone(trimToNull(r.emergencyPhone()));
        if(r.address()!=null) user.setAddress(trimToNull(r.address()));
    }
    private static String trimToNull(String value) {
        if(value==null) return null;String trimmed=value.trim();return trimmed.isEmpty() ? null : trimmed;
    }
    public record CreateUserRequest(@NotBlank @Size(min=3,max=64) String username,
        @NotBlank @Size(min=6,max=72) String password,@NotNull Role role,@Size(max=64) String displayName,Long managerId,Boolean disabled,
        String phone,String idCard,LocalDate birthDate,String gender,LocalDate hireDate,
        String emergencyContact,String emergencyPhone,String address) {}
    public record UpdateUserRequest(Role role,@Size(max=64) String displayName,Long managerId,Boolean clearManager,Boolean disabled,
        @Size(min=6,max=72) String password,String phone,String idCard,LocalDate birthDate,String gender,LocalDate hireDate,
        String emergencyContact,String emergencyPhone,String address) {}
    public record AdminUserResponse(Long id,String username,String role,String displayName,Long managerId,boolean disabled,
        String phone,String idCard,LocalDate birthDate,String gender,LocalDate hireDate,
        String emergencyContact,String emergencyPhone,String address) {
        static AdminUserResponse from(UserEntity u) {
            return new AdminUserResponse(u.getId(),u.getUsername(),u.getRole().name(),u.getDisplayName(),u.getManagerId(),u.isDisabled(),
                u.getPhone(),u.getIdCard(),u.getBirthDate(),u.getGender(),u.getHireDate(),
                u.getEmergencyContact(),u.getEmergencyPhone(),u.getAddress());
        }
    }
}
