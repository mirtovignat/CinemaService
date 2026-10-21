package com.example.demo.cursor.codec;

import com.example.demo.cursor.RecommendationCursorKey;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Objects;

@Component
public class RecommendationKeyCodec {

    public String encode(BigDecimal rating, Long id) {
        Objects.requireNonNull(id);
        String ratingPart = rating != null ? rating.toPlainString() : "null";
        return ratingPart + "_" + id;
    }

    public RecommendationCursorKey decode(String cursorValue) {
        int separatorIndex = cursorValue.lastIndexOf("_");
        if (separatorIndex <= 0 || separatorIndex == cursorValue.length() - 1) {
            throw BusinessException.of(ErrorCode.INVALID_CURSOR);
        }

        String ratingPart = cursorValue.substring(0, separatorIndex);
        String identifierPart = cursorValue.substring(separatorIndex + 1);

        BigDecimal rating;
        if ("null".equals(ratingPart)) {
            rating = null;
        } else {
            try {
                rating = new BigDecimal(ratingPart);
            } catch (NumberFormatException numberFormatException) {
                throw BusinessException.of(ErrorCode.INVALID_CURSOR);
            }
        }
        long identifier;
        try {
            identifier = Long.parseLong(identifierPart);
        } catch (NumberFormatException numberFormatException) {
            throw BusinessException.of(ErrorCode.INVALID_CURSOR);
        }

        return RecommendationCursorKey.builder()
                .rating(rating)
                .identifier(identifier)
                .build();
    }
}