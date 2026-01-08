package com.smart.complaint.routing_system.applicant.controller;

import com.smart.complaint.routing_system.applicant.dto.ComplaintDto;
import com.smart.complaint.routing_system.applicant.dto.ComplaintSearchResult;
import com.smart.complaint.routing_system.applicant.dto.NormalizationResponse;
import com.smart.complaint.routing_system.applicant.service.AiService;
import com.smart.complaint.routing_system.applicant.service.ApplicantService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.attribute.UserPrincipal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 민원인 컨트롤러
@RestController
@RequiredArgsConstructor
public class ApplicantController {

    private final AiService aiService;
    private final ApplicantService applicantService;

    // 토큰 유효성 검사 엔드포인트
    @GetMapping("/api/auth/validate")
    public ResponseEntity<?> validateToken(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        // JwtAuthenticationFilter를 거쳐 여기까지 왔다면 토큰은 유효한 것입니다.
        if (userPrincipal != null) {
            return ResponseEntity.ok().build(); // 200 OK
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); // 401
    }

    @PostMapping("/api/complaints")
    public ResponseEntity<NormalizationResponse> sendComplaints(@AuthenticationPrincipal String applicantId,
            @RequestBody ComplaintDto request) {

        NormalizationResponse aiData = aiService.getNormalization(request);

        // 2. 서비스 호출 (분석 데이터 전달)
        // aiData 안에 들어있는 embedding(double[])을 서비스로 넘깁니다.
        List<ComplaintSearchResult> similarComplaints = aiService.getSimilarityScore(aiData.embedding());

        // 3. 결과 확인 (콘솔 출력 및 반환)
        similarComplaints.forEach(result -> {
            System.out.println("유사 민원 발견 - [" + result.simScore() + "] " + result.title());
        });

        return ResponseEntity.ok(null);
    }

    @GetMapping("/api/complaints")
    public ResponseEntity<List<ComplaintDto>> getAllComplaints(@AuthenticationPrincipal String applicantId) {

        // 현재 로그인한 사용자의 모든 민원 조회
        List<ComplaintDto> complaints = applicantService.getAllComplaints(applicantId);

        return ResponseEntity.ok(complaints);
    }
}
