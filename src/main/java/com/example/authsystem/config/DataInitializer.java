package com.example.authsystem.config;

import com.example.authsystem.entity.Resource;
import com.example.authsystem.entity.Role;
import com.example.authsystem.entity.User;
import com.example.authsystem.repository.ResourceRepository;
import com.example.authsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final ResourceRepository resourceRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        seedUsers();
        seedSampleResources();
    }

    private void seedUsers() {
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = User.builder()
                    .username("admin")
                    .email("admin@example.com")
                    .password(passwordEncoder.encode("Admin@123"))
                    .role(Role.ADMIN)
                    .build();
            userRepository.save(admin);
            log.info("Initialized test seed user: [admin] with role ADMIN");
        }

        if (userRepository.findByUsername("user").isEmpty()) {
            User user = User.builder()
                    .username("user")
                    .email("user@example.com")
                    .password(passwordEncoder.encode("User@123"))
                    .role(Role.USER)
                    .build();
            userRepository.save(user);
            log.info("Initialized test seed user: [user] with role USER");
        }
    }

    private void seedSampleResources() {
        if (resourceRepository.count() == 0) {
            Resource r1 = Resource.builder()
                    .name("Conference Room A")
                    .description("Executive conference room with 4K display and conference phone")
                    .type("CONFERENCE_ROOM")
                    .available(true)
                    .build();

            Resource r2 = Resource.builder()
                    .name("Projector 4K Pro")
                    .description("High-definition portable projector with HDMI and wireless casting")
                    .type("EQUIPMENT")
                    .available(true)
                    .build();

            Resource r3 = Resource.builder()
                    .name("Private Workstation Pod 101")
                    .description("Soundproof quiet booth with standing desk and dual monitors")
                    .type("WORKSPACE")
                    .available(true)
                    .build();

            resourceRepository.save(r1);
            resourceRepository.save(r2);
            resourceRepository.save(r3);
            log.info("Initialized sample resources for testing");
        }
    }
}
