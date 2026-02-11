package com.ticketrush.domain.auth.oauth;

import com.ticketrush.domain.auth.model.SocialProfile;
import com.ticketrush.domain.user.SocialProvider;
import com.ticketrush.global.config.SocialLoginProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class NaverOAuthClient implements SocialOAuthClient {

    private static final String AUTHORIZE_ENDPOINT = "https://nid.naver.com/oauth2.0/authorize";
    private static final String TOKEN_ENDPOINT = "https://nid.naver.com/oauth2.0/token";
    private static final String PROFILE_ENDPOINT = "https://openapi.naver.com/v1/nid/me";

    private final SocialLoginProperties socialLoginProperties;
    private final RestClient restClient = RestClient.create();

    @Override
    public SocialProvider provider() {
        return SocialProvider.NAVER;
    }

    @Override
    public String buildAuthorizeUrl(String state) {
        SocialLoginProperties.Provider provider = socialLoginProperties.getNaver();
        validateConfig(provider.getClientId(), "NAVER_CLIENT_ID");
        validateConfig(provider.getRedirectUri(), "NAVER_REDIRECT_URI");
        if (!StringUtils.hasText(state)) {
            throw new IllegalArgumentException("state is required for naver authorize url");
        }

        return UriComponentsBuilder.fromHttpUrl(AUTHORIZE_ENDPOINT)
                .queryParam("response_type", "code")
                .queryParam("client_id", provider.getClientId())
                .queryParam("redirect_uri", provider.getRedirectUri())
                .queryParam("state", state)
                .toUriString();
    }

    @Override
    @SuppressWarnings("unchecked")
    public SocialProfile fetchProfile(String code, String state) {
        SocialLoginProperties.Provider provider = socialLoginProperties.getNaver();
        validateConfig(provider.getClientId(), "NAVER_CLIENT_ID");
        validateConfig(provider.getClientSecret(), "NAVER_CLIENT_SECRET");
        validateConfig(provider.getRedirectUri(), "NAVER_REDIRECT_URI");
        if (!StringUtils.hasText(state)) {
            throw new IllegalArgumentException("state is required for naver token exchange");
        }

        String tokenUri = UriComponentsBuilder.fromHttpUrl(TOKEN_ENDPOINT)
                .queryParam("grant_type", "authorization_code")
                .queryParam("client_id", provider.getClientId())
                .queryParam("client_secret", provider.getClientSecret())
                .queryParam("code", code)
                .queryParam("state", state)
                .toUriString();

        Map<String, Object> tokenResponse = restClient.get()
                .uri(tokenUri)
                .retrieve()
                .body(Map.class);
        String accessToken = tokenResponse == null ? null : (String) tokenResponse.get("access_token");
        if (!StringUtils.hasText(accessToken)) {
            throw new IllegalArgumentException("Failed to obtain naver access token");
        }

        Map<String, Object> profileResponse = restClient.get()
                .uri(PROFILE_ENDPOINT)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(Map.class);
        Map<String, Object> responseBody = profileResponse != null && profileResponse.get("response") instanceof Map
                ? (Map<String, Object>) profileResponse.get("response")
                : null;
        if (responseBody == null || !StringUtils.hasText((String) responseBody.get("id"))) {
            throw new IllegalArgumentException("Failed to read naver user id");
        }

        String socialId = (String) responseBody.get("id");
        String email = (String) responseBody.get("email");
        String displayName = responseBody.get("name") != null
                ? (String) responseBody.get("name")
                : (String) responseBody.get("nickname");

        return new SocialProfile(SocialProvider.NAVER, socialId, email, displayName);
    }

    private void validateConfig(String value, String key) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("Missing oauth config: " + key);
        }
    }
}
