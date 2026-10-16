// demo/src/main/java/com/example/demo/service/catalog/LibraryService.java
package com.example.demo.service.catalog;

import com.example.demo.dto.catalog.LibrarianMovieViewDTO;
import com.example.demo.mapper.entity.PurchasedMapper;
import com.example.demo.mapper.entity.UserMapper;
import com.example.demo.model.entity.PurchasedMovie;
import com.example.demo.repository.entity.PurchasedMovieRepository;
import com.example.demo.repository.entity.UserRepository;
import com.example.demo.service.cursor.AbstractCursorService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

@Service
public class LibraryService extends AbstractCursorService<PurchasedMovie, LibrarianMovieViewDTO> {

    private final PurchasedMovieRepository purchasedMovieRepository;

    public LibraryService(PurchasedMovieRepository purchasedMovieRepository,
                          UserRepository userRepository,
                          UserMapper userMapper,
                          PurchasedMapper purchasedMapper,
                          @Value("${app.catalog.page-size:20}") int defaultPageSize) {
        super(userRepository,
                userMapper,
                entities -> entities.stream()
                        .map(purchasedMapper::toOwnerViewFromLibrarian)
                        .toList(),
                purchasedMovie -> purchasedMovie.getId().getMovieId(),
                defaultPageSize);
        this.purchasedMovieRepository = purchasedMovieRepository;
    }

    @Override
    protected Slice<PurchasedMovie> fetchSlice(Long userId,
                                               Long cursorValue,
                                               boolean isAscending,
                                               Pageable pageable) {
        return isAscending
                ? purchasedMovieRepository.findPageAsc(userId, cursorValue, pageable)
                : purchasedMovieRepository.findPageDesc(userId, cursorValue, pageable);
    }
}