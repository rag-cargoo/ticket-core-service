package com.ticketrush.domain.user.service;

import com.ticketrush.domain.user.User;
import com.ticketrush.domain.user.UserTier;

import java.util.List;

public interface UserService {
    User createUser(String username, UserTier tier);
    List<User> getUsers();
    User getUser(Long id);
    User updateUser(Long id, String username, UserTier tier, String email, String displayName);
    void deleteUser(Long id);
}
