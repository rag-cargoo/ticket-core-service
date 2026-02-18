package com.ticketrush.domain.user.service;

import com.ticketrush.domain.user.User;
import com.ticketrush.domain.user.UserRepository;
import com.ticketrush.domain.user.UserTier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public User createUser(String username, UserTier tier) {
        String normalizedUsername = normalizeRequired(username, "username");
        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }
        return userRepository.save(new User(normalizedUsername, tier));
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getUsers() {
        return userRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
    }

    @Override
    @Transactional
    public User updateUser(Long id, String username, UserTier tier, String email, String displayName) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));

        String normalizedUsername = normalizeNullable(username);
        if (normalizedUsername != null && !normalizedUsername.equals(user.getUsername())
                && userRepository.existsByUsername(normalizedUsername)) {
            throw new IllegalArgumentException("Username already exists: " + normalizedUsername);
        }

        user.updateProfile(normalizedUsername, tier, email, displayName);
        return user;
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("User not found: " + id);
        }
        userRepository.deleteById(id);
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
