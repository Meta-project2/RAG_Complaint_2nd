package com.smart.complaint.routing_system.applicant.controller;

import com.smart.complaint.routing_system.applicant.dto.ComplaintDto;
import com.smart.complaint.routing_system.applicant.dto.ComplaintSearchResult;
import com.smart.complaint.routing_system.applicant.dto.UserLoginRequest;
import com.smart.complaint.routing_system.applicant.dto.NormalizationResponse;
import com.smart.complaint.routing_system.applicant.service.AiService;
import com.smart.complaint.routing_system.applicant.service.ApplicantService;
import com.smart.complaint.routing_system.applicant.service.jwt.JwtTokenProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;

import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties.Jwt;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 민원인 컨트롤러
@RestController
@RequiredArgsConstructor
public class ApplicantController {

    private final AiService aiService;
    private final ApplicantService applicantService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("api/applicant/signup")
    public ResponseEntity<Void> login(@RequestBody UserLoginRequest loginRequest) {

        applicantService.signup(loginRequest.userId(), loginRequest.password(), loginRequest.displayName());
        
        return ResponseEntity.ok(new LoginResponse(token));
    }

    // 토큰 유효성 검사 엔드포인트
    @GetMapping("/api/auth/validate")
    public ResponseEntity<?> validateToken(@AuthenticationPrincipal String providerId) {
        // JwtAuthenticationFilter를 거쳐 여기까지 왔다면 토큰은 유효한 것입니다.
        if (providerId == null) {
            System.out.println("컨트롤러: 인증된 유저 정보가 없습니다.");
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok().build();
    }

    // 사용자 로그인 후 main page 이동 시 로직
    // 가장 최근에 넣은 미원 3개를 불러옴
    @GetMapping("/api/applicant/complaints/top3")
    public ResponseEntity<List<ComplaintDto>> getTop3RecentComplaints(@AuthenticationPrincipal String applicantId) {

        System.out.println("현재 로그인한 사용자:" + applicantId);
        // 현재 로그인한 사용자의 최근 3개 민원 조회
        List<ComplaintDto> complaints = applicantService.getTop3RecentComplaints(applicantId);

        return ResponseEntity.ok(complaints);
    }

    // 사용자의 모든 민원 조회, 키워드 검색 가능
    @GetMapping("/api/applicant/complaints")
    public ResponseEntity<List<ComplaintDto>> getAllComplaints(@AuthenticationPrincipal String applicantId, String keyword) {

        System.out.println("현재 로그인한 사용자:" + applicantId);
        // 현재 로그인한 사용자의 모든 민원 조회
        List<ComplaintDto> complaints = applicantService.getAllComplaints(applicantId, keyword);

        return ResponseEntity.ok(complaints);
    }

    @PostMapping("/api/applicant/complaints")
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
}
