package com.ticketrush.application.user.port.inbound;

import com.ticketrush.application.user.model.UserResult;

import java.util.List;

public interface UserUseCase {

    UserResult createUser(String username, String tier);

    List<UserResult> getUsers();

    UserResult getUser(Long id);

    UserResult updateUser(Long id, String username, String tier, String email, String displayName);

    void deleteUser(Long id);
}
