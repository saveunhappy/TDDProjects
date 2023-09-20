package com.geektime.tdd.args;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;

public class Args {
    @SuppressWarnings("unchecked")
    public static <T> T parse(Class<T> optionsClass, String... args) {
        try {
            Constructor<?> constructor = optionsClass.getDeclaredConstructors()[0];
            Parameter parameter = constructor.getParameters()[0];
            Option option = parameter.getAnnotation(Option.class);//这个就是l,p,d,传的参数是-l,-p,-d,
            List<String> arguments = Arrays.asList(args);
            return (T) constructor.newInstance(arguments.contains("-" +option.value()));//在这里进行拼接
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
