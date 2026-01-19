package com.smart.complaint.routing_system.applicant.service;

import com.smart.complaint.routing_system.applicant.domain.IncidentStatus;
import com.smart.complaint.routing_system.applicant.dto.IncidentRequestDto;
import com.smart.complaint.routing_system.applicant.entity.Complaint;
import com.smart.complaint.routing_system.applicant.entity.Incident;
import com.smart.complaint.routing_system.applicant.repository.ComplaintRepository;
import com.smart.complaint.routing_system.applicant.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final ComplaintRepository complaintRepository;

    // ... 기존 getMajorIncidents 코드 유지 ...

    /**
     * [기능 1] 사건 제목 수정
     */
    @Transactional
    public void updateIncidentTitle(Long incidentId, String newTitle) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사건입니다."));
        incident.updateTitle(newTitle);
    }

    /**
     * [기능 2] 선택한 민원들로 '새 사건방' 만들기
     */
    @Transactional
    public void createIncidentFromComplaints(List<Long> complaintIds, String newTitle) {
        // 1. 민원 조회
        List<Complaint> complaints = complaintRepository.findAllById(complaintIds);
        if (complaints.isEmpty()) return;

        // 2. 새 사건(Incident) 생성 및 저장
        Incident newIncident = Incident.builder()
                .title(newTitle)
                .status(IncidentStatus.OPEN)
                .complaintCount(complaints.size())
                .openedAt(LocalDateTime.now()) // 일단 현재시간 (정확히는 민원 중 가장 빠른 시간이어야 함)
                .closedAt(LocalDateTime.now())
                .build();

        incidentRepository.save(newIncident);

        // 3. 민원들을 새 방으로 이동시키고, 통계 갱신
        moveComplaintsLogic(complaints, newIncident);
    }

    /**
     * [기능 3] 기존 사건으로 민원 이동
     */
    @Transactional
    public void moveComplaintsToExistingIncident(List<Long> complaintIds, Long targetIncidentId) {
        Incident targetIncident = incidentRepository.findById(targetIncidentId)
                .orElseThrow(() -> new IllegalArgumentException("목표 사건이 존재하지 않습니다."));

        List<Complaint> complaints = complaintRepository.findAllById(complaintIds);

        moveComplaintsLogic(complaints, targetIncident);
    }

    // [공통 로직] 민원 이동 및 카운트/날짜 갱신
    private void moveComplaintsLogic(List<Complaint> complaints, Incident targetIncident) {
        for (Complaint complaint : complaints) {
            // 1. 민원의 소속 변경
            complaint.assignIncident(targetIncident);
        }

        // 2. (중요) 타겟 사건의 민원 수 및 최근 발생일 재계산
        // 실제로는 DB 쿼리로 count를 다시 세는 것이 정확합니다.
        List<Complaint> allMembers = complaintRepository.findAllByIncidentId(targetIncident.getId());
        targetIncident.updateComplaintCount(allMembers.size());

        // 최근 발생일 갱신 (가장 최근 민원의 날짜로)
        allMembers.stream()
                .map(Complaint::getReceivedAt)
                .max(LocalDateTime::compareTo)
                .ifPresent(targetIncident::updateClosedAt);

        // *참고: 정확도(Silhouette Score)와 키워드 재추출은 Java에서 하기 복잡하므로,
        //       추후 파이썬 배치 스크립트가 돌 때 갱신되도록 하거나,
        //       간단한 키워드 카운팅 로직만 Java에 추가하는 것을 권장합니다.
    }
}