package com.smart.complaint.routing_system.applicant.service;

import java.util.List;

import com.smart.complaint.routing_system.applicant.repository.ComplaintRepository;
import com.smart.complaint.routing_system.applicant.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.smart.complaint.routing_system.applicant.domain.UserRole;
import com.smart.complaint.routing_system.applicant.dto.ComplaintDto;
import com.smart.complaint.routing_system.applicant.entity.User;

// 민원인 서비스
@Service
@RequiredArgsConstructor
public class ApplicantService {

    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;

    @Transactional
    public String signup(String userId, String password, String displayName) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hashedPassword = encoder.encode(password);

        User user = new User(userId, hashedPassword, displayName, UserRole.CITIZEN);
        userRepository.save(user);

        return "User registered successfully";
    }

    public List<ComplaintDto> getTop3RecentComplaints(String applicantId) {

        return complaintRepository.findTop3RecentComplaintByApplicantId(applicantId);
    }

    public List<ComplaintDto> getAllComplaints(String applicantId, String keyword) {

        return complaintRepository.findAllByApplicantId(applicantId, null);
    }
}
