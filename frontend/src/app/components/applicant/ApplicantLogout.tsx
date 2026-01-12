import { useEffect } from "react";
import { useNavigate } from "react-router-dom";

const ApplicantLogout = () => {
  const navigate = useNavigate();
  useEffect(() => {
    localStorage.removeItem('accessToken');
    navigate('/applicant/login', { replace: true }); // 뒤로가기 방지
  }, []);
  return null;
};

export default ApplicantLogout;