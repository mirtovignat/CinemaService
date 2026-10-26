package com.example.demo.service.user;

import com.example.demo.dto.movie.MovieCardViewDTO;
import com.example.demo.dto.response.PersonalAccountResponse;
import com.example.demo.dto.user.UserForOwnerViewDTO;
import com.example.demo.dto.wallet.WalletForOwnerViewDTO;
import com.example.demo.mapper.entity.UserMapper;
import com.example.demo.model.entity.User;
import com.example.demo.repository.entity.UserRepository;
import com.example.demo.service.catalog.RatedQueryService;
import com.example.demo.service.movie.MovieFilterMetaService;
import com.example.demo.service.movie.MovieRecommendationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class UserQueryService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final RatedQueryService ratedQueryService;
    private final MovieRecommendationService movieRecommendationService;
    private final MovieFilterMetaService movieFilterMetaService;
    private final BigDecimal maxBalance;

    public UserQueryService(UserRepository userRepository,
                            UserMapper userMapper,
                            RatedQueryService ratedQueryService,
                            MovieRecommendationService movieRecommendationService,
                            MovieFilterMetaService movieFilterMetaService,
                            @Value("${app.wallet.max-balance:100000.00}") BigDecimal maxBalance) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.ratedQueryService = ratedQueryService;
        this.movieRecommendationService = movieRecommendationService;
        this.movieFilterMetaService = movieFilterMetaService;
        this.maxBalance = maxBalance;
    }

    public PersonalAccountResponse buildUserPage(User user) {
        Long userId = user.getId();

        Slice<MovieCardViewDTO> firstPage =
                movieRecommendationService.getFirstRecommendationsPage(userId);

        return PersonalAccountResponse.builder()
                .userForOwnerViewDTO(userMapper.toOwnerView(user))
                .userSidebarViewDTO(userMapper.toSidebarView(user))
                .topThreeRatedMovies(ratedQueryService.getTopNRatedMovies(userId))
                .recommendedMovies(firstPage.getContent())
                .genres(movieFilterMetaService.getAllGenres())
                .build();
    }

    public BigDecimal getBalance(Long userId) {
        return getUser(userId).getBalance();
    }

    public User getUser(Long userId) {
        return userRepository.findByIdOrThrow(userId);
    }

    public String getUsername(Long userId) {
        return getUser(userId).getUsername();
    }

    public WalletForOwnerViewDTO getWalletForOwner(Long userId) {
        return userMapper.toWalletView(getUser(userId), maxBalance);
    }

    public UserForOwnerViewDTO getUserForOwner(Long userId) {
        return userMapper.toOwnerView(getUser(userId));
    }
}