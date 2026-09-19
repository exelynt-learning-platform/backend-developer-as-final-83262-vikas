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
import org.springframework.dao.DataIntegrityViolationException;
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
        seedUserIfAbsent("admin", "admin@example.com", "Admin@123", Role.ADMIN);
        seedUserIfAbsent("user", "user@example.com", "User@123", Role.USER);
        seedSampleResources();
    }

    private void seedUserIfAbsent(String username, String email, String rawPassword, Role role) {
        try {
            boolean userExists = userRepository.existsByUsername(username) || userRepository.existsByEmail(email);
            if (!userExists) {
                User user = User.builder()
                        .username(username)
                        .email(email)
                        .password(passwordEncoder.encode(rawPassword))
                        .role(role)
                        .build();
                userRepository.save(user);
                log.info("Initialized test seed user: [{}] with role {}", username, role);
            }
        } catch (DataIntegrityViolationException e) {
            log.warn("User [{}] or email [{}] was already created concurrently: {}", username, email, e.getMessage());
        }
    }

    private void seedSampleResources() {
        seedResourceIfAbsent("Conference Room A",
                "Executive conference room with 4K display and conference phone",
                "CONFERENCE_ROOM", true);
        seedResourceIfAbsent("Projector 4K Pro",
                "High-definition portable projector with HDMI and wireless casting",
                "EQUIPMENT", true);
        seedResourceIfAbsent("Private Workstation Pod 101",
                "Soundproof quiet booth with standing desk and dual monitors",
                "WORKSPACE", true);
    }

    private void seedResourceIfAbsent(String name, String description, String type, boolean available) {
        try {
            if (!resourceRepository.existsByNameIgnoreCase(name)) {
                Resource resource = Resource.builder()
                        .name(name)
                        .description(description)
                        .type(type)
                        .available(available)
                        .build();
                resourceRepository.save(resource);
                log.info("Initialized sample resource: [{}]", name);
            }
        } catch (DataIntegrityViolationException e) {
            log.warn("Resource [{}] was already created concurrently: {}", name, e.getMessage());
        }
    }
}
