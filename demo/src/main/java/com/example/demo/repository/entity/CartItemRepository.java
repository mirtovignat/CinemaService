package com.example.demo.repository.entity;

import com.example.demo.model.entity.CartItem;
import com.example.demo.repository.base.AbstractLinkedCollectionRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CartItemRepository extends AbstractLinkedCollectionRepository<CartItem> {

}