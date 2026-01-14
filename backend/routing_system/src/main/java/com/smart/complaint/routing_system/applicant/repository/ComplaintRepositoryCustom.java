package com.smart.complaint.routing_system.applicant.repository;

import com.smart.complaint.routing_system.applicant.dto.ComplaintDetailResponse;
import com.smart.complaint.routing_system.applicant.dto.ComplaintDetailDto;
import com.smart.complaint.routing_system.applicant.dto.ComplaintDto;
import com.smart.complaint.routing_system.applicant.dto.ComplaintResponse;
import com.smart.complaint.routing_system.applicant.dto.ComplaintSearchCondition;
import com.smart.complaint.routing_system.applicant.dto.ComplaintSearchResult;

import java.util.List;

public interface ComplaintRepositoryCustom {
    List<ComplaintResponse> search(Long departmentId, ComplaintSearchCondition condition);
    List<ComplaintSearchResult> findSimilarComplaint(double[] queryEmbedding, int limit);

    public ComplaintDetailResponse getComplaintDetail(Long complaintId);
    List<ComplaintDto> findTop3RecentComplaintByApplicantId(Long id);
    List<ComplaintDetailDto> findAllByApplicantId(Long applicantId, String keyword);
}