package com.example.demo.service.user;

import com.example.demo.dto.user.AppUserDetails;
import com.example.demo.dto.user.SessionUser;
import com.example.demo.model.entity.User;
import com.example.demo.repository.entity.UserRepository;
import com.example.demo.util.PhoneNormalizer;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@AllArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PhoneNormalizer phoneNormalizer;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(login)
                .or(() -> userRepository.findByEmail(login))
                .or(() -> {
                    String normalizedPhone = phoneNormalizer.normalize(login);
                    return normalizedPhone != null
                            ? userRepository.findByPhone(normalizedPhone)
                            : Optional.empty();
                })
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден: " + login));
        return AppUserDetails.builder()
                .sessionUser(SessionUser.from(user))
                .passwordHash(user.getPasswordHash())
                .build();
    }
}