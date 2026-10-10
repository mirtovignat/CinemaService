package com.example.demo.mapper.entity;

import com.example.demo.dto.authorize.RegisterFormDTO;
import com.example.demo.dto.user.UserForOwnerViewDTO;
import com.example.demo.dto.wallet.WalletForOwnerViewDTO;
import com.example.demo.mapper.util.MapperUtils;
import com.example.demo.model.entity.User;
import com.example.demo.util.PhoneNormalizer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring",
        uses = MapperUtils.class,
        imports = {PhoneNormalizer.class})
public interface UserMapper {

    @Mapping(target = "surname", expression = "java(MapperUtils.extractSurname(user.getFullName()))")
    @Mapping(target = "name", expression = "java(MapperUtils.extractName(user.getFullName()))")
    @Mapping(target = "patronymic", expression = "java(MapperUtils.extractPatronymic(user.getFullName()))")
    @Mapping(target = "age", expression = "java(MapperUtils.calculateAge(user.getBirthDate()))")
    UserForOwnerViewDTO toOwnerView(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deletableState", ignore = true)
    @Mapping(target = "passwordHash", source = "rawPassword", qualifiedByName = "rawToEncoded")
    @Mapping(target = "fullName", expression = "java(MapperUtils.toFullName(registerFormDTO.surname(), registerFormDTO.name(), registerFormDTO.patronymic()))")
    @Mapping(target = "phone", expression = "java(PhoneNormalizer.normalizePhone(registerFormDTO.phone()))")
    @Mapping(target = "balance", ignore = true)
    @Mapping(target = "purchases", ignore = true)
    @Mapping(target = "favourites", ignore = true)
    @Mapping(target = "ratings", ignore = true)
    @Mapping(target = "cartItems", ignore = true)
    User fromRegisterForm(RegisterFormDTO registerFormDTO);

    WalletForOwnerViewDTO toWalletView(User user);
}