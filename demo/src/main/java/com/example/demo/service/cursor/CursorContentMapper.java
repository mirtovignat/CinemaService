package com.example.demo.service.cursor;

import java.util.List;

@FunctionalInterface
public interface CursorContentMapper<Entity, DTO> {
    List<DTO> map(List<Entity> entities);
}