import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import axios from 'axios';

const ApplicantComplaintDetailPage = () => {
  // 1. URL에서 :id 값을 가져옴
  const { id } = useParams<{ id: string }>(); 
  const [complaint, setComplaint] = useState<any>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchDetail = async () => {
      try {
        const token = localStorage.getItem('accessToken');
        // 2. 전달받은 id를 사용해 백엔드 API 호출
        const response = await axios.get(`http://localhost:8080/api/applicant/complaints/${id}`, {
          headers: { Authorization: `Bearer ${token}` }
        });
        setComplaint(response.data);
      } catch (error) {
        console.error("상세 정보 로드 실패:", error);
      } finally {
        setLoading(false);
      }
    };

    if (id) fetchDetail();
  }, [id]); // id가 바뀔 때마다 실행

  if (loading) return <div>로딩 중...</div>;
  if (!complaint) return <div>민원을 찾을 수 없습니다.</div>;

  return (
    <div className="max-w-4xl mx-auto p-6">
      <h1 className="text-2xl font-bold">{complaint.title}</h1>
      <p className="mt-4">{complaint.body}</p>
      {/* 상세 데이터 렌더링... */}
    </div>
  );
};

export default ApplicantComplaintDetailPage;