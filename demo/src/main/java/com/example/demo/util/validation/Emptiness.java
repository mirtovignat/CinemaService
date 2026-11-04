package com.example.demo.util.validation;

import java.util.Collection;
import java.util.Map;

public final class Emptiness {

    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static <T> boolean isEmpty(Collection<T> collection) {
        return collection == null || collection.isEmpty();
    }

    public static <K, V> boolean isEmpty(Map<K, V> map) {
        return map == null || map.isEmpty();
    }

}