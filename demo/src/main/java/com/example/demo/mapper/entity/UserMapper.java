package com.example.demo.mapper.entity;

import com.example.demo.dto.authorize.RegisterFormDTO;
import com.example.demo.dto.user.UserForOwnerViewDTO;
import com.example.demo.dto.user.UserSidebarViewDTO;
import com.example.demo.dto.wallet.WalletForOwnerViewDTO;
import com.example.demo.mapper.util.AgeMapper;
import com.example.demo.mapper.util.NameMapper;
import com.example.demo.model.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;

@Mapper(componentModel = "spring", uses = {AgeMapper.class, NameMapper.class})
public interface UserMapper {

    @Mapping(target = "surname",    source = "fullName", qualifiedByName = "extractSurname")
    @Mapping(target = "name",       source = "fullName", qualifiedByName = "extractName")
    @Mapping(target = "patronymic", source = "fullName", qualifiedByName = "extractPatronymic")
    @Mapping(target = "age",        source = "birthDate", qualifiedByName = "calculateAge")
    UserForOwnerViewDTO toOwnerView(User user);

    @Mapping(target = "surname",    source = "fullName", qualifiedByName = "extractSurname")
    @Mapping(target = "name",       source = "fullName", qualifiedByName = "extractName")
    @Mapping(target = "patronymic", source = "fullName", qualifiedByName = "extractPatronymic")
    UserSidebarViewDTO toSidebarView(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deletableState", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "fullName", source = ".", qualifiedByName = "fullNameFromRegisterForm")
    @Mapping(target = "balance", constant = "0")
    @Mapping(target = "purchases", ignore = true)
    @Mapping(target = "favourites", ignore = true)
    @Mapping(target = "ratings", ignore = true)
    @Mapping(target = "cartItems", ignore = true)
    @Mapping(target = "saveHistory", ignore = true)
    @Mapping(target = "sex", expression = "java(Boolean.TRUE.equals(registerFormDTO.sex()))")
    User fromRegisterForm(RegisterFormDTO registerFormDTO);

    @Mapping(target = "maxBalance", source = "maxBalance")
    WalletForOwnerViewDTO toWalletView(User user, BigDecimal maxBalance);
}