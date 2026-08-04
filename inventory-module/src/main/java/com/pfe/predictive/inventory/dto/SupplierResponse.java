package com.pfe.predictive.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierResponse {
    private Long id;
    private String name;
    private String contactName;
    private String email;
    private String phone;
    private String address;
    private Integer leadTimeDays;
    private String notes;
    private boolean active;
    private LocalDateTime createdDate;
}
