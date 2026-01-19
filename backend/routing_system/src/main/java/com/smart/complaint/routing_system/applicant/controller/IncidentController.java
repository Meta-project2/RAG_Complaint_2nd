package com.smart.complaint.routing_system.applicant.controller;

import com.smart.complaint.routing_system.applicant.domain.IncidentStatus;
import com.smart.complaint.routing_system.applicant.dto.IncidentDetailResponse;
import com.smart.complaint.routing_system.applicant.dto.IncidentListResponse;
import com.smart.complaint.routing_system.applicant.dto.IncidentRequestDto; // [추가] DTO import
import com.smart.complaint.routing_system.applicant.entity.Complaint;
import com.smart.complaint.routing_system.applicant.entity.Incident;
import com.smart.complaint.routing_system.applicant.repository.ComplaintRepository;
import com.smart.complaint.routing_system.applicant.repository.IncidentRepository;
import com.smart.complaint.routing_system.applicant.service.IncidentService; // [추가] Service import
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "사건 API", description = "사건(군집) 관리 및 조회 API")
@RestController
@RequestMapping("/api/agent/incidents")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentRepository incidentRepository;
    private final ComplaintRepository complaintRepository;
    private final IncidentService incidentService; // [추가] 비즈니스 로직 처리를 위해 주입

    @Operation(summary = "사건 목록 조회")
    @GetMapping
    public Page<IncidentListResponse> getIncidents(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) IncidentStatus status,
            @PageableDefault(size = 10) Pageable pageable) {

        // 검색/필터가 적용된 QueryDSL 메서드를 호출합니다.
        return incidentRepository.searchIncidents(search, status, pageable);
    }

    @Operation(summary = "사건 상세 조회")
    @GetMapping("/{idStr}")
    public IncidentDetailResponse getIncidentDetail(@PathVariable String idStr) {
        // [수정] ID 파싱 로직을 메소드로 분리하여 재사용
        Long id = parseIncidentId(idStr);

        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사건 없음"));

        List<Complaint> complaints = complaintRepository.findAllByIncidentId(id);
        if (complaints == null) complaints = new ArrayList<>();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        List<IncidentDetailResponse.IncidentComplaintDto> complaintDtos = complaints.stream()
                .map(c -> IncidentDetailResponse.IncidentComplaintDto.builder()
                        .originalId(c.getId())
                        .id(String.format("C2026-%04d", c.getId()))
                        .title(c.getTitle())
                        .receivedAt(c.getReceivedAt().format(formatter))
                        .status(c.getStatus())
                        .build())
                .collect(Collectors.toList());

        return IncidentDetailResponse.builder()
                .id(idStr)
                .title(incident.getTitle())
                .status(incident.getStatus())
                .district("-")
                .firstOccurred(incident.getOpenedAt() != null ? incident.getOpenedAt().format(formatter) : "-")
                .lastOccurred(incident.getClosedAt() != null ? incident.getClosedAt().format(formatter) : "-")
                .complaintCount(incident.getComplaintCount() != null ? incident.getComplaintCount() : complaints.size())
                .avgProcessTime(calculateAverageProcessTime(complaints))
                .complaints(complaintDtos)
                .build();
    }

    // ==========================================
    // [신규 기능 추가] 프론트엔드 요청 대응
    // ==========================================

    @Operation(summary = "사건 제목 수정")
    @PatchMapping("/{idStr}")
    public ResponseEntity<Void> updateTitle(
            @PathVariable String idStr,
            @RequestBody IncidentRequestDto.UpdateTitle request) {
        Long id = parseIncidentId(idStr);
        incidentService.updateIncidentTitle(id, request.getTitle());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "새 사건방 만들기 (민원 선택 생성)")
    @PostMapping("/create-from-complaints")
    public ResponseEntity<Void> createIncident(@RequestBody IncidentRequestDto.CreateRoom request) {
        incidentService.createIncidentFromComplaints(request.getComplaintIds(), request.getTitle());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "기존 사건으로 민원 이동")
    @PutMapping("/move") // URL: /api/agent/incidents/move 로 매핑됨 (프론트엔드 호출 경로 확인 필요)
    public ResponseEntity<Void> moveComplaints(@RequestBody IncidentRequestDto.MoveComplaint request) {
        incidentService.moveComplaintsToExistingIncident(request.getComplaintIds(), request.getTargetIncidentId());
        return ResponseEntity.ok().build();
    }

    // ==========================================
    // Helper Methods
    // ==========================================

    private Long parseIncidentId(String idStr) {
        try {
            // "C2026-123" 형태 등에서 숫자만 추출하거나 마지막 부분 파싱
            String[] parts = idStr.split("-");
            return Long.parseLong(parts[parts.length - 1]);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID 형식이 잘못되었습니다.");
        }
    }

    private String calculateAverageProcessTime(List<Complaint> complaints) {
        if (complaints == null || complaints.isEmpty()) return "0분";

        long totalMinutes = 0;
        int count = 0;

        for (Complaint c : complaints) {
            if (c.getReceivedAt() != null && c.getClosedAt() != null) {
                java.time.Duration duration = java.time.Duration.between(c.getReceivedAt(), c.getClosedAt());
                totalMinutes += duration.toMinutes();
                count++;
            }
        }

        if (count == 0) return "대기 중";

        long avgMinutes = totalMinutes / count;
        long days = avgMinutes / (24 * 60);
        long remainingMinutesAfterDays = avgMinutes % (24 * 60);
        long hours = remainingMinutesAfterDays / 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("일 ");
        if (hours > 0) sb.append(hours).append("시간 ");

        // 시간 단위 이하일 경우 분 단위 표시 추가 (선택 사항)
        if (days == 0 && hours == 0) sb.append(remainingMinutesAfterDays).append("분");

        return sb.toString().trim();
    }
}