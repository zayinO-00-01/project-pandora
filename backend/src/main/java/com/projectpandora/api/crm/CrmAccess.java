package com.projectpandora.api.crm;

import static com.projectpandora.api.crm.CrmStore.*;
import com.projectpandora.api.security.AccessService;
import com.projectpandora.api.user.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CrmAccess {
    static final String IMPORT = "SHOP_IMPORT", FINANCE = "FINANCE", CONTRACTS = "CONTRACTS",
        CARD = "CARD_CORRECT", REVIEW = "CUSTOMER_REVIEW", SERVICE = "SERVICE_CORRECT";
    static final Set<String> PERMISSIONS = Set.of(IMPORT, FINANCE, CONTRACTS, CARD, REVIEW, SERVICE);
    private final AccessService access;
    private final UserRepository users;
    private final CrmStore db;

    CrmAccess(AccessService access, UserRepository users, CrmStore db) {
        this.access = access;
        this.users = users;
        this.db = db;
    }

    record Member(long id, String name) {}
    record Capabilities(boolean administrator, boolean revenue, Set<String> permissions) {}
    record Scope(long id, Role role, Set<String> permissions, Map<Long, UserEntity> users) {
        boolean founder() { return role == Role.FOUNDER; }
        boolean may(String permission) { return permissions.contains(permission); }
        boolean maySeeOwner(long owner) {
            if (owner == id || founder()) return true;
            if (!AccessService.isLeader(role)) return false;
            var seen = new HashSet<Long>();
            for (Long current = owner; current != null && seen.add(current);) {
                var user = users.get(current);
                if (user == null) return false;
                current = user.getManagerId();
                if (current != null && current == id) return true;
            }
            return false;
        }
        UserEntity active(long id) {
            var user = users.get(id);
            if (user == null || user.isDisabled() || user.getRole() == Role.ADMIN)
                throw bad("请选择在职业务成员");
            return user;
        }
        String name(long id) {
            var user = users.get(id);
            if (user == null) return "原成员";
            return user.getDisplayName() == null ? user.getUsername() : user.getDisplayName();
        }
    }

    Scope business() {
        var me = access.assertBusiness();
        return new Scope(me.getId(), me.getRole(), permissions(me.getId()), users.findAll().stream()
            .collect(Collectors.toMap(UserEntity::getId, Function.identity())));
    }
    Scope requiredPermission(String permission) {
        Scope result = business();
        if (!result.may(permission)) throw denied();
        return result;
    }
    Set<String> permissions(long userId) {
        return Set.copyOf(db.values(String.class, "SELECT permission_code FROM crm_permissions WHERE user_id=?", userId));
    }
    Capabilities capabilities() {
        var me = access.currentUser();
        var permissions = me.getRole() == Role.ADMIN ? Set.<String>of() : permissions(me.getId());
        return new Capabilities(me.getRole() == Role.ADMIN, me.getRole() == Role.FOUNDER || permissions.contains(FINANCE), permissions);
    }
    List<Member> members() {
        var scope = business();
        return scope.users().values().stream().filter(access::isActiveBusiness)
            .sorted(Comparator.comparing(UserEntity::getId))
            .map(u -> new Member(u.getId(), scope.name(u.getId()))).toList();
    }
    Set<String> inspect(long target) {
        access.assertAdmin();
        if (users.findById(target).filter(u -> u.getRole() != Role.ADMIN).isEmpty()) throw bad("请选择业务账号");
        return permissions(target);
    }
    @Transactional
    public Set<String> grant(long target, Set<String> codes) {
        db.lock();
        var admin = access.currentUser();
        if (admin.getRole() != Role.ADMIN) throw denied();
        if (codes == null || codes.stream().anyMatch(code -> code == null || !PERMISSIONS.contains(code)))
            throw bad("权限选项无效");
        var previous = inspect(target);
        if (previous.equals(codes)) return previous;
        db.write("DELETE FROM crm_permissions WHERE user_id=?", target);
        codes.stream().sorted().forEach(code ->
            db.write("INSERT INTO crm_permissions (user_id,permission_code) VALUES (?,?)", target, code));
        db.audit(admin.getId(), "GRANTS", "user=" + target + "; " + previous + " -> " + codes);
        return Set.copyOf(codes);
    }
}
