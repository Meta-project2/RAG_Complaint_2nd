package com.smart.complaint.routing_system.applicant.dto;

import java.util.Map;

@SuppressWarnings("unchecked")
public record OAuth2Attributes(
        Map<String, Object> attributes,
        String nameAttributeKey,
        String name,
        String email,
        String id) {
    public static OAuth2Attributes of(String registrationId, String userNameAttributeName,
            Map<String, Object> attributes) {
        if ("naver".equals(registrationId)) {
            // 네이버는 userNameAttributeName이 보통 "response"로 오지만,
            // 실제 식별값은 그 안의 "id"이므로 "id"를 넘겨줍니다.
            return ofNaver("id", attributes);
        }
        if ("kakao".equals(registrationId)) {
            return ofKakao("id", attributes);
        }
        return null;
    }

    private static OAuth2Attributes ofNaver(String nameAttributeKey, Map<String, Object> attributes) {
        // 네이버는 attributes 안에 "response"라는 Map이 또 들어있음
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");

        return new OAuth2Attributes(
                response, // 서비스 레이어에 전달할 attributes를 response 맵으로 교체
                nameAttributeKey,
                (String) response.get("name"),
                (String) response.get("email"),
                (String) response.get("id"));
    }

    private static OAuth2Attributes ofKakao(String nameAttributeKey, Map<String, Object> attributes) {
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

        String email = (kakaoAccount != null && kakaoAccount.get("email") != null)
                ? (String) kakaoAccount.get("email")
                : "NO_EMAIL";

        return new OAuth2Attributes(
                attributes,
                nameAttributeKey,
                (String) profile.get("nickname"),
                email,
                String.valueOf(attributes.get("id")));
    }
}