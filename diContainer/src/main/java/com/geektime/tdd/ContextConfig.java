package com.geektime.tdd;

import jakarta.inject.Provider;
import jakarta.inject.Qualifier;
import jakarta.inject.Scope;
import jakarta.inject.Singleton;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.function.Function;

import static java.util.Arrays.stream;

public class ContextConfig {
    private Map<Component, ComponentProvider<?>> components = new HashMap<>();
    private Map<Class<?>, Function<ComponentProvider<?>, ComponentProvider<?>>> scopes = new HashMap<>();

    public ContextConfig() {
        scope(Singleton.class, SingletonProvider::new);
    }

    public <Type> void bind(Class<Type> type, Type instance) {
        components.put(new Component(type, null), context -> instance);
    }

    public <Type> void bind(Class<Type> type, Type instance, Annotation... qualifiers) {
        if (stream(qualifiers).anyMatch(q -> !q.annotationType().isAnnotationPresent(Qualifier.class))) {
            throw new IllegalComponentException();
        }
        for (Annotation qualifier : qualifiers) {
            components.put(new Component(type, qualifier), context -> instance);
        }
    }

    public <Type, Implementation extends Type>
    void bind(Class<Type> type, Class<Implementation> implementation) {
        //components.put(new Component(type, null), new InjectionProvider<>(implementation));
        //可以使用delegate的方式，既然是类上面有注解，那么就获取这个类上的注解，getAnnotations();
        bind(type, implementation, implementation.getAnnotations());
    }

    public <Type, Implementation extends Type>
    void bind(Class<Type> type, Class<Implementation> implementation, Annotation... annotations) {
        if (stream(annotations).map(Annotation::annotationType)
                .anyMatch(t -> !t.isAnnotationPresent(Qualifier.class) && !t.isAnnotationPresent(Scope.class))) {
            throw new IllegalComponentException();
        }
        //这种写法不可以，会限定成Class，并且上边界限定成Annotation，但是这边要求本身就是Annotation。
//        List<? extends Class<? extends Annotation>> qualifiers = stream(annotations).map(Annotation::annotationType).filter(a -> a.isAnnotationPresent(Qualifier.class)).toList();
        Optional<Annotation> scopeFromType = stream(implementation.getAnnotations()).filter(a -> a.annotationType().isAnnotationPresent(Scope.class)).findFirst();

        List<Annotation> qualifiers = stream(annotations)
                .filter(a -> a.annotationType().isAnnotationPresent(Qualifier.class)).toList();
        Optional<Annotation> scope = stream(annotations)
                .filter(a -> a.annotationType().isAnnotationPresent(Scope.class)).findFirst()
                .or(() -> scopeFromType);

        ComponentProvider<?> injectionProvider = new InjectionProvider<>(implementation);
        ComponentProvider<?> provider = scope
        .<ComponentProvider<?>>map(s -> new SingletonProvider(injectionProvider))
                .orElse(injectionProvider);
        if (qualifiers.isEmpty()) {
            components.put(new Component(type, null), provider);
        }
        for (Annotation qualifier : qualifiers) {
            components.put(new Component(type, qualifier), provider);
        }
    }

    public <ScopeType extends Annotation> void scope(Class<ScopeType> scope,
                                                     Function<ComponentProvider<?>, ComponentProvider<?>> provider) {
        scopes.put(scope, provider);
    }

    static class SingletonProvider<T> implements ComponentProvider<T> {
        private T singleton;
        private ComponentProvider<T> provider;

        public SingletonProvider(ComponentProvider<T> provider) {
            this.provider = provider;
        }

        @Override
        public T get(Context context) {
            //第一次进入这里肯定是null，还是反射去创建，第二次进入就有值了，那么就取出来，就相当于是singleton了
            if (singleton == null) {
                singleton = provider.get(context);
            }
            return singleton;
        }

        @Override
        public List<ComponentRef<?>> getDependencies() {
            return provider.getDependencies();
        }
    }

    public Context getContext() {
        //这个dependencies中就是记录了所有的，还有你的参数中有的依赖，也去给你put进去，

        for (Component component : components.keySet()) {
            checkDependencies(component, new Stack<>());
        }
        return new Context() {
            @Override
            public <ComponentType> Optional<ComponentType> get(ComponentRef<ComponentType> ref) {
                if (ref.isContainer()) {
                    if (ref.getContainer() != Provider.class) return Optional.empty();
                    return (Optional<ComponentType>) Optional.ofNullable(getComponent(ref))
                            .map(provider -> (Provider<Object>) () -> provider.get(this));
                }
                return Optional.ofNullable(getComponent(ref)).
                        map(provider -> (ComponentType) provider.get(this));
            }

        };
    }

    private <ComponentType> ComponentProvider<?> getComponent(ComponentRef<ComponentType> ref) {
        return components.get(ref.component());
    }

    private void checkDependencies(Component component, Stack<Component> visiting) {
        /*注意原来的实现，dependencies.get(component)获取的是什么？是一个List，就是所有的依赖，然后接下来就是去判断
         * containsKey,如果没有Bind过，那么当然没有啊*/
        //这个是去找的所有bind过的依赖，然后把所有的key的依赖都放到一个栈中去，这里是找的所有的依赖，如果之前有添加过
        //那就说明有环了，就是有循环依赖
        for (ComponentRef dependency : components.get(component).getDependencies()) {
            //这个dependency.component()就是指的Dependency，其实就是dependency本身，但是有其他的属性，所以就
            //把属性封装到了component()这个方法中去，
            if (!components.containsKey(dependency.component())) {
                throw new DependencyNotFoundException(component, dependency.component());
            }
            if (!dependency.isContainer()) {
                if (visiting.contains(dependency.component())) throw new CyclicDependenciesFoundException(visiting);
                visiting.push(dependency.component());
                checkDependencies(dependency.component(), visiting);
//                checkDependencies(dependency.getComponent(), visiting);
                visiting.pop();
            }

        }
    }

}
