package com.smart.complaint.routing_system.applicant.service;

import com.smart.complaint.routing_system.applicant.dto.ComplaintAnswerRequest;
import com.smart.complaint.routing_system.applicant.dto.ComplaintRerouteRequest;
import com.smart.complaint.routing_system.applicant.entity.ChildComplaint;
import com.smart.complaint.routing_system.applicant.entity.Complaint;
import com.smart.complaint.routing_system.applicant.entity.ComplaintReroute;
import com.smart.complaint.routing_system.applicant.repository.ComplaintRepository;
import com.smart.complaint.routing_system.applicant.repository.ComplaintRerouteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final ComplaintRerouteRepository rerouteRepository;

    /**
     * 1. 담당자 배정 (Assign)
     * - 민원의 상태를 '처리중'으로 변경하고 담당자를 지정합니다.
     */
    public void assignManager(Long complaintId, Long userId) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new IllegalArgumentException("해당 민원을 찾을 수 없습니다. ID=" + complaintId));

        // 이미 다른 담당자가 있는지 체크하는 로직을 추가할 수 있습니다.
        // if (complaint.getAnsweredBy() != null && !complaint.getAnsweredBy().equals(userId)) { ... }

        complaint.assignManager(userId); // Entity의 편의 메서드 호출
    }

    /**
     * 2. 답변 저장/전송 (Answer)
     * [수정] 부모 ID로 들어왔지만, 실제 답변은 '가장 최신 민원(자식 포함)'에 저장해야 함.
     */
    public void saveAnswer(Long complaintId, ComplaintAnswerRequest request) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new IllegalArgumentException("해당 민원을 찾을 수 없습니다. ID=" + complaintId));

        // 1) 자식 민원이 있는지 확인
        List<ChildComplaint> children = complaint.getChildComplaints();

        if (children != null && !children.isEmpty()) {
            // 2) 자식이 있다면 가장 최신(ID가 큰 것 or CreatedAt이 최신) 자식을 찾음
            // ID Auto Increment 가정이면 ID max가 최신
            ChildComplaint latestChild = children.stream()
                    .max(Comparator.comparing(ChildComplaint::getId))
                    .orElseThrow();

            // 3) 최신 자식에 답변 저장
            if (request.isTemporary()) {
                latestChild.updateAnswerDraft(request.getAnswer());
            } else {
                // 자식 민원 종결 처리 (담당자는 일단 부모 담당자를 따라가거나, 현재 세션 유저를 넣어야 함)
                // 여기선 간단히 부모 담당자를 따라간다고 가정하거나, 컨트롤러에서 userId를 받아와야 함.
                // 일단 null 대신 complaint.getAnsweredBy() 사용
                latestChild.completeAnswer(request.getAnswer(), complaint.getAnsweredBy());
            }
        } else {
            // 4) 자식이 없으면 기존대로 부모(최초 민원)에 답변 저장
            if (request.isTemporary()) {
                complaint.updateAnswerDraft(request.getAnswer());
            } else {
                complaint.completeAnswer(request.getAnswer());
            }
        }
    }

    /**
     * 3. 재이관 요청 (Reroute)
     * - 민원 테이블은 건드리지 않고, 재이관 이력 테이블에 요청 데이터를 쌓습니다.
     * - 관리자가 승인하기 전까지는 기존 부서/담당자가 유지됩니다.
     */
    public void requestReroute(Long complaintId, ComplaintRerouteRequest request, Long userId) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new IllegalArgumentException("해당 민원을 찾을 수 없습니다. ID=" + complaintId));

        // 재이관 요청 엔티티 생성
        ComplaintReroute reroute = ComplaintReroute.builder()
                .complaint(complaint)
                .originDepartmentId(complaint.getCurrentDepartmentId()) // 현재 부서
                .targetDepartmentId(request.getTargetDeptId())          // 희망 부서
                .requestReason(request.getReason())                     // 사유
                .requesterId(userId)                                    // 요청자 (나)
                .status("PENDING")                                      // 대기 상태
                .build();

        rerouteRepository.save(reroute);
    }

    /**
     * 4. 담당 취소 (Release)
     * - 담당자를 비우고 상태를 다시 '접수(RECEIVED)'로 되돌립니다.
     */
    public void releaseManager(Long complaintId, Long userId) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new IllegalArgumentException("해당 민원을 찾을 수 없습니다."));

        if (complaint.getAnsweredBy() == null || !complaint.getAnsweredBy().equals(userId)) {
            throw new IllegalStateException("본인이 담당한 민원만 취소할 수 있습니다.");
        }
        complaint.releaseManager();
    }
}