package com.projectpandora.api.user;

import com.projectpandora.api.common.ApiException;
import com.projectpandora.api.security.AccessService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/profile")
@Transactional
public class ProfileController {
    private final UserRepository users;
    private final AccessService access;
    private final PasswordEncoder passwords;
    private final OrganizationGuard organization;
    public ProfileController(UserRepository users,AccessService access,PasswordEncoder passwords,OrganizationGuard organization) {this.users=users;this.access=access;this.passwords=passwords;this.organization=organization;}
    public record Profile(Long id,String username,String displayName,String role,String avatarData) {
        static Profile from(UserEntity user){return new Profile(user.getId(),user.getUsername(),user.getDisplayName(),user.getRole().name(),user.getAvatarData());}
    }
    public record PasswordChange(@NotBlank @Size(max=72) String currentPassword,@NotBlank @Size(min=8,max=72) String newPassword) {}
    public record AvatarChange(@NotNull @Size(max=60000) String avatarData) {}
    public record Result(String message) {}
    private UserEntity self() {return users.findById(access.assertBusiness().getId()).orElseThrow(()->new ApiException(401,"账号不可用"));}
    @GetMapping @Transactional(readOnly=true)
    public Profile get() {return Profile.from(self());}
    @PutMapping("/password")
    public Result password(@Valid @RequestBody PasswordChange request) {
        organization.lock();var user=self();
        if(request.currentPassword().getBytes(StandardCharsets.UTF_8).length>72)throw new ApiException(400,"当前密码不正确");
        if(!passwords.matches(request.currentPassword(),user.getPasswordHash()))throw new ApiException(400,"当前密码不正确");
        if(request.newPassword().getBytes(StandardCharsets.UTF_8).length>72)throw new ApiException(400,"新密码不能超过 72 个 UTF-8 字节");
        if(passwords.matches(request.newPassword(),user.getPasswordHash()))throw new ApiException(400,"新密码不能与当前密码相同");
        user.setPasswordHash(passwords.encode(request.newPassword()));user.revokeSessions();users.saveAndFlush(user);
        return new Result("密码已修改，请使用新密码重新登录");
    }
    @PutMapping("/avatar")
    public Profile avatar(@Valid @RequestBody AvatarChange request) {
        var user=self();var data=request.avatarData();
        if(!data.isEmpty())validateAvatar(data);
        user.setAvatarData(data.isEmpty()?null:data);return Profile.from(users.saveAndFlush(user));
    }
    private void validateAvatar(String value) {
        try {
            boolean png=value.startsWith("data:image/png;base64,");
            if(!png && !value.startsWith("data:image/jpeg;base64,"))throw new IllegalArgumentException();
            var bytes=Base64.getDecoder().decode(value.substring(value.indexOf(',')+1));
            if(bytes.length==0 || bytes.length>45000)throw new IllegalArgumentException();
            try(var input=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                var readers=ImageIO.getImageReaders(input);if(!readers.hasNext())throw new IllegalArgumentException();
                var reader=readers.next();try {
                    reader.setInput(input);var format=reader.getFormatName();
                    if(!(png?format.equalsIgnoreCase("png"):format.equalsIgnoreCase("jpeg")) || reader.getWidth(0)>1024 || reader.getHeight(0)>1024 || reader.read(0)==null)throw new IllegalArgumentException();
                } finally {reader.dispose();}
            }
        } catch(Exception e) {throw new ApiException(400,"头像须为有效的 PNG 或 JPEG 图片，最大 45 KB、1024×1024 像素");}
    }
}
