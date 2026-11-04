package com.example.demo.service.user;

import com.example.demo.dto.authorize.Identifier;
import com.example.demo.dto.user.PasswordChangingDTO;
import com.example.demo.dto.user.ProfileFieldUpdateDTO;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.model.entity.User;
import com.example.demo.repository.base.WatchHistoryItemRepository;
import com.example.demo.repository.entity.CartItemRepository;
import com.example.demo.repository.entity.FavouriteMovieRepository;
import com.example.demo.repository.entity.PurchasedMovieRepository;
import com.example.demo.repository.entity.UserRepository;
import com.example.demo.util.PhoneNormalizer;
import com.example.demo.util.text.StringSanitizer;
import com.example.demo.util.validation.Amounts;
import com.example.demo.util.validation.Emptiness;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class UserCommandService {

    private final UserQueryService userQueryService;
    private final UserRepository userRepository;
    private final CartItemRepository cartItemRepository;
    private final FavouriteMovieRepository favouriteMovieRepository;
    private final PurchasedMovieRepository purchasedMovieRepository;
    private final WatchHistoryItemRepository watchHistoryItemRepository;
    private final PasswordEncoder passwordEncoder;
    private final PhoneNormalizer phoneNormalizer;
    private final BigDecimal maxBalance;

    public UserCommandService(UserQueryService userQueryService,
                              UserRepository userRepository,
                              CartItemRepository cartItemRepository,
                              FavouriteMovieRepository favouriteMovieRepository,
                              PurchasedMovieRepository purchasedMovieRepository,
                              WatchHistoryItemRepository watchHistoryItemRepository,
                              PasswordEncoder passwordEncoder,
                              PhoneNormalizer phoneNormalizer,
                              @Value("${app.wallet.max-balance:100000.00}") BigDecimal maxBalance) {
        this.userQueryService = userQueryService;
        this.userRepository = userRepository;
        this.cartItemRepository = cartItemRepository;
        this.favouriteMovieRepository = favouriteMovieRepository;
        this.purchasedMovieRepository = purchasedMovieRepository;
        this.watchHistoryItemRepository = watchHistoryItemRepository;
        this.passwordEncoder = passwordEncoder;
        this.phoneNormalizer = phoneNormalizer;
        this.maxBalance = maxBalance;
    }

    public ProfileFieldUpdateDTO prepareAndValidateField(ProfileFieldUpdateDTO profileFieldUpdateDTO, Long userId) {
        User user = userQueryService.getUser(userId);
        String field = StringSanitizer.trimOrEmpty(profileFieldUpdateDTO.field());
        Identifier identifier = profileFieldUpdateDTO.identifier();

        switch (identifier) {
            case USERNAME -> {
                if (field.equals(user.getUsername())) {
                    throw BusinessException.of(ErrorCode.DATA_COINCIDENCE);
                }
                if (userRepository.existsByUsernameIgnoreCase(field)) {
                    throw BusinessException.of(ErrorCode.USERNAME_TAKEN);
                }
                return ProfileFieldUpdateDTO.builder().identifier(identifier).field(field).build();
            }
            case EMAIL -> {
                if (field.equals(user.getEmail())) {
                    throw BusinessException.of(ErrorCode.DATA_COINCIDENCE);
                }
                if (userRepository.existsByEmail(field)) {
                    throw BusinessException.of(ErrorCode.ALREADY_TAKEN);
                }
                return ProfileFieldUpdateDTO.builder().identifier(identifier).field(field).build();
            }
            case PHONE -> {
                String currentPhone = StringSanitizer.trimOrEmpty(user.getPhone());

                if (field.isEmpty()) {
                    if (currentPhone.isEmpty()) {
                        throw BusinessException.of(ErrorCode.DATA_COINCIDENCE);
                    }
                    return ProfileFieldUpdateDTO.builder().identifier(identifier).build();
                }

                String normalizedPhone = phoneNormalizer.normalize(field);
                if (normalizedPhone == null) {
                    throw BusinessException.of(ErrorCode.PHONE_INVALID);
                }
                if (normalizedPhone.equals(currentPhone)) {
                    throw BusinessException.of(ErrorCode.DATA_COINCIDENCE);
                }
                if (userRepository.existsByPhone(normalizedPhone)) {
                    throw BusinessException.of(ErrorCode.ALREADY_TAKEN);
                }
                return ProfileFieldUpdateDTO.builder().identifier(identifier).field(normalizedPhone).build();
            }
            default -> throw BusinessException.of(ErrorCode.VALIDATION_ERROR);
        }
    }

    @Transactional
    public void updateField(Long userId, ProfileFieldUpdateDTO profileFieldUpdateDTO) {
        try {
            switch (profileFieldUpdateDTO.identifier()) {
                case USERNAME -> userRepository.updateUsername(userId, profileFieldUpdateDTO.field());
                case EMAIL -> userRepository.updateEmail(userId, profileFieldUpdateDTO.field());
                case PHONE -> {
                    if (Emptiness.isBlank(profileFieldUpdateDTO.field())) {
                        userRepository.deletePhone(userId);
                    } else {
                        userRepository.updatePhone(userId, profileFieldUpdateDTO.field());
                    }
                }
                default -> throw BusinessException.of(ErrorCode.VALIDATION_ERROR);
            }
        } catch (DataIntegrityViolationException exception) {
            throw BusinessException.of(ErrorCode.ALREADY_TAKEN);
        }
    }

    public String prepareAndValidatePassword(PasswordChangingDTO passwordChangingDTO, Long userId) {
        User user = userQueryService.getUser(userId);
        String currentPasswordHash = user.getPasswordHash();
        validatePasswordChange(passwordChangingDTO, currentPasswordHash);
        return passwordEncoder.encode(passwordChangingDTO.rawPassword());
    }

    private void validatePasswordChange(PasswordChangingDTO passwordChangingDTO, String currentPasswordHash) {
        if (!passwordEncoder.matches(passwordChangingDTO.currentPassword(), currentPasswordHash)) {
            throw BusinessException.of(ErrorCode.PASSWORD_INVALID);
        }
        passwordChangingDTO.isMismatch();
    }

    @Transactional
    public void updatePassword(String encodedPassword, Long userId) {
        User user = userRepository.findByIdOrThrow(userId);
        user.setPasswordHash(encodedPassword);
        userRepository.save(user);
    }

    @Transactional
    public BigDecimal saveTopUp(BigDecimal amount, Long userId) {
        Amounts.requirePositive(amount);

        User lockedUser = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> BusinessException.of(ErrorCode.USER_NOT_FOUND, "id=" + userId));

        BigDecimal newBalance = lockedUser.getBalance().add(amount);
        if (newBalance.compareTo(maxBalance) > 0) {
            throw BusinessException.of(ErrorCode.VALIDATION_ERROR);
        }

        lockedUser.addMoney(amount);
        userRepository.save(lockedUser);
        return lockedUser.getBalance();
    }

    @Transactional
    public void deleteAccount(Long userId) {
        watchHistoryItemRepository.deleteAllByUserId(userId);
        favouriteMovieRepository.deleteAllByUserId(userId);
        cartItemRepository.deleteAllByUserId(userId);
        purchasedMovieRepository.deleteAllByUserId(userId);
        userRepository.softDeleteById(userId);
    }

    @Transactional
    public void deletePhone(Long userId) {
        userRepository.deletePhone(userId);
    }
}