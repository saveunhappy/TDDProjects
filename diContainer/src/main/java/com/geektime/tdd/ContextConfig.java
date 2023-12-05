package com.geektime.tdd;

import jakarta.inject.Provider;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

import static java.util.Arrays.stream;

public class ContextConfig {
    private Map<Class<?>, ComponentProvider<?>> providers = new HashMap<>();

    public <Type> void bind(Class<Type> type, Type instance) {
        providers.put(type, context -> instance);
    }

    public <Type, Implementation extends Type>
    void bind(Class<Type> type, Class<Implementation> implementation) {
        providers.put(type, new InjectionProvider<>(implementation));
    }

    public Context getContext() {
        //这个dependencies中就是记录了所有的，还有你的参数中有的依赖，也去给你put进去，
        for (Class<?> component : providers.keySet()) {
            checkDependencies(component, new Stack<>());
        }
        return new Context() {
            @Override
            public Optional get(Type type) {
                if (isContainerType(type)) return getContainer((ParameterizedType) type);
                return getComponent((Class<?>) type);
            }

            private <Type> Optional<Type> getComponent(Class<Type> type) {
                return Optional.ofNullable(providers.get(type)).
                        map(provider -> (Type) provider.get(this));
            }

            private Optional<Object> getContainer(ParameterizedType type) {

                if (type.getRawType() != Provider.class) return Optional.empty();
                return Optional.ofNullable(providers.get(getComponentType(type)))
                        //如果其他类型也可以的话，这里就不是这么写的了，因为这里返回值固定就是Provider<Object>
                        //而这个方法的返回值Optional<Object>中的Object就是指代Provider<Object>这个整体，返回的只能是
                        //Provider这个类型的，其他的类型不支持，所以报错
                        /** 注意这里 ，map里面是创建了一个Provider，创建这个对象的时候，
                         * 对象是Dependency的实现类，构造器依赖是Provider<Component>
                         * 然后Component是jakarta的Provider的get获取的，可以理解为
                         * 懒加载，或者说是创建了一个Provider对象，并不是Component对象
                         * 在真正获取对象的时候采取调用get方法
                         * */
                        .map(provider -> (Provider<Object>) () -> provider.get(this));
            }
        };
    }

    private static Class<?> getComponentType(Type type) {
        return (Class<?>) ((ParameterizedType)type).getActualTypeArguments()[0];
    }

    private static boolean isContainerType(Type type) {
        return type instanceof ParameterizedType;
    }

    private void checkDependencies(Class<?> component, Stack<Class<?>> visiting) {
        /*注意原来的实现，dependencies.get(component)获取的是什么？是一个List，就是所有的依赖，然后接下来就是去判断
         * containsKey,如果没有Bind过，那么当然没有啊*/
        //这个是去找的所有bind过的依赖，然后把所有的key的依赖都放到一个栈中去，这里是找的所有的依赖，如果之前有添加过
        //那就说明有环了，就是有循环依赖
        for (Type dependency : providers.get(component).getDependencies()) {
            if (isContainerType(dependency)) {
                checkContainerTypeDependency(component, dependency);
            }else{
                checkComponentDependency(component, visiting, (Class<?>) dependency);
            }

        }
    }

    private void checkContainerTypeDependency(Class<?> component, Type dependency) {
        if (!providers.containsKey(getComponentType(dependency))) throw new DependencyNotFoundException(component, getComponentType(dependency));
    }

    private void checkComponentDependency(Class<?> component, Stack<Class<?>> visiting, Class<?> dependency) {
        //就是你bind一个接口就得有一个实现类，如果你Component依赖了Dependency
        //但是你没有bind过，那就在dependencies没有找到，就抛出异常,key是bind过的，value就是你的构造器参数
        //也是一个Class，如果没有找到，那是不应该的，因为key和value都应该在dependencies中的
        //Component->Dependency->String现在如果String没有，
        // 但是依赖有两个Component:Dependency,Dependency:String
        //dependencies.get(String)这个找不到，没有Bind过，所以报错
        if (!providers.containsKey(dependency)) throw new DependencyNotFoundException(component, dependency);
        if (visiting.contains(dependency)) throw new CyclicDependenciesFoundException(visiting);
        visiting.push(dependency);
        checkDependencies(dependency, visiting);
        visiting.pop();
    }

}
