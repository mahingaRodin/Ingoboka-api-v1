package com.ingoboka_api.v1.common.security;

import com.ingoboka_api.v1.common.util.PhoneNumberUtils;
import com.ingoboka_api.v1.identity.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IngobokaUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) {
        String trimmed = username.trim();
        if (PhoneNumberUtils.looksLikeEmail(trimmed)) {
            return userRepository
                    .findByEmailIgnoreCase(trimmed.toLowerCase())
                    .map(IngobokaUserDetails::new)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        }

        String normalizedPhone = PhoneNumberUtils.normalizeRwanda(trimmed);
        return userRepository
                .findByPhoneNumber(normalizedPhone)
                .or(() -> userRepository.findByPhoneNumber(trimmed))
                .map(IngobokaUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
