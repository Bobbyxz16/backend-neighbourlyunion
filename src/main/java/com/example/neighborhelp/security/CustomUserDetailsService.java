package com.example.neighborhelp.security;

import com.example.neighborhelp.entity.User;
import com.example.neighborhelp.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom UserDetailsService implementation for Spring Security authentication.
 *
 * This service bridges the application's User entity with Spring Security's
 * authentication framework. It loads user details from the database during
 * the authentication process and converts them into Spring Security's
 * UserDetails format.
 *
 * @author NeighborHelp Team
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        // Ensure required fields are not null
        String username = user.getEmail();
        if (username == null || username.isEmpty()) {
            throw new UsernameNotFoundException("User email is empty");
        }

        // Handle null password for OAuth users
        String password = user.getPassword() != null ? user.getPassword() : "";

        // Ensure user is enabled
        boolean enabled = user.getEnabled() != null ? user.getEnabled() : true;

        // Set default values for account status
        boolean accountNonExpired = true;
        boolean credentialsNonExpired = true;
        boolean accountNonLocked = true;

        // Handle roles - ensure at least one role
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (user.getRole() != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        } else {
            // Default role if none specified
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }

        return new org.springframework.security.core.userdetails.User(
                username,
                password,
                enabled,
                accountNonExpired,
                credentialsNonExpired,
                accountNonLocked,
                authorities
        );
    }
}