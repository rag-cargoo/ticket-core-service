package com.ticketrush.domain.user.service;

import com.ticketrush.domain.user.User;
import com.ticketrush.domain.user.UserTier;

public interface UserService {
    User createUser(String username, UserTier tier);
    User getUser(Long id);
    void deleteUser(Long id);
}
