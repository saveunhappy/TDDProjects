package com.geektime.tdd;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

public interface Context {
    <ComponentType> Optional<ComponentType> get(Ref<ComponentType> ref);

    class Ref<ComponentType> {
        public static <ComponentType> Ref<ComponentType> of(Class<ComponentType> component) {
            return new Ref(component);
        }

        public static Ref of(Type type) {
            return new Ref(type);
        }

        private Type container;
        private Class<?> component;

        public Ref(Type type) {
            init(type);
        }

        public Ref(Class<ComponentType> component) {
            init(component);
        }

        protected Ref() {
            Type type = ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
            init(type);
        }

        private void init(Type type) {
            if (type instanceof ParameterizedType container) {
                this.container = container.getRawType();
                this.component = (Class<?>) container.getActualTypeArguments()[0];
            }else{
                this.component = (Class<?>) type;
            }
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Ref ref = (Ref) o;
            return Objects.equals(container, ref.container) && Objects.equals(component, ref.component);
        }

        @Override
        public int hashCode() {
            return Objects.hash(container, component);
        }
    }
}
