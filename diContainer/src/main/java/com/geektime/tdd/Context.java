package com.geektime.tdd;

import java.lang.reflect.ParameterizedType;
import java.util.Optional;

public interface Context {
    <Type> Optional<Type> get(Class<Type> type);

    Optional<Object> get(ParameterizedType type);
}
