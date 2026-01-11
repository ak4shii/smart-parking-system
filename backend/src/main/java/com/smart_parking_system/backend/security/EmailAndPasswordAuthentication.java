package com.smart_parking_system.backend.security;

import com.smart_parking_system.backend.entity.User;
import com.smart_parking_system.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class EmailAndPasswordAuthentication implements AuthenticationProvider {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String email = authentication.getName();
        String password = authentication.getCredentials().toString();
        User user = userRepository.findUserByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with email: " + email + ". Please register.")
                );

        if (Boolean.FALSE.equals(user.getEnabled())) {
            throw new DisabledException("User account is disabled");
        }

        String role = user.getRole();
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));
        if (passwordEncoder.matches(password, user.getPasswordHash())) {
            return new UsernamePasswordAuthenticationToken(user, null, authorities);
        } else {
            throw new BadCredentialsException("Invalid password.");
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
