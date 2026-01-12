package com.smart.complaint.routing_system.applicant.config;

import com.smart.complaint.routing_system.applicant.domain.ErrorMessage;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final ErrorMessage errorMessage;

    public BusinessException(ErrorMessage errorMessage) {
        super(errorMessage.getMessage());
        this.errorMessage = errorMessage;
    }
}