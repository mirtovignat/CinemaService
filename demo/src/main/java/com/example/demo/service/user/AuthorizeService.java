package com.example.demo.service.user;

import com.example.demo.dto.authorize.RegisterFormDTO;
import com.example.demo.dto.user.SessionUser;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.mapper.entity.UserMapper;
import com.example.demo.model.base.DeletableState;
import com.example.demo.model.entity.User;
import com.example.demo.repository.entity.UserRepository;
import com.example.demo.util.PhoneNormalizer;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@AllArgsConstructor
public class AuthorizeService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final PhoneNormalizer phoneNormalizer;

    public void validateRegister(RegisterFormDTO registerFormDTO) {
        registerFormDTO.ifMismatch();
        registerFormDTO.validateAge();

        String phone = registerFormDTO.phone();
        String normalizedPhone = null;
        if (phone != null && !phone.trim().isEmpty()) {
            normalizedPhone = phoneNormalizer.normalize(phone);
            if (normalizedPhone == null) {
                throw BusinessException.of(ErrorCode.PHONE_INVALID);
            }
        }
        validateUniqueness(registerFormDTO.username(), registerFormDTO.email(), normalizedPhone);
    }

    public void validateUniqueness(String username, String email, String normalizedPhone) {
        if (userRepository.existsByUsername(username)) {
            throw BusinessException.of(ErrorCode.ALREADY_REGISTERED);
        }
        if (userRepository.existsByEmail(email)) {
            throw BusinessException.of(ErrorCode.ALREADY_REGISTERED);
        }
        if (normalizedPhone != null && userRepository.existsByPhone(normalizedPhone)) {
            throw BusinessException.of(ErrorCode.ALREADY_REGISTERED);
        }
    }

    public SessionUser register(RegisterFormDTO registerFormDTO) {
        User preparedUser = prepareUser(registerFormDTO);
        User savedUser = userRepository.saveAndFlush(preparedUser);
        return SessionUser.from(savedUser);
    }

    private User prepareUser(RegisterFormDTO registerFormDTO) {
        User preparedUser = userMapper.fromRegisterForm(registerFormDTO);
        preparedUser.setPasswordHash(passwordEncoder.encode(registerFormDTO.rawPassword()));
        preparedUser.setPhone(phoneNormalizer.normalize(registerFormDTO.phone()));
        preparedUser.setBalance(BigDecimal.ZERO);
        preparedUser.setDeletableState(DeletableState.builder().build());
        return preparedUser;
    }
}