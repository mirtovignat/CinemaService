package com.example.demo.service.user;

import com.example.demo.dto.authorize.Identifier;
import com.example.demo.dto.user.PasswordChangingDTO;
import com.example.demo.dto.user.ProfileFieldUpdateDTO;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.model.entity.User;
import com.example.demo.repository.entity.CartItemRepository;
import com.example.demo.repository.entity.FavouriteMovieRepository;
import com.example.demo.repository.entity.UserRepository;
import com.example.demo.util.UserSuffixGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class UserCommandService {

    private final UserRepository userRepository;
    private final UserQueryService userQueryService;
    private final PasswordEncoder passwordEncoder;
    private final CartItemRepository cartItemRepository;
    private final FavouriteMovieRepository favouriteMovieRepository;
    private final UserSuffixGenerator userSuffixGenerator;

    public ProfileFieldUpdateDTO prepareAndValidateField(ProfileFieldUpdateDTO dto, Long userId) {
        User user = userQueryService.getUser(userId);
        String field = dto.field();
        Identifier identifier = dto.identifier();

        switch (identifier) {
            case USERNAME -> {
                if (field.equals(user.getUsername())) {
                    throw BusinessException.of(ErrorCode.DATA_COINCIDENCE);
                }
                if (userRepository.existsByUsername(field)) {
                    throw BusinessException.of(ErrorCode.ALREADY_TAKEN);
                }
            }
            case EMAIL -> {
                if (field.equals(user.getEmail())) {
                    throw BusinessException.of(ErrorCode.DATA_COINCIDENCE);
                }
                if (userRepository.existsByEmail(field)) {
                    throw BusinessException.of(ErrorCode.ALREADY_TAKEN);
                }
            }
            case PHONE -> {
                String currentPhone = user.getPhone() != null ? user.getPhone() : "";
                if (field.equals(currentPhone)) {
                    throw BusinessException.of(ErrorCode.DATA_COINCIDENCE);
                }
                if (userRepository.existsByPhone(field)) {
                    throw BusinessException.of(ErrorCode.ALREADY_TAKEN);
                }
            }
            default -> throw BusinessException.of(ErrorCode.VALIDATION_ERROR, "Неизвестное поле: " + identifier);
        }
        return dto;
    }

    @Transactional
    public void updateField(Long userId, ProfileFieldUpdateDTO dto) {
        switch (dto.identifier()) {
            case USERNAME -> userRepository.updateUsername(userId, dto.field());
            case EMAIL -> userRepository.updateEmail(userId, dto.field());
            case PHONE -> userRepository.updatePhone(userId, dto.field());
            default -> throw BusinessException.of(ErrorCode.VALIDATION_ERROR, "Неизвестное поле: " + dto.identifier());
        }
    }

    public String prepareAndValidatePassword(PasswordChangingDTO passwordChangingDTO, Long userId) {
        User user = userQueryService.getUser(userId);
        passwordChangingDTO.isMismatch();
        passwordChangingDTO.isCoincidence();
        if (!passwordEncoder.matches(passwordChangingDTO.currentPassword(), user.getPasswordHash())) {
            throw BusinessException.of(ErrorCode.PASSWORD_INVALID);
        }
        return encodePassword(passwordChangingDTO.rawPassword());
    }

    private String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    @Transactional
    public void updatePassword(String encodedPassword, Long userId) {
        User user = userQueryService.getUser(userId);
        user.setPasswordHash(encodedPassword);
        userRepository.save(user);
    }

    @Transactional
    public void saveTopUp(BigDecimal amount, User user) {
        user.addMoney(amount);
        userRepository.save(user);
    }

    @Transactional
    public void deleteAccount(Long userId) {
        User user = userQueryService.getUser(userId);
        favouriteMovieRepository.deleteAllByUserId(userId);
        cartItemRepository.deleteAllByUserId(userId);
        String suffix = userSuffixGenerator.generate(user);
        userRepository.softDeleteById(userId, suffix);
    }

    @Transactional
    public void deletePhone(Long userId) {
        userRepository.deletePhone(userId);
    }

    @Transactional
    public void createUser(User user) {
        userRepository.save(user);
    }
}