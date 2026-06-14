package com.internship.splitwise.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.internship.splitwise.dto.LoginRequest;
import com.internship.splitwise.dto.UserRegisterRequest;
import com.internship.splitwise.model.User;
import com.internship.splitwise.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testRegisterSuccess() throws Exception {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setName("Aisha");
        request.setEmail("aisha@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("aisha@example.com")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    public void testLoginSuccessExistingUser() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("aisha@example.com");
        request.setPassword("password123");

        User user = User.builder()
                .id(UUID.randomUUID())
                .name("Aisha")
                .email("aisha@example.com")
                .password("password123")
                .build();

        when(userRepository.findByEmail("aisha@example.com")).thenReturn(Optional.of(user));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock-jwt-token-for-development"))
                .andExpect(jsonPath("$.user.email").value("aisha@example.com"));
    }
}
