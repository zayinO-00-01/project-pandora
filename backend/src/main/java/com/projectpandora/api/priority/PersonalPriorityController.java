package com.projectpandora.api.priority;

import com.projectpandora.api.security.AccessService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/priorities/personal")
public class PersonalPriorityController {
    private final PersonalPriorityService service;
    private final AccessService access;

    public PersonalPriorityController(PersonalPriorityService service, AccessService access) {
        this.service = service;
        this.access = access;
    }

    @GetMapping
    public PersonalPriorityResponse get() {
        return service.get(access.assertBusiness().getId());
    }

    @PutMapping
    public PersonalPriorityResponse save(@Valid @RequestBody PersonalPriorityRequest request) {
        return service.save(access.assertBusiness().getId(), request);
    }
}
