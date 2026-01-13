package com.smart.complaint.routing_system.applicant.dto;

import com.smart.complaint.routing_system.applicant.domain.ComplaintStatus;
import lombok.Builder;
import java.time.LocalDateTime;

@Builder
public record ComplaintDto(
        Long id,
        String title,
        ComplaintStatus complaintStatus,
        LocalDateTime createdAt
) {}