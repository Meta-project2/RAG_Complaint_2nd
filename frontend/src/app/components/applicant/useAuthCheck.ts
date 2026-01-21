import { useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import api from './AxiosInterface';
import Swal from 'sweetalert2';

export function useAuthGuard() {
    const navigate = useNavigate();
    const location = useLocation();

    useEffect(() => {
        const checkAuth = async () => {
            const token = localStorage.getItem('accessToken');

            // 1. 프론트엔드 1차 체크: 토큰 자체가 없는 경우
            if (!token) {
                handleUnauthorized("로그인이 필요한 서비스입니다.");
                return;
            }

            try {
                // 2. 백엔드 2차 체크: 토큰 유효성 검증 API 호출
                await api.get('/auth/validate');
            } catch (error) {
                // 3. 토큰이 만료되었거나 변조된 경우 (401 에러 등)
                handleUnauthorized("세션이 만료되었습니다. 다시 로그인해주세요.");
            }
        };

        // 알림 후 메인으로 쫓아내는 공통 함수
        const handleUnauthorized = (message: string) => {
            localStorage.removeItem('accessToken'); // 유효하지 않은 토큰 삭제
            Swal.fire({
                title: '접근 제한',
                text: message,
                icon: 'error',
                confirmButtonColor: '#1e40af',
                confirmButtonText: '확인'
            }).then(() => {
                navigate('/applicant/main'); // 메인으로 내쫓기
            });
        };

        // 특정 경로(보호하고 싶은 경로들)에서만 실행되도록 조건 부여 가능
        const protectedPaths = ['/applicant/complaints', '/applicant/complaints/form'];
        const isProtected = protectedPaths.some(path => location.pathname.startsWith(path));

        if (isProtected) {
            checkAuth();
        }
    }, [location.pathname, navigate]);
}