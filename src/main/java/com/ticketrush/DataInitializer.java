package com.ticketrush;

import com.ticketrush.domain.user.User;
import com.ticketrush.domain.user.UserRole;
import com.ticketrush.domain.user.UserRepository;
import com.ticketrush.domain.user.UserTier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * [System] 초기 시스템 기동 확인용 데이터 이니셜라이저
 * 상세한 테스트 데이터는 /api/concerts/setup API를 통해 동적으로 생성하십시오.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // 1. 시스템 관리자 유저가 없는 경우에만 생성 (기동 확인용)
        if (userRepository.count() == 0) {
            userRepository.save(new User("admin", UserTier.VIP, UserRole.ADMIN));
            log.info(">>>> Initial data created: Admin user registered.");
        }

        // 2. 추가적인 초기 데이터가 필요한 경우 아래에 구현하십시오.
        log.info(">>>> System is ready. Use APIs for further data setup.");
    }
}
