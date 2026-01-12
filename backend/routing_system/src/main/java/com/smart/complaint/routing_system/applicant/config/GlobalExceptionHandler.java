package com.smart.complaint.routing_system.applicant.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.smart.complaint.routing_system.applicant.dto.ErrorResponse;
import com.smart.complaint.routing_system.applicant.domain.ErrorMessage;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    protected ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        ErrorMessage errorMessage = e.getErrorMessage();
        ErrorResponse response = ErrorResponse.builder()
                .status(errorMessage.getStatus())
                .errorCode(errorMessage.getCode())
                .message(errorMessage.getMessage())
                .build();
        return new ResponseEntity<>(response, HttpStatus.valueOf(errorMessage.getStatus()));
    }

    // 그 외 예상치 못한 모든 예외 처리
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorResponse> handleException(Exception e) {
        ErrorResponse response = ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .errorCode("GLOBAL_ERROR")
                .message("예상치 못한 오류가 발생했습니다.")
                .build();
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
