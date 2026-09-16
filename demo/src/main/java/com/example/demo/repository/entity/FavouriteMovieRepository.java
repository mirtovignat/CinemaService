package com.example.demo.repository.entity;

import com.example.demo.model.entity.FavouriteMovie;
import com.example.demo.repository.base.AbstractLinkedCollectionRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FavouriteMovieRepository extends AbstractLinkedCollectionRepository<FavouriteMovie> {

}