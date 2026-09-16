package com.example.demo.model.base;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Builder
@Embeddable
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class DeletableState {
    @Builder.Default
    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;
}