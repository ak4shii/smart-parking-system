package com.smart_parking_system.backend.controller;

import com.smart_parking_system.backend.dto.LoginRequestDto;
import com.smart_parking_system.backend.dto.LoginResponseDto;
import com.smart_parking_system.backend.dto.RegisterRequestDto;
import com.smart_parking_system.backend.dto.UserDto;
import com.smart_parking_system.backend.entity.User;
import com.smart_parking_system.backend.repository.UserRepository;
import com.smart_parking_system.backend.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
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

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> apiLogin(@RequestBody LoginRequestDto loginRequestDto) {

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequestDto.email(),
                            loginRequestDto.password()));

            var userDto = new UserDto();
            var loggedUser = (User) authentication.getPrincipal();
            BeanUtils.copyProperties(loggedUser, userDto);
            userDto.setId(loggedUser.getId());
            userDto.setRole(loggedUser.getRole());
            userDto.setCreatedAt(loggedUser.getCreatedAt());
            String jwtToken = jwtUtil.generateJwtToken(authentication);

            System.out.println(jwtToken);
            System.out.println(userDto);

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
        return ResponseEntity.status(HttpStatus.CREATED).body("Register successfully");
    }

    private ResponseEntity<LoginResponseDto> buildErrorResponse(HttpStatus status, String message) {
        return ResponseEntity
                .status(status)
                .body(new LoginResponseDto(message, null, null));
    }
}
