package com.example.demo.model.entity;

import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.model.base.DeletableState;
import com.example.demo.model.base.IdentifiableEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

@SuperBuilder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User extends IdentifiableEntity {

    @Embedded
    @Builder.Default
    private DeletableState deletableState = DeletableState.builder().build();

    @Builder.Default
    @Column(name = "sex", nullable = false)
    private boolean sex = true;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "phone", unique = true)
    private String phone;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Builder.Default
    @Column(name = "balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "currency_code", nullable = false)
    private String currencyCode = "RUB";

    @Builder.Default
    @Column(name = "save_history", nullable = false)
    private boolean saveHistory = true;

    @ToString.Exclude
    @Builder.Default
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PurchasedMovie> purchases = new LinkedHashSet<>();

    @ToString.Exclude
    @Builder.Default
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<CartItem> cartItems = new LinkedHashSet<>();

    @ToString.Exclude
    @Builder.Default
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<FavouriteMovie> favourites = new LinkedHashSet<>();

    @ToString.Exclude
    @Builder.Default
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<RatedMovie> ratings = new LinkedHashSet<>();

    public void addMoney(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Сумма пополнения должна быть больше нуля");
        }
        this.balance = this.balance.add(amount);
    }

    public void spendMoney(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Сумма списания должна быть больше нуля");
        }
        if (this.balance.compareTo(amount) < 0) {
            throw BusinessException.of(ErrorCode.INSUFFICIENT_FUNDS);
        }
        this.balance = this.balance.subtract(amount);
    }
}