package com.ticketrush.api.controller;

import com.ticketrush.domain.user.service.UserService;
import com.ticketrush.api.dto.UserRequest;
import com.ticketrush.api.dto.UserResponse;
import com.ticketrush.api.dto.UserUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
        return ResponseEntity.ok(UserResponse.from(userService.createUser(request.getUsername(), request.getTier())));
    }

    /**
     * 유저 목록 조회
     */
    @GetMapping
    public ResponseEntity<List<UserResponse>> getUsers() {
        return ResponseEntity.ok(userService.getUsers().stream()
                .map(UserResponse::from)
                .toList());
    }

    /**
     * 유저 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(UserResponse.from(userService.getUser(id)));
    }

    /**
     * 유저 정보 수정
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id, @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(UserResponse.from(
                userService.updateUser(id, request.getUsername(), request.getTier(), request.getEmail(), request.getDisplayName())
        ));
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
