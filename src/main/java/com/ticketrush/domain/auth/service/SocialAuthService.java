package com.ticketrush.domain.auth.service;

import com.ticketrush.domain.auth.model.SocialAuthorizeInfo;
import com.ticketrush.domain.auth.model.SocialLoginResult;
import com.ticketrush.domain.auth.model.SocialProfile;
import com.ticketrush.domain.auth.oauth.SocialOAuthClient;
import com.ticketrush.domain.user.SocialProvider;
import com.ticketrush.domain.user.User;
import com.ticketrush.domain.user.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SocialAuthService {

    private final UserRepository userRepository;
    private final List<SocialOAuthClient> oauthClients;

    private final Map<SocialProvider, SocialOAuthClient> clientByProvider = new EnumMap<>(SocialProvider.class);

    @PostConstruct
    void initClientMap() {
        for (SocialOAuthClient oauthClient : oauthClients) {
            clientByProvider.put(oauthClient.provider(), oauthClient);
        }
    }

    @Transactional(readOnly = true)
    public SocialAuthorizeInfo getAuthorizeInfo(SocialProvider provider, String state) {
        String effectiveState = (state == null || state.isBlank()) ? UUID.randomUUID().toString() : state.trim();
        SocialOAuthClient client = getClient(provider);
        return new SocialAuthorizeInfo(provider, effectiveState, client.buildAuthorizeUrl(effectiveState));
    }

    @Transactional
    public SocialLoginResult login(SocialProvider provider, String code, String state) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("authorization code is required");
        }

        SocialProfile profile = getClient(provider).fetchProfile(code.trim(), state);
        User existing = userRepository.findBySocialProviderAndSocialId(profile.getProvider(), profile.getSocialId())
                .orElse(null);

        if (existing != null) {
            existing.updateSocialProfile(profile.getEmail(), profile.getDisplayName());
            return new SocialLoginResult(existing, false);
        }

        String username = generateUsername(profile.getProvider(), profile.getSocialId());
        User created = User.socialUser(
                username,
                profile.getProvider(),
                profile.getSocialId(),
                profile.getEmail(),
                profile.getDisplayName()
        );
        return new SocialLoginResult(userRepository.save(created), true);
    }

    private SocialOAuthClient getClient(SocialProvider provider) {
        SocialOAuthClient client = clientByProvider.get(provider);
        if (client == null) {
            throw new IllegalArgumentException("OAuth client is not configured for provider: " + provider);
        }
        return client;
    }

    private String generateUsername(SocialProvider provider, String socialId) {
        String base = provider.name().toLowerCase() + "_" + socialId;
        String candidate = base;
        int suffix = 1;
        while (userRepository.existsByUsername(candidate)) {
            candidate = base + "_" + suffix++;
        }
        return candidate;
    }
}
