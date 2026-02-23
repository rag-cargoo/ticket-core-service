package com.ticketrush.application.user.service;

import com.ticketrush.application.user.model.UserResult;
import com.ticketrush.domain.user.User;
import com.ticketrush.domain.user.UserRepository;
import com.ticketrush.domain.user.UserTier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserResult createUser(String username, String tier) {
        String normalizedUsername = normalizeRequired(username, "username");
        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }
        User saved = userRepository.save(new User(normalizedUsername, resolveTier(tier)));
        return UserResult.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResult> getUsers() {
        return userRepository.findAll().stream()
                .map(UserResult::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResult getUser(Long id) {
        return UserResult.from(requireUser(id));
    }

    @Override
    @Transactional
    public UserResult updateUser(Long id, String username, String tier, String email, String displayName) {
        User user = requireUser(id);

        String normalizedUsername = normalizeNullable(username);
        if (normalizedUsername != null && !normalizedUsername.equals(user.getUsername())
                && userRepository.existsByUsername(normalizedUsername)) {
            throw new IllegalArgumentException("Username already exists: " + normalizedUsername);
        }

        user.updateProfile(normalizedUsername, resolveTier(tier), email, displayName);
        return UserResult.from(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("User not found: " + id);
        }
        userRepository.deleteById(id);
    }

    private User requireUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
    }

    private UserTier resolveTier(String rawTier) {
        String normalizedTier = normalizeNullable(rawTier);
        if (normalizedTier == null) {
            return null;
        }
        try {
            return UserTier.valueOf(normalizedTier.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid user tier: " + rawTier);
        }
    }

    private String normalizeRequired(String value, String fieldName) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return normalized;
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
