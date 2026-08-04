package com.pfe.predictive.preference.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserPreferenceValue {
    @NotBlank
    private String value;
}
