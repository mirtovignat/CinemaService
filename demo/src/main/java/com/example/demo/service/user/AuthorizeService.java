package com.example.demo.service.user;

import com.example.demo.dto.authorize.Identifier;
import com.example.demo.dto.authorize.LoginFormDTO;
import com.example.demo.dto.authorize.RegisterFormDTO;
import com.example.demo.dto.user.SessionUser;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.mapper.entity.UserMapper;
import com.example.demo.model.base.DeletableState;
import com.example.demo.model.entity.User;
import com.example.demo.repository.entity.UserRepository;
import com.example.demo.util.PhoneNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AuthorizeService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public SessionUser login(LoginFormDTO loginFormDTO) {
        User user = validateAndGetUser(loginFormDTO);
        return SessionUser.from(user);
    }

    private User validateAndGetUser(LoginFormDTO loginFormDTO) {
        Identifier identifier = loginFormDTO.identifier();
        String identifierValue = loginFormDTO.identifierValue();
        User user = switch (identifier) {
            case USERNAME -> userRepository.findByUsername(identifierValue)
                    .orElseThrow(() -> BusinessException.of(ErrorCode.USER_NOT_FOUND, identifierValue));
            case EMAIL -> userRepository.findByEmail(identifierValue)
                    .orElseThrow(() -> BusinessException.of(ErrorCode.USER_NOT_FOUND, identifierValue));
            case PHONE -> userRepository.findByPhone(PhoneNormalizer.normalizePhone(identifierValue))
                    .orElseThrow(() -> BusinessException.of(ErrorCode.USER_NOT_FOUND, identifierValue));
        };
        if (!passwordEncoder.matches(loginFormDTO.rawPassword(), user.getPasswordHash())) {
            throw BusinessException.of(ErrorCode.PASSWORD_INVALID);
        }
        return user;
    }

    public void validateRegister(RegisterFormDTO registerFormDTO) {
        registerFormDTO.ifMismatch();
        registerFormDTO.validateAge();
        if (userRepository.existsByUsername(registerFormDTO.username())) {
            throw BusinessException.of(ErrorCode.ALREADY_REGISTERED);
        }
        if (userRepository.existsByEmail(registerFormDTO.email())) {
            throw BusinessException.of(ErrorCode.ALREADY_REGISTERED);
        }
        String phone = registerFormDTO.phone();
        if (phone != null) {
            phone = phone.trim();
            if (!phone.isEmpty() && userRepository.existsByPhone(phone)) {
                throw BusinessException.of(ErrorCode.ALREADY_REGISTERED);
            }
        }
    }

    @Transactional
    public SessionUser register(RegisterFormDTO registerFormDTO) {
        User user = userMapper.fromRegisterForm(registerFormDTO);
        user.setBalance(BigDecimal.ZERO);
        user.setDeletableState(DeletableState.builder().build());
        userRepository.save(user);
        return SessionUser.from(user);
    }
}