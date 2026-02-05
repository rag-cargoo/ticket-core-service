package com.ticketrush.domain.user.service;

import com.ticketrush.domain.user.User;

public interface UserService {
    User createUser(String username);
    User getUser(Long id);
    void deleteUser(Long id);
}
