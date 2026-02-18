package com.ticketrush.domain.user.service;

import com.ticketrush.domain.user.UserTier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(UserServiceImpl.class)
class UserServiceImplDataJpaTest {

    @Autowired
    private UserService userService;

    @Test
    void updateUser_updatesTierAndProfileFields() {
        var created = userService.createUser("alpha", UserTier.BASIC);

        var updated = userService.updateUser(
                created.getId(),
                "alpha-updated",
                UserTier.VIP,
                "alpha@example.com",
                "Alpha User"
        );

        assertThat(updated.getUsername()).isEqualTo("alpha-updated");
        assertThat(updated.getTier()).isEqualTo(UserTier.VIP);
        assertThat(updated.getEmail()).isEqualTo("alpha@example.com");
        assertThat(updated.getDisplayName()).isEqualTo("Alpha User");
    }

    @Test
    void updateUser_throwsWhenUsernameDuplicated() {
        var first = userService.createUser("first-user", UserTier.BASIC);
        var second = userService.createUser("second-user", UserTier.BASIC);

        assertThatThrownBy(() -> userService.updateUser(
                second.getId(),
                first.getUsername(),
                null,
                null,
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username already exists");
    }
}
