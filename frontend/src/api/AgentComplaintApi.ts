import { springApi } from "../lib/springApi";

// 백엔드 DB 테이블과 매칭되는 타입 정의
export interface ComplaintDto {
  id: number;
  title: string;
  body: string;
  addressText?: string;     // DB 컬럼: address_text
  status: 'RECEIVED' | 'NORMALIZED' | 'RECOMMENDED' | 'IN_PROGRESS' | 'CLOSED';
  urgency: 'LOW' | 'MEDIUM' | 'HIGH';
  receivedAt: string;
  createdAt: string;        // DB 컬럼: created_at
  updatedAt?: string;
  districtId?: number;
  incidentId?: string | null;
  // 필요한 경우 프론트엔드용 추가 필드
  category?: string; // 백엔드에서 아직 안 주면 프론트에서 임시 처리 필요할 수 있음
  tags?: string[];
  neutralSummary?: string;    //  중립 요약
}

export const AgentComplaintApi = {
  // 1. [목록] 모든 민원 가져오기
  getAll: async (params?: any) => {
    // 컨트롤러 주소: /api/agent/complaints
    const response = await springApi.get<ComplaintDto[]>("/api/agent/complaints", { params });
    return response.data;
  },

  // 2. [상세] 특정 민원 1개 가져오기
  getDetail: async (id: string | number) => {
    const response = await springApi.get<ComplaintDto>(`/api/agent/complaints/${id}`);
    return response.data;
  }
};