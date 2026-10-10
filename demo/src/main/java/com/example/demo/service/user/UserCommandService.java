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
import com.example.demo.util.PhoneNormalizer;
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

    public ProfileFieldUpdateDTO prepareAndValidateField(ProfileFieldUpdateDTO profileFieldUpdateDTO,
                                                         Long userId) {
        User user = userQueryService.getUser(userId);
        String field = profileFieldUpdateDTO.field();
        Identifier identifier = profileFieldUpdateDTO.identifier();

        switch (identifier) {
            case USERNAME -> {
                if (field.equals(user.getUsername())) {
                    throw BusinessException.of(ErrorCode.DATA_COINCIDENCE);
                }
                if (userRepository.existsByUsername(field)) {
                    throw BusinessException.of(ErrorCode.ALREADY_TAKEN);
                }
                return profileFieldUpdateDTO;
            }
            case EMAIL -> {
                if (field.equals(user.getEmail())) {
                    throw BusinessException.of(ErrorCode.DATA_COINCIDENCE);
                }
                if (userRepository.existsByEmail(field)) {
                    throw BusinessException.of(ErrorCode.ALREADY_TAKEN);
                }
                return profileFieldUpdateDTO;
            }
            case PHONE -> {
                String trimmedField = field != null ? field.trim() : "";
                String currentPhone = user.getPhone() != null ? user.getPhone() : "";

                if (trimmedField.isEmpty()) {
                    if (currentPhone.isEmpty()) {
                        throw BusinessException.of(ErrorCode.DATA_COINCIDENCE);
                    }
                    return ProfileFieldUpdateDTO.builder()
                            .identifier(identifier)
                            .build();
                }

                String normalized = PhoneNormalizer.normalizePhone(trimmedField);
                if (normalized == null) {
                    throw BusinessException.of(ErrorCode.PHONE_INVALID);
                }
                if (normalized.equals(currentPhone)) {
                    throw BusinessException.of(ErrorCode.DATA_COINCIDENCE);
                }
                if (userRepository.existsByPhone(normalized)) {
                    throw BusinessException.of(ErrorCode.ALREADY_TAKEN);
                }
                return ProfileFieldUpdateDTO.builder()
                        .identifier(identifier)
                        .field(normalized)
                        .build();
            }
            default -> throw BusinessException.of(ErrorCode.VALIDATION_ERROR);
        }
    }

    @Transactional
    public void updateField(Long userId, ProfileFieldUpdateDTO profileFieldUpdateDTO) {
        switch (profileFieldUpdateDTO.identifier()) {
            case USERNAME -> userRepository.updateUsername(userId, profileFieldUpdateDTO.field());
            case EMAIL -> userRepository.updateEmail(userId, profileFieldUpdateDTO.field());
            case PHONE -> {
                String value = profileFieldUpdateDTO.field();
                if (value == null || value.isBlank()) {
                    userRepository.deletePhone(userId);
                } else {
                    userRepository.updatePhone(userId, value);
                }
            }
            default -> throw BusinessException.of(ErrorCode.VALIDATION_ERROR);
        }
    }

    public String prepareAndValidatePassword(PasswordChangingDTO passwordChangingDTO, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.of(ErrorCode.USER_NOT_FOUND, userId));
        if (!passwordEncoder.matches(passwordChangingDTO.currentPassword(), user.getPasswordHash())) {
            throw BusinessException.of(ErrorCode.PASSWORD_INVALID);
        }
        passwordChangingDTO.isMismatch();
        passwordChangingDTO.isCoincidence();
        return passwordEncoder.encode(passwordChangingDTO.rawPassword());
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
}