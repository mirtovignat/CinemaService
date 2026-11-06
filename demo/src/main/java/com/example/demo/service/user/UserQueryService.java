package com.example.demo.service.user;

import com.example.demo.config.properties.WalletProperties;
import com.example.demo.dto.response.PersonalAccountResponse;
import com.example.demo.dto.user.UserForOwnerViewDTO;
import com.example.demo.dto.wallet.WalletForOwnerViewDTO;
import com.example.demo.mapper.entity.UserMapper;
import com.example.demo.model.entity.User;
import com.example.demo.repository.entity.UserRepository;
import com.example.demo.service.catalog.RatedQueryService;
import com.example.demo.service.movie.MovieFilterMetaService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserQueryService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final RatedQueryService ratedQueryService;
    private final MovieFilterMetaService movieFilterMetaService;
    private final WalletProperties walletProperties;

    public PersonalAccountResponse buildUserPage(User user) {
        Long userId = user.getId();

        return PersonalAccountResponse.builder()
                .userForOwnerViewDTO(userMapper.toOwnerView(user))
                .userSidebarViewDTO(userMapper.toSidebarView(user))
                .topRatedMovies(ratedQueryService.getTopNRatedMovies(userId))
                .genres(movieFilterMetaService.getAllGenres())
                .build();
    }

    public User getUser(Long userId) {
        return userRepository.findByIdOrThrow(userId);
    }

    public WalletForOwnerViewDTO getWalletForOwner(Long userId) {
        return userMapper.toWalletView(getUser(userId), walletProperties.maxBalance());
    }

    public UserForOwnerViewDTO getUserForOwner(Long userId) {
        return userMapper.toOwnerView(getUser(userId));
    }
}