package com.smart_parking_system.backend.controller;

import com.smart_parking_system.backend.dto.LoginRequestDto;
import com.smart_parking_system.backend.dto.LoginResponseDto;
import com.smart_parking_system.backend.dto.RegisterRequestDto;
import com.smart_parking_system.backend.dto.RegisterResponseDto;
import com.smart_parking_system.backend.dto.UserDto;
import com.smart_parking_system.backend.entity.User;
import com.smart_parking_system.backend.repository.UserRepository;
import com.smart_parking_system.backend.service.IMqttBrokerService;
import com.smart_parking_system.backend.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.password.CompromisedPasswordChecker;
import org.springframework.security.authentication.password.CompromisedPasswordDecision;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final CompromisedPasswordChecker compromisedPasswordChecker;
    private final IMqttBrokerService mqttBrokerService;

    @Value("${mqtt.broker-uri}")
    private String mqttBrokerUri;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> apiLogin(@RequestBody LoginRequestDto loginRequestDto) {

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequestDto.email(),
                            loginRequestDto.password()));

            var loggedUser = (User) authentication.getPrincipal();

            if (Boolean.FALSE.equals(loggedUser.getEnabled())) {
                return buildErrorResponse(HttpStatus.FORBIDDEN, "Account is disabled");
            }

            var userDto = new UserDto();
            BeanUtils.copyProperties(loggedUser, userDto);
            userDto.setId(loggedUser.getId());
            userDto.setRole(loggedUser.getRole());
            userDto.setCreatedAt(loggedUser.getCreatedAt());
            String jwtToken = jwtUtil.generateJwtToken(authentication);

            return ResponseEntity.status(HttpStatus.OK)
                    .body(new LoginResponseDto(HttpStatus.OK.getReasonPhrase(), userDto, jwtToken));
        } catch (BadCredentialsException e) {
            return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Invalid username and password");
        } catch (AuthenticationException e) {
            return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Authentication failed");
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error has occurred");
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequestDto registerRequestDto) {
        Optional<User> existingUser = userRepository.findUserByEmail(registerRequestDto.getEmail());
        if (existingUser.isPresent()) {
            Map<String, String> errors = new HashMap<>();
            User user = existingUser.get();

            if (user.getEmail().equalsIgnoreCase(registerRequestDto.getEmail())) {
                errors.put("email", "Email is already registered");
            }

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
        }

        CompromisedPasswordDecision decision = compromisedPasswordChecker.check(registerRequestDto.getPassword());
        if (decision.isCompromised()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("password", "Choose a strong password"));
        }

        User user = new User();
        BeanUtils.copyProperties(registerRequestDto, user);
        user.setPasswordHash(passwordEncoder.encode(registerRequestDto.getPassword()));
        user.setRole("ROLE_USER");
        user.setEnabled(true);
        user.setCreatedAt(Instant.now());
        userRepository.save(user);
        userRepository.flush();

        String mqttUsername = user.getEmail();
        String mqttPassword = registerRequestDto.getPassword();

        try {
            mqttBrokerService.createBrokerUser(mqttUsername, mqttPassword);

            mqttBrokerService.setUserAcl(mqttUsername, user.getId());

            user.setMqttUsername(mqttUsername);
            user.setMqttPasswordHash(passwordEncoder.encode(mqttPassword));
            userRepository.save(user);

            RegisterResponseDto response = new RegisterResponseDto(
                    "Register successfully",
                    mqttUsername,
                    mqttPassword,
                    mqttBrokerUri);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            userRepository.delete(user);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create MQTT broker account"));
        }
    }

    private ResponseEntity<LoginResponseDto> buildErrorResponse(HttpStatus status, String message) {
        return ResponseEntity
                .status(status)
                .body(new LoginResponseDto(message, null, null));
    }
}
