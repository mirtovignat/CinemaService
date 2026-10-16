// demo/src/main/java/com/example/demo/service/cursor/AbstractCursorService.java
package com.example.demo.service.cursor;

import com.example.demo.dto.response.CursorPageResponse;
import com.example.demo.dto.response.UserCursorPageResponse;
import com.example.demo.dto.user.UserSidebarViewDTO;
import com.example.demo.mapper.entity.UserMapper;
import com.example.demo.repository.entity.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Function;

public abstract class AbstractCursorService<Entity, DTO> {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final CursorContentMapper<Entity, DTO> contentMapper;
    private final Function<Entity, Long> cursorExtractor;
    private final int defaultPageSize;

    protected AbstractCursorService(UserRepository userRepository,
                                    UserMapper userMapper,
                                    CursorContentMapper<Entity, DTO> contentMapper,
                                    Function<Entity, Long> cursorExtractor,
                                    int defaultPageSize) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.contentMapper = contentMapper;
        this.cursorExtractor = cursorExtractor;
        this.defaultPageSize = defaultPageSize;
    }

    @Transactional(readOnly = true)
    public CursorPageResponse<DTO> getPage(Long userId,
                                           String rawCursor,
                                           String sortDirection,
                                           Integer requestedPageSize) {
        Long cursor = decodeCursor(rawCursor);
        String normalizedSort = normalizeSortDirection(sortDirection);
        boolean ascending = "asc".equals(normalizedSort);

        int pageSize = resolvePageSize(requestedPageSize);
        Pageable pageable = PageRequest.of(0, pageSize + 1);

        Slice<Entity> slice = fetchSlice(userId, cursor, ascending, pageable);

        List<Entity> rawContent = slice.getContent();
        boolean hasMore = rawContent.size() > pageSize;
        List<Entity> contentEntities = hasMore
                ? rawContent.subList(0, pageSize)
                : rawContent;

        List<DTO> content = contentMapper.map(contentEntities);

        String nextCursor = hasMore && !contentEntities.isEmpty()
                ? encodeCursor(cursorExtractor.apply(contentEntities.get(contentEntities.size() - 1)))
                : null;

        return CursorPageResponse.<DTO>builder()
                .content(content)
                .hasMore(hasMore)
                .nextCursor(nextCursor)
                .sort(normalizedSort)
                .build();
    }

    @Transactional(readOnly = true)
    public UserCursorPageResponse<DTO> getPageWithSidebar(Long userId,
                                                          String rawCursor,
                                                          String sortDirection,
                                                          Integer requestedPageSize) {
        CursorPageResponse<DTO> page = getPage(userId, rawCursor, sortDirection, requestedPageSize);
        UserSidebarViewDTO sidebar = userMapper.toSidebarView(
                userRepository.findByIdOrThrow(userId));
        return UserCursorPageResponse.of(page, sidebar);
    }

    private Long decodeCursor(String rawCursor) {
        if (rawCursor == null || rawCursor.isBlank()) return null;
        try {
            return Long.parseLong(rawCursor.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String encodeCursor(Long cursorValue) {
        return cursorValue != null ? String.valueOf(cursorValue) : null;
    }

    private String normalizeSortDirection(String sortDirection) {
        if (sortDirection == null) return "desc";
        return "asc".equalsIgnoreCase(sortDirection) ? "asc" : "desc";
    }

    private int resolvePageSize(Integer requestedPageSize) {
        if (requestedPageSize == null || requestedPageSize <= 0) return defaultPageSize;
        return Math.min(requestedPageSize, 100);
    }

    protected abstract Slice<Entity> fetchSlice(Long userId,
                                                Long cursorValue,
                                                boolean isAscending,
                                                Pageable pageable);
}