package com.geektime.tdd;

import java.util.*;

import static java.util.Arrays.stream;

public class ContextConfig {
    private Map<Class<?>, ComponentProvider<?>> providers = new HashMap<>();

    public <Type> void bind(Class<Type> type, Type instance) {
        providers.put(type, new ComponentProvider<>() {
            @Override
            public Object get(Context context) {
                return instance;
            }

            @Override
            public List<Class<?>> getDependency() {
                return List.of();
            }
        });
    }

    //这个和    public static <T> T parse(Class<T> optionsClass, String... args) 一样的，只是泛型的名字变了。
    public <Type, Implementation extends Type>
    void bind(Class<Type> type, Class<Implementation> implementation) {
        providers.put(type, new ConstructorInjectionProvider<>(implementation));
    }

    public Context getContext() {
        //这个dependencies中就是记录了所有的，还有你的参数中有的依赖，也去给你put进去，
        for (Class<?> component : providers.keySet()) {
            checkDependencies(component, new Stack<>());
        }
        return new Context() {
            @Override
            public <Type> Optional<Type> get(Class<Type> type) {
                return Optional.ofNullable(providers.get(type)).map(provider -> (Type) provider.get(this));
            }
        };
    }

    private void checkDependencies(Class<?> component, Stack<Class<?>> visiting) {
        /*注意原来的实现，dependencies.get(component)获取的是什么？是一个List，就是所有的依赖，然后接下来就是去判断
        * containsKey,如果没有Bind过，那么当然没有啊*/
        //这个是去找的所有bind过的依赖，然后把所有的key的依赖都放到一个栈中去，这里是找的所有的依赖，如果之前有添加过
        //那就说明有环了，就是有循环依赖
        for (Class<?> dependency : providers.get(component).getDependency()) {
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

}
