package com.geektime.tdd;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

class Ref {
    private Type container;
    private final Class<?> component;

    public Ref(ParameterizedType container) {
        this.container = container.getRawType();
        this.component = (Class<?>) container.getActualTypeArguments()[0];
    }

    public Ref(Class<?> component) {
        this.component = component;
    }

    static Ref of(Type type) {
        if (type instanceof ParameterizedType container) return new Ref(container);
        return new Ref((Class<?>) type);
    }

    public Type getContainer() {
        return container;
    }

    public Class<?> getComponent() {
        return component;
    }

    public boolean isContainer() {
        return this.container != null;
    }
}
