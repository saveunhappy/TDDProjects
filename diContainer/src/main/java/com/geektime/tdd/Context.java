package com.geektime.tdd;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;

public interface Context {
    Optional getType(Type type);
}
