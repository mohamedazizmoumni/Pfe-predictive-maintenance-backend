package com.pfe.predictive.inquiry.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ContactInquiryRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 150)
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Size(max = 150)
    private String email;

    @Size(max = 150)
    private String company;

    @Size(max = 30)
    private String phone;

    @Size(max = 200)
    private String subject;

    @NotBlank(message = "Message is required")
    @Size(max = 4000)
    private String message;
}
