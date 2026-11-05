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
        String ratingPart;
        if (rating != null) {
            ratingPart = rating.toPlainString();
        } else {
            ratingPart = "null";
        }
        return ratingPart + "_" + id;
    }

    public RecommendationCursorKey decode(String cursorValue) {
        int separatorIndex = cursorValue.lastIndexOf("_");
        if (separatorIndex <= 0 || separatorIndex == cursorValue.length() - 1) {
            throw BusinessException.of(ErrorCode.INVALID_CURSOR);
        }

        String movieRatingPart = cursorValue.substring(0, separatorIndex);
        String movieIdPart = cursorValue.substring(separatorIndex + 1);

        BigDecimal rating;
        if ("null".equals(movieRatingPart)) {
            rating = null;
        } else {
            try {
                rating = new BigDecimal(movieRatingPart);
            } catch (NumberFormatException numberFormatException) {
                throw BusinessException.of(ErrorCode.INVALID_CURSOR);
            }
        }
        long movieId;
        try {
            movieId = Long.parseLong(movieIdPart);
        } catch (NumberFormatException numberFormatException) {
            throw BusinessException.of(ErrorCode.INVALID_CURSOR);
        }

        return RecommendationCursorKey.builder()
                .movieRating(rating)
                .movieId(movieId)
                .build();
    }
}