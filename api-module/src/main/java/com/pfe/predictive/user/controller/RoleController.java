package com.pfe.predictive.user.controller;

import com.pfe.predictive.data.repository.RoleRepository;
import com.pfe.predictive.user.dto.RoleDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
public class RoleController {

    private final RoleRepository roleRepository;

    public RoleController(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @GetMapping
    public ResponseEntity<List<RoleDTO>> getRoles() {
        List<RoleDTO> roles = roleRepository.findAll().stream()
                .sorted(Comparator.comparing(role -> role.getName() == null ? "" : role.getName()))
                .map(role -> new RoleDTO(role.getId(), role.getName(), role.getDescription()))
                .toList();

        return ResponseEntity.ok(roles);
    }
}
