package com.example.demo.service.catalog;

import com.example.demo.dto.catalog.LibrarianMovieViewDTO;
import com.example.demo.mapper.entity.PurchasedMapper;
import com.example.demo.model.entity.PurchasedMovie;
import com.example.demo.repository.entity.PurchasedMovieRepository;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class LibraryService extends AbstractCatalogService<PurchasedMovie, LibrarianMovieViewDTO> {

    private final PurchasedMapper purchasedMapper;

    protected LibraryService(PurchasedMovieRepository purchasedMovieRepository,
                             PurchasedMapper purchasedMapper) {
        super(purchasedMovieRepository);
        this.purchasedMapper = purchasedMapper;
    }

    @Override
    protected Function<PurchasedMovie, LibrarianMovieViewDTO> getMapper() {
        return purchasedMapper::toOwnerViewFromLibrarian;
    }
}