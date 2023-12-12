package com.geektime.tdd;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;

public class ComponentRef<ComponentType> {
    public static <ComponentType> ComponentRef<ComponentType> of(Class<ComponentType> component) {
        return new ComponentRef(component, null);
    }

    public static <ComponentType> ComponentRef<ComponentType> of(Class<ComponentType> component, Annotation qualifier) {
        return new ComponentRef(component, qualifier);
    }

    public static ComponentRef of(Type type) {
        return new ComponentRef(type, null);
    }

    private Type container;

    private Component component;
    private Class<ComponentType> componentType;
    private Annotation qualifier;

    ComponentRef(Type type, Annotation qualifier) {
        init(type, qualifier);
        this.qualifier = qualifier;
    }

    protected ComponentRef() {
        Type type = ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        init(type, null);
    }

    private void init(Type type, Annotation qualifier) {
        if (type instanceof ParameterizedType container) {
            this.container = container.getRawType();
            this.componentType = (Class<ComponentType>) container.getActualTypeArguments()[0];
            this.component = new Component(componentType,qualifier);
        } else {
            this.componentType = (Class<ComponentType>) type;
            this.component = new Component(componentType,qualifier);
        }
    }

    public Annotation getQualifier() {
        return qualifier;
    }

    public Type getContainer() {
        return container;
    }

    public Class<?> getComponentType() {
        return componentType;
    }

    public boolean isContainer() {
        return this.container != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ComponentRef ref = (ComponentRef) o;
        return Objects.equals(container, ref.container) && Objects.equals(componentType, ref.componentType);
    }

    public Component component() {
        return component;
    }

    @Override
    public int hashCode() {
        return Objects.hash(container, componentType);
    }

}
