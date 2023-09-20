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
            Object value = null;
            if(parameter.getType() == boolean.class){
                value = arguments.contains("-" + option.value());
            }
            if(parameter.getType() == int.class){
                //获取-p的索引
                int index = arguments.indexOf("-" + option.value());
                //那-p后面跟着的8080就是index的位置 + 1了，然后这个获取到是String类型的，需要转换为Int类型的
                value = Integer.valueOf(arguments.get(index + 1));
            }
            if(parameter.getType() == String.class){
                int index = arguments.indexOf("-" + option.value());
                value = arguments.get(index + 1);
            }
            //value就是-l/p/d后面的
            return (T) constructor.newInstance(value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
