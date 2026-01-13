package com.smart.complaint.routing_system.applicant.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
@Valid
public record UserSignUpDto(
    @NotBlank
    @Size(min = 5, max = 15, message = "아이디는 5~15자의 영문 소문자와 숫자만 가능합니다.")
    @Pattern(regexp = "^[a-z0-9]{5,15}$")
    String userId,
    @NotBlank
    @Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하로 입력해주세요.")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,20}$")
    String password,
    @NotBlank
    String displayName, 
    @NotBlank
    @Pattern(regexp = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    String email) {
}

