package com.example.demo.util;

import com.example.demo.model.entity.User;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

@Component
public class UserSuffixGenerator {

    private static final int SUFFIX_LENGTH = 12;

    public String generate(User user) {
        Long userId = user.getId();
        if (userId == null) {
            throw new IllegalStateException("User id is null");
        }
        String raw = user.getUsername() + "_" + userId;
        String hash = DigestUtils.md5DigestAsHex(
                raw.getBytes(StandardCharsets.UTF_8)
        );
        return hash.substring(0, SUFFIX_LENGTH);
    }
}