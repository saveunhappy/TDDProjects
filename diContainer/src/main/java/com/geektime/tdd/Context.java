package com.geektime.tdd;

import java.lang.reflect.Type;
import java.util.Optional;

public interface Context {
    default Optional get(Type type){
        return get(Ref.of(type));
    }
    Optional get(Ref ref);
}
