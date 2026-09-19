package com.example.authsystem.config;

import com.example.authsystem.entity.Resource;
import com.example.authsystem.entity.Role;
import com.example.authsystem.entity.User;
import com.example.authsystem.repository.ResourceRepository;
import com.example.authsystem.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private DataInitializer dataInitializer;

    @Test
    @DisplayName("Should seed users and resources when absent")
    void testRunWhenAbsent() {
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(resourceRepository.existsByNameIgnoreCase(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");

        assertDoesNotThrow(() -> dataInitializer.run());

        verify(userRepository, times(2)).save(any(User.class));
        verify(resourceRepository, times(3)).save(any(Resource.class));
    }

    @Test
    @DisplayName("Should skip seeding if records already exist")
    void testRunWhenAlreadyExists() {
        when(userRepository.existsByUsername(anyString())).thenReturn(true);
        when(resourceRepository.existsByNameIgnoreCase(anyString())).thenReturn(true);

        assertDoesNotThrow(() -> dataInitializer.run());

        verify(userRepository, never()).save(any(User.class));
        verify(resourceRepository, never()).save(any(Resource.class));
    }

    @Test
    @DisplayName("Should gracefully handle DataIntegrityViolationException on concurrent insert")
    void testHandleDataIntegrityViolationException() {
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenThrow(new DataIntegrityViolationException("Duplicate"));

        when(resourceRepository.existsByNameIgnoreCase(anyString())).thenReturn(false);
        when(resourceRepository.save(any(Resource.class))).thenThrow(new DataIntegrityViolationException("Duplicate"));

        assertDoesNotThrow(() -> dataInitializer.run());
    }
}
