package com.smart.complaint.routing_system.applicant.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ErrorResponse {

    private final LocalDateTime timestamp = LocalDateTime.now();
    private final int status; // HTTP 상태 코드 (예: 400)
    private final String errorCode; // 우리만의 에러 구분 코드 (예: USER_DUPLICATE)
    private final String message; // 에러 메시지

}
