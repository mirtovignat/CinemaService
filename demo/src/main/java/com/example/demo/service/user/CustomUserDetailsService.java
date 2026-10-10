package com.example.demo.service.user;

import com.example.demo.dto.user.AppUserDetails;
import com.example.demo.dto.user.SessionUser;
import com.example.demo.model.entity.User;
import com.example.demo.repository.entity.UserRepository;
import com.example.demo.util.PhoneNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(login)
                .or(() -> userRepository.findByEmail(login))
                .or(() -> {
                    String normalizedPhone = PhoneNormalizer.normalizePhone(login);
                    return normalizedPhone != null
                            ? userRepository.findByPhone(normalizedPhone)
                            : Optional.empty();
                })
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден: " + login));
        return new AppUserDetails(SessionUser.from(user), user.getPasswordHash());
    }
}