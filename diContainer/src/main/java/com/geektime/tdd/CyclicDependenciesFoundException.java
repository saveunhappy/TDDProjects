package com.geektime.tdd;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

public class CyclicDependenciesFoundException extends RuntimeException{
    private Set<Class<?>> components = new HashSet<>();

    public CyclicDependenciesFoundException(List<Class<?>> visiting) {
        components.addAll(visiting);
    }

    public Class<?>[] getComponents() {
        return components.toArray(Class<?>[]::new);
    }
}
