package com.example.demo.mapper.entity;

import com.example.demo.dto.authorize.RegisterFormDTO;
import com.example.demo.dto.user.UserForOwnerViewDTO;
import com.example.demo.dto.user.UserSidebarViewDTO;
import com.example.demo.dto.wallet.WalletForOwnerViewDTO;
import com.example.demo.mapper.util.AgeMapper;
import com.example.demo.mapper.util.NameMapper;
import com.example.demo.model.entity.User;
import com.example.demo.util.normalizer.PhoneNormalizer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring",
        uses = {NameMapper.class, AgeMapper.class},
        imports = {NameMapper.class, AgeMapper.class})
public abstract class UserMapper {

    @Autowired
    protected PhoneNormalizer phoneNormalizer;

    @Mapping(target = "surname",    expression = "java(NameMapper.extractSurname(user.getFullName()))")
    @Mapping(target = "name",       expression = "java(NameMapper.extractName(user.getFullName()))")
    @Mapping(target = "patronymic", expression = "java(NameMapper.extractPatronymic(user.getFullName()))")
    @Mapping(target = "age",        expression = "java(AgeMapper.calculateAge(user.getBirthDate()))")
    public abstract UserForOwnerViewDTO toOwnerView(User user);

    @Mapping(target = "surname",    expression = "java(NameMapper.extractSurname(user.getFullName()))")
    @Mapping(target = "name",       expression = "java(NameMapper.extractName(user.getFullName()))")
    @Mapping(target = "patronymic", expression = "java(NameMapper.extractPatronymic(user.getFullName()))")
    public abstract UserSidebarViewDTO toSidebarView(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deletableState", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "fullName", expression = "java(NameMapper.toFullName(registerFormDTO.surname(), registerFormDTO.name(), registerFormDTO.patronymic()))")
    @Mapping(target = "phone", expression = "java(phoneNormalizer.normalize(registerFormDTO.phone()))")
    @Mapping(target = "balance", ignore = true)
    @Mapping(target = "purchases", ignore = true)
    @Mapping(target = "favourites", ignore = true)
    @Mapping(target = "ratings", ignore = true)
    @Mapping(target = "cartItems", ignore = true)
    @Mapping(target = "saveHistory", ignore = true)
    public abstract User fromRegisterForm(RegisterFormDTO registerFormDTO);

    public abstract WalletForOwnerViewDTO toWalletView(User user);
}