package com.example.demo.service.user;

import com.example.demo.dto.movie.MovieCardViewDTO;
import com.example.demo.dto.response.PersonalAccountResponse;
import com.example.demo.dto.user.UserForOwnerViewDTO;
import com.example.demo.dto.wallet.WalletForOwnerViewDTO;
import com.example.demo.mapper.entity.UserMapper;
import com.example.demo.model.entity.User;
import com.example.demo.repository.entity.CartItemRepository;
import com.example.demo.repository.entity.FavouriteMovieRepository;
import com.example.demo.repository.entity.PurchasedMovieRepository;
import com.example.demo.repository.entity.UserRepository;
import com.example.demo.service.movie.MovieRecommendationService;
import com.example.demo.service.movie.MovieService;
import com.example.demo.service.rated.RatedQueryService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@AllArgsConstructor
public class UserQueryService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PurchasedMovieRepository purchasedMovieRepository;
    private final RatedQueryService ratedQueryService;
    private final FavouriteMovieRepository favouriteMovieRepository;
    private final CartItemRepository cartItemRepository;
    private final MovieRecommendationService movieRecommendationService;
    private final MovieService movieService;

    private static final int RECOMMENDATIONS_PAGE_SIZE = 8;

    @Transactional(readOnly = true)
    public PersonalAccountResponse buildUserPage(UserForOwnerViewDTO userForOwner) {
        Long userId = userForOwner.id();

        Slice<MovieCardViewDTO> firstPage = movieRecommendationService.getRecommendedMoviesSlice(
                userId,
                PageRequest.of(0, RECOMMENDATIONS_PAGE_SIZE)
        );

        return PersonalAccountResponse.builder()
                .userForOwner(userForOwner)
                .purchasesCount(purchasedMovieRepository.countByUserId(userId))
                .ratingsCount(ratedQueryService.countByUserId(userId))
                .favouritesCount(favouriteMovieRepository.countByUserId(userId))
                .cartCount(cartItemRepository.countByUserId(userId))
                .topThreeRatedMovies(ratedQueryService.getTopNRatedMovies(userId))
                .recommendedMovies(firstPage.getContent())
                .genres(movieService.getAllGenres())
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
        return userMapper.toWalletView(getUser(userId));
    }

    public UserForOwnerViewDTO getUserForOwner(Long userId) {
        return userMapper.toOwnerView(getUser(userId));
    }
}