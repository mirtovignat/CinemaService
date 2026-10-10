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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AuthorizeService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

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
        if (phone != null && !phone.trim().isEmpty()) {
            String normalizedPhone = PhoneNormalizer.normalizePhone(phone);
            if (normalizedPhone == null) {
                throw BusinessException.of(ErrorCode.PHONE_INVALID);
            }
            if (userRepository.existsByPhone(normalizedPhone)) {
                throw BusinessException.of(ErrorCode.ALREADY_REGISTERED);
            }
        }
    }

    @Transactional
    public SessionUser register(RegisterFormDTO registerFormDTO) {
        User user = userMapper.fromRegisterForm(registerFormDTO);
        user.setPhone(PhoneNormalizer.normalizePhone(registerFormDTO.phone()));
        user.setBalance(BigDecimal.ZERO);
        user.setDeletableState(DeletableState.builder().build());
        user = userRepository.saveAndFlush(user);
        return SessionUser.from(user);
    }
}