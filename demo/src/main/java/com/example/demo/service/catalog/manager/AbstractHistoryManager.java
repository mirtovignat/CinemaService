package com.example.demo.service.catalog.manager;

import com.example.demo.dto.response.GroupedHistoryResponse;
import com.example.demo.dto.user.UserForOwnerViewDTO;
import com.example.demo.mapper.entity.UserMapper;
import com.example.demo.model.entity.User;
import com.example.demo.repository.entity.UserRepository;
import com.example.demo.util.HistoryGrouper;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;

public abstract class AbstractHistoryManager<Entity, DTO> {

    private final UserRepository userRepository;
    private final HistoryGrouper historyGrouper;
    private final UserMapper userMapper;
    private final Function<DTO, LocalDateTime> dateExtractor;

    protected AbstractHistoryManager(UserRepository userRepository,
                                     HistoryGrouper historyGrouper,
                                     UserMapper userMapper,
                                     Function<DTO, LocalDateTime> dateExtractor) {
        this.userRepository = userRepository;
        this.historyGrouper = historyGrouper;
        this.userMapper = userMapper;
        this.dateExtractor = dateExtractor;
    }

    protected abstract List<Entity> fetchEntities(Long userId);

    protected abstract List<DTO> convertToDtos(List<Entity> entities);

    @Transactional(readOnly = true)
    public GroupedHistoryResponse<DTO> getGroupedHistory(Long userId) {
        User user = userRepository.findByIdOrThrow(userId);
        List<DTO> dtos = convertToDtos(fetchEntities(userId));
        var grouped = historyGrouper.groupByDateDesc(dtos, dateExtractor);
        UserForOwnerViewDTO userForOwner = userMapper.toOwnerView(user);
        return GroupedHistoryResponse.<DTO>builder()
                .userForOwner(userForOwner)
                .groupedItems(grouped)
                .build();
    }
}