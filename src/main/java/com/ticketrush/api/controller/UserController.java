package com.ticketrush.api.controller;

import com.ticketrush.domain.user.service.UserService;
import com.ticketrush.api.dto.UserRequest;
import com.ticketrush.api.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 유저 생성 (회원가입 기반)
     */
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestBody UserRequest request) {
        return ResponseEntity.ok(UserResponse.from(userService.createUser(request.getUsername())));
    }

    /**
     * 유저 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(UserResponse.from(userService.getUser(id)));
    }

    /**
     * 유저 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}