package com.projectpandora.api.user;

import com.projectpandora.api.common.ApiException;
import com.projectpandora.api.security.AccessService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/view-requests")
public class ViewRequestController {
    private final ViewRequestRepository requests;
    private final UserRepository users;
    private final AccessService access;
    public ViewRequestController(ViewRequestRepository requests,UserRepository users,AccessService access) {
        this.requests=requests;this.users=users;this.access=access;
    }
    public record Input(@NotNull Long targetId,@NotBlank @Size(max=500) String reason) {}
    public record Decision(@Pattern(regexp="approved|denied|revoked") @NotNull String status) {}
    public record Response(Long id,UserSummary requester,UserSummary target,String reason,String status,Instant updatedAt) {}
    private Response response(ViewRequestEntity r) {
        return new Response(r.id,UserSummary.from(users.findById(r.requesterId).orElseThrow()),UserSummary.from(users.findById(r.targetId).orElseThrow()),r.reason,r.status,r.updatedAt);
    }
    @GetMapping
    public List<Response> list() {
        var me=access.currentUser();
        return requests.findByRequesterIdOrTargetIdOrderByUpdatedAtDesc(me.getId(),me.getId()).stream().map(this::response).toList();
    }
    @PostMapping @Transactional
    public Response create(@Valid @RequestBody Input input) {
        var target=users.findById(input.targetId()).orElseThrow(()->new ApiException(404,"上级不存在"));
        if(!access.isDirectManager(target))throw new ApiException(403,"只能申请查看直属上级");
        var r=requests.findByRequesterIdAndTargetId(access.currentUser().getId(),target.getId()).orElseGet(ViewRequestEntity::new);
        if("pending".equals(r.status) || "approved".equals(r.status))throw new ApiException(409,"已有待审批或已批准的申请");
        r.requesterId=access.currentUser().getId();r.targetId=target.getId();r.reason=input.reason().trim();r.status="pending";r.updatedAt=Instant.now();
        return response(requests.saveAndFlush(r));
    }
    @PutMapping("/{id}") @Transactional
    public Response decide(@PathVariable Long id,@Valid @RequestBody Decision input) {
        var r=requests.findById(id).orElseThrow(()->new ApiException(404,"申请不存在"));
        if(!r.targetId.equals(access.currentUser().getId()))throw new ApiException(403,"仅被申请的直属上级可审批或撤销");
        var requester=users.findById(r.requesterId).orElseThrow(()->new ApiException(404,"申请人不存在"));
        if(!r.targetId.equals(requester.getManagerId()) || requester.getRole().level()>=access.currentUser().getRole().level())throw new ApiException(403,"组织关系已变化，不能审批该申请");
        if(input.status().equals("revoked") ? !"approved".equals(r.status) : !"pending".equals(r.status))throw new ApiException(409,"申请状态已变化，请刷新");
        r.status=input.status();r.updatedAt=Instant.now();return response(requests.saveAndFlush(r));
    }
}
