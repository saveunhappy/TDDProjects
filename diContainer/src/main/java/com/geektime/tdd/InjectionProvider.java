package com.geektime.tdd;

import jakarta.inject.Inject;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static java.util.stream.Stream.concat;

class InjectionProvider<T> implements ComponentProvider<T> {

    private Constructor<T> injectConstructor;

    private List<Field> injectFields;

    private List<Method> injectMethods;

    public InjectionProvider(Class<T> component) {
        if (Modifier.isAbstract(component.getModifiers())) throw new IllegalComponentException();
        this.injectConstructor = getInjectConstructor(component);
        this.injectFields = getInjectFields(component);
        this.injectMethods = getInjectMethods(component);
        if (injectFields.stream().anyMatch(f -> Modifier.isFinal(f.getModifiers())))
            throw new IllegalComponentException();
        if (injectMethods.stream().anyMatch(m -> m.getTypeParameters().length != 0))
            throw new IllegalComponentException();
    }


    @Override
    public T get(Context context) {
        try {
            Object[] dependencies = stream(injectConstructor.getParameters())
                    .map(p -> context.get(p.getType()).get())
                    .toArray(Object[]::new);
            T instance = injectConstructor.newInstance(dependencies);
            for (Field field : injectFields) {
                field.set(instance, context.get(field.getType()).get());
            }
            for (Method method : injectMethods) {
                //目前是install没有参数，那么就不会进入map，一会儿看有参数的情况
                method.invoke(instance, stream(method.getParameterTypes())
                        //invoke调用的第一个参数是对象，第二个是参数，t -> context.get(t).get()是把每一个
                        //bind过的都取出来，getParameterTypes是类型，get(t)就是取bind过的实体对象
                        // 然后toArray，变成一个参数的数组。
                        .map(t -> context.get(t).get()).toArray());
            }
            return instance;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Class<?>> getDependency() {
        return concat(concat(stream(injectConstructor.getParameters()).map(Parameter::getType),
                        injectFields.stream().map(Field::getType)),
                //是要取所有method的所有参数，method有多个，一个method又有多个参数，所以使用flatMap
                injectMethods.stream().flatMap(m -> stream(m.getParameterTypes())))
                .toList();
    }

    private static <T> List<Field> getInjectFields(Class<T> component) {
        List<Field> injectFields = new ArrayList<>();
        Class<?> current = component;
        while (current != Object.class) {
            //注意，这里是current
            injectFields.addAll(injectable(current.getDeclaredFields()).toList());
            current = current.getSuperclass();
        }
        return injectFields;
    }

    private static <T extends AnnotatedElement> Stream<T> injectable(T[] declaredFields) {
        return stream(declaredFields)
                .filter(f -> f.isAnnotationPresent(Inject.class));
    }

    private static <T> List<Method> getInjectMethods(Class<T> component) {
        List<Method> injectMethods = new ArrayList<>();
        Class<?> current = component;
        while (current != Object.class) {
            injectMethods.addAll(
                    //先获取到所有的方法
                    stream(current.getDeclaredMethods())
                            //然后获取到所有标注了Inject的
                            .filter(m -> m.isAnnotationPresent(Inject.class))
                            //然后如果往上走的话，filter的是要留下的，noneMatch就是不匹配的，就是如果是和父类的签名一样
                            //那么就不添加，那反之，就添加，所以noneMatch就是要求签名不一样的才添加进来，
                            //然后还有一个，要求参数类型一样，记住，java中是有重载的，所以要名字和参数一样的才不添加
                            .filter(m -> injectMethods.stream().noneMatch(o -> o.getName().equals(m.getName())
                                    && Arrays.equals(o.getParameterTypes(), m.getParameterTypes())))
                            //就是你直接bind的这个实现类，如果你的方法没有加@Inject，但是你有父类，而且你的父类有同名的
                            //方法又@Injecct,你这个没有加@Inject的方法就不应该被调用，所以要过滤掉，因为是依赖注入的
                            //所以你这个是普通的方法，就不会在获取的时候调用，这个就是如果没有标注@Inject但是和父类
                            //的方法一样，也不应该添加进来
                            .filter(m -> stream(component.getDeclaredMethods()).filter(m1 -> !m1.isAnnotationPresent(Inject.class))
                                    .noneMatch(o -> o.getName().equals(m.getName())
                                            && Arrays.equals(o.getParameterTypes(), m.getParameterTypes())))

                            .toList());
            current = current.getSuperclass();
        }
        Collections.reverse(injectMethods);

        return injectMethods;
    }

    private static <Type> Constructor<Type> getInjectConstructor(Class<Type> implementation) {
        List<Constructor<?>> injectConstructors = stream(implementation.getConstructors())
                .filter(c -> c.isAnnotationPresent(Inject.class)).toList();
        if (injectConstructors.size() > 1) throw new IllegalComponentException();

        //找不到被@Inject标注的，并且找不到默认的构造函数
        return (Constructor<Type>) injectConstructors.stream().findFirst().orElseGet(() -> {
            try {
                return implementation.getDeclaredConstructor();
            } catch (NoSuchMethodException e) {
                //这里之前是RuntimeException，改成我们的自定义异常了
                throw new IllegalComponentException();
            }
        });
    }
}
