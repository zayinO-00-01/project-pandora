package com.projectpandora.api.label;
import com.projectpandora.api.common.ApiException;
import com.projectpandora.api.security.AccessService;
import com.projectpandora.api.user.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/labels") @Transactional
public class LabelController {
    private final LabelRepository labels;private final AccessService access;private final OrganizationGuard organization;
    public LabelController(LabelRepository labels,AccessService access,OrganizationGuard organization){this.labels=labels;this.access=access;this.organization=organization;}
    public record Input(@NotBlank @Size(max=64) String name,@NotNull @Pattern(regexp="#[0-9a-fA-F]{6}") String color) {}
    private void founder(){organization.lock();if(access.assertBusiness().getRole()!=Role.FOUNDER)throw new ApiException(403,"仅创始人可维护标签");}
    @GetMapping @Transactional(readOnly=true) public List<LabelResponse> list(){access.assertBusiness();return labels.findAll().stream().filter(WorkLabel::isActive).map(LabelResponse::from).toList();}
    @PostMapping public LabelResponse create(@Valid @RequestBody Input r){founder();var label=new WorkLabel();label.setName(r.name().trim());label.setColor(r.color().toUpperCase());return LabelResponse.from(labels.saveAndFlush(label));}
    @PutMapping("/{id}") public LabelResponse update(@PathVariable Long id,@Valid @RequestBody Input r){founder();var label=labels.findById(id).orElseThrow(()->new ApiException(404,"标签不存在"));label.setName(r.name().trim());label.setColor(r.color().toUpperCase());return LabelResponse.from(labels.saveAndFlush(label));}
    @DeleteMapping("/{id}") public java.util.Map<String,String> archive(@PathVariable Long id){founder();var label=labels.findById(id).orElseThrow(()->new ApiException(404,"标签不存在"));label.setActive(false);labels.saveAndFlush(label);return java.util.Map.of("message","标签已停用，历史记录保留原标签");}
}
