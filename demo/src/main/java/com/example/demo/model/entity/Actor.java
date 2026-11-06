package com.example.demo.model.entity;

import com.example.demo.model.base.IdentifiableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "actors")
public class Actor extends IdentifiableEntity {

    @Column(name = "full_name", nullable = false, unique = true, columnDefinition = "text")
    private String fullName;
}