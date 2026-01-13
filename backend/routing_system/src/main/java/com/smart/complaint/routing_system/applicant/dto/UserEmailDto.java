package com.smart.complaint.routing_system.applicant.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@Valid
public record UserEmailDto(
    @NotBlank
    String email) {
    
}
