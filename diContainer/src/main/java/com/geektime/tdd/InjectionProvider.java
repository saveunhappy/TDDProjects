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
            T instance = injectConstructor.newInstance(toDependency(context, injectConstructor));
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

    private Object[] toDependency(Context context, Executable executable) {
        return stream(executable.getParameterTypes())
                .map(t -> context.get(t).get())
                .toArray(Object[]::new);
    }

    @Override
    public List<Class<?>> getDependency() {
        return concat(concat(stream(injectConstructor.getParameters()).map(Parameter::getType), injectFields.stream().map(Field::getType)),
                //是要取所有method的所有参数，method有多个，一个method又有多个参数，所以使用flatMap
                injectMethods.stream().flatMap(m -> stream(m.getParameterTypes()))).toList();
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

    private static <T> List<Method> getInjectMethods(Class<T> component) {
        List<Method> injectMethods = new ArrayList<>();
        Class<?> current = component;
        while (current != Object.class) {
            //子到父遍历的，先把子的标注了@Inject的添加进来
            //injectable就是标注了@Inject注解的
            injectMethods.addAll(injectable(current.getDeclaredMethods())
                    //然后到父类之后，injectMethods里面就有@Inject方法了，然后这个时候
                    //父类的@Inject标注的方法和子类的就重复了，就不添加了，因为下面有reverse，
                    //所以调用的时候也是先调用的父类的
                    .filter(m -> isOverrideByInjectMethod(injectMethods, m))
                    //这个是component，最底层的那个，就是实现类，首先是往父类一直走的，第一步是没有添加
                    // @Inject注解，所以没有添加进去，然后到父类，但是父类是有的@Inject注解的，然后添加进来了
                    //然后如果没有这行代码那么就添加进来了，然后加了这行代码，发现和最底层的那个没有加@Inject
                    //注解的方法名一样，就过滤掉，然后就不添加进来了。因为如果添加进来了，那么使用构造器创建对象
                    //创建的就是实现类，方法是父类的方法，然后子类也有这个方法，创建的也是子类的方法，所以就调用了
                    .filter(m -> isOverrideByNoInjectMethod(component, m))
                    .toList());
            current = current.getSuperclass();
        }
        //注意，这里reverse了，所以是从父类开始调用的，如果是直接调用父类的话，那么不就是相当于FieldInjection吗？就不用
        //考虑这么多了
        Collections.reverse(injectMethods);
        return injectMethods;
    }

    private static <Type> Constructor<Type> getInjectConstructor(Class<Type> implementation) {
        List<Constructor<?>> injectConstructors = injectable(implementation.getConstructors()).toList();
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

    private static <T extends AnnotatedElement> Stream<T> injectable(T[] declaredFields) {
        return stream(declaredFields).filter(f -> f.isAnnotationPresent(Inject.class));
    }

    private static boolean isOverride(Method m, Method o) {
        return o.getName().equals(m.getName()) && Arrays.equals(o.getParameterTypes(), m.getParameterTypes());
    }

    private static <T> boolean isOverrideByNoInjectMethod(Class<T> component, Method m) {
        return stream(component.getDeclaredMethods()).
                filter(m1 -> !m1.isAnnotationPresent(Inject.class))
                .noneMatch(o -> isOverride(m, o));
    }

    private static boolean isOverrideByInjectMethod(List<Method> injectMethods, Method m) {
        return injectMethods.stream().noneMatch(o -> isOverride(m, o));
    }

}
