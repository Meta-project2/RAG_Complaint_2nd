package com.smart.complaint.routing_system.applicant.dto;

import lombok.Data;

@Data
public class ComplaintRerouteRequest {
    private Long targetDeptId; // 희망 부서 ID
    private String reason;     // 재이관 사유
}