package com.ticketrush.application.auth.service;

import com.ticketrush.application.auth.model.SocialAuthorizeResult;
import com.ticketrush.application.auth.model.SocialLoginUserResult;
import com.ticketrush.domain.auth.model.SocialProfile;
import com.ticketrush.domain.auth.oauth.SocialOAuthClient;
import com.ticketrush.domain.user.SocialProvider;
import com.ticketrush.domain.user.User;
import com.ticketrush.domain.user.UserRepository;
import com.ticketrush.domain.user.UserTier;
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
public class SocialAuthServiceImpl implements SocialAuthService {

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
    public SocialAuthorizeResult getAuthorizeInfo(String provider, String state) {
        SocialProvider socialProvider = SocialProvider.from(provider);
        String effectiveState = (state == null || state.isBlank()) ? UUID.randomUUID().toString() : state.trim();
        SocialOAuthClient client = getClient(socialProvider);
        return new SocialAuthorizeResult(
                socialProvider.name().toLowerCase(),
                effectiveState,
                client.buildAuthorizeUrl(effectiveState)
        );
    }

    @Transactional
    public SocialLoginUserResult login(String provider, String code, String state) {
        SocialProvider socialProvider = SocialProvider.from(provider);
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("authorization code is required");
        }

        SocialProfile profile = getClient(socialProvider).fetchProfile(code.trim(), state);
        User existing = userRepository.findBySocialProviderAndSocialId(profile.getProvider(), profile.getSocialId())
                .orElse(null);

        if (existing != null) {
            existing.updateSocialProfile(profile.getEmail(), profile.getDisplayName());
            return toLoginResult(existing, false);
        }

        String username = generateUsername(profile.getProvider(), profile.getSocialId());
        User created = User.socialUser(
                username,
                UserTier.BASIC,
                profile.getProvider(),
                profile.getSocialId(),
                profile.getEmail(),
                profile.getDisplayName()
        );
        return toLoginResult(userRepository.save(created), true);
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

    private SocialLoginUserResult toLoginResult(User user, boolean newUser) {
        String provider = user.getSocialProvider() == null ? null : user.getSocialProvider().name().toLowerCase();
        String role = user.getRole() == null ? null : user.getRole().name();
        return new SocialLoginUserResult(
                user.getId(),
                user.getUsername(),
                provider,
                user.getSocialId(),
                user.getEmail(),
                user.getDisplayName(),
                role,
                newUser
        );
    }
}
