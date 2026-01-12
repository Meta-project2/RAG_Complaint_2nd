package com.smart.complaint.routing_system.applicant.service;

import java.util.List;

import com.smart.complaint.routing_system.applicant.repository.ComplaintRepository;
import com.smart.complaint.routing_system.applicant.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.smart.complaint.routing_system.applicant.config.BusinessException;
import com.smart.complaint.routing_system.applicant.domain.UserRole;
import com.smart.complaint.routing_system.applicant.dto.ComplaintDto;
import com.smart.complaint.routing_system.applicant.dto.UserLoginRequest;
import com.smart.complaint.routing_system.applicant.entity.User;
import com.smart.complaint.routing_system.applicant.domain.ErrorMessage;

// 민원인 서비스
@Service
@RequiredArgsConstructor
public class ApplicantService {

    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder encoder;

    @Transactional
    public String applicantSignUp(UserLoginRequest loginRequest) {

        String hashedPassword = encoder.encode(loginRequest.password());

        User user = new User(loginRequest.userId(), hashedPassword, loginRequest.displayName(), UserRole.CITIZEN);
        userRepository.findByUsername(loginRequest.userId()).ifPresent(existingUser -> {
            throw new BusinessException(ErrorMessage.USER_DUPLICATE);
        });
        userRepository.save(user);

        return "회원가입에 성공하였습니다.";
    }

    public String[] applicantLogin(UserLoginRequest loginRequest) {

        User user = userRepository.findByUsername(loginRequest.userId())
                .orElseThrow(() -> new BusinessException(ErrorMessage.USER_NOT_FOUND));

        if (!encoder.matches(loginRequest.password(), user.getPassword())) {
            throw new BusinessException(ErrorMessage.INVALID_PASSWORD);
        }

        return new String[] { String.valueOf(user.getId()), user.getRole().name() };
    }

    public List<ComplaintDto> getTop3RecentComplaints(String applicantId) {

        return complaintRepository.findTop3RecentComplaintByApplicantId(applicantId);
    }

    public List<ComplaintDto> getAllComplaints(String applicantId, String keyword) {

        return complaintRepository.findAllByApplicantId(applicantId, null);
    }
}
