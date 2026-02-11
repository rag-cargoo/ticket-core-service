package com.ticketrush.domain.auth.oauth;

import com.ticketrush.domain.auth.model.SocialProfile;
import com.ticketrush.domain.user.SocialProvider;
import com.ticketrush.global.config.SocialLoginProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class KakaoOAuthClient implements SocialOAuthClient {

    private static final String AUTHORIZE_ENDPOINT = "https://kauth.kakao.com/oauth/authorize";
    private static final String TOKEN_ENDPOINT = "https://kauth.kakao.com/oauth/token";
    private static final String PROFILE_ENDPOINT = "https://kapi.kakao.com/v2/user/me";

    private final SocialLoginProperties socialLoginProperties;
    private final RestClient restClient = RestClient.create();

    @Override
    public SocialProvider provider() {
        return SocialProvider.KAKAO;
    }

    @Override
    public String buildAuthorizeUrl(String state) {
        SocialLoginProperties.Provider provider = socialLoginProperties.getKakao();
        validateConfig(provider.getClientId(), "KAKAO_CLIENT_ID");
        validateConfig(provider.getRedirectUri(), "KAKAO_REDIRECT_URI");

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(AUTHORIZE_ENDPOINT)
                .queryParam("response_type", "code")
                .queryParam("client_id", provider.getClientId())
                .queryParam("redirect_uri", provider.getRedirectUri());
        if (StringUtils.hasText(state)) {
            builder.queryParam("state", state);
        }
        return builder.toUriString();
    }

    @Override
    @SuppressWarnings("unchecked")
    public SocialProfile fetchProfile(String code, String state) {
        SocialLoginProperties.Provider provider = socialLoginProperties.getKakao();
        validateConfig(provider.getClientId(), "KAKAO_CLIENT_ID");
        validateConfig(provider.getRedirectUri(), "KAKAO_REDIRECT_URI");

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", provider.getClientId());
        form.add("redirect_uri", provider.getRedirectUri());
        form.add("code", code);
        if (StringUtils.hasText(provider.getClientSecret())) {
            form.add("client_secret", provider.getClientSecret());
        }

        Map<String, Object> tokenResponse = restClient.post()
                .uri(TOKEN_ENDPOINT)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);
        String accessToken = tokenResponse == null ? null : (String) tokenResponse.get("access_token");
        if (!StringUtils.hasText(accessToken)) {
            throw new IllegalArgumentException("Failed to obtain kakao access token");
        }

        Map<String, Object> profileResponse = restClient.get()
                .uri(PROFILE_ENDPOINT)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(Map.class);

        Object socialIdRaw = profileResponse == null ? null : profileResponse.get("id");
        if (socialIdRaw == null) {
            throw new IllegalArgumentException("Failed to read kakao user id");
        }

        Map<String, Object> account = profileResponse.get("kakao_account") instanceof Map
                ? (Map<String, Object>) profileResponse.get("kakao_account")
                : null;
        Map<String, Object> properties = profileResponse.get("properties") instanceof Map
                ? (Map<String, Object>) profileResponse.get("properties")
                : null;

        String email = account == null ? null : (String) account.get("email");
        String displayName = properties == null ? null : (String) properties.get("nickname");

        return new SocialProfile(
                SocialProvider.KAKAO,
                String.valueOf(socialIdRaw),
                email,
                displayName
        );
    }

    private void validateConfig(String value, String key) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("Missing oauth config: " + key);
        }
    }
}
