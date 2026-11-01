package com.example.demo.util.validation;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Emptiness {

    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static boolean isPresent(String value) {
        return !isBlank(value);
    }

    public static <T> boolean isEmpty(Collection<T> collection) {
        return collection == null || collection.isEmpty();
    }

    public static <T> boolean isNotEmpty(Collection<T> collection) {
        return !isEmpty(collection);
    }

    public static <K, V> boolean isEmpty(Map<K, V> map) {
        return map == null || map.isEmpty();
    }

    @SafeVarargs
    public static <T> boolean isAnyNull(T... values) {
        if (values == null) {
            return true;
        }
        for (T value : values) {
            if (value == null) {
                return true;
            }
        }
        return false;
    }
}