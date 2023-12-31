package com.geektime.tdd.args;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public class Args<T> {
    private static Map<Class<?>, OptionParser> PARSER = Map.of(
            boolean.class, OptionParsers.bool(),
            int.class, OptionParsers.unary(0, Integer::parseInt),
            String.class, OptionParsers.unary("", String::valueOf),
            String[].class,OptionParsers.list(String[]::new,String::valueOf),
            Integer[].class,OptionParsers.list(Integer[]::new,Integer::parseInt)
    );
    //这是外部的原来的api，所以这个是直接创建Args的时候就是传的PARSER，如果外部调用就是new，那就是自己传进来了。
    public static <T> T parse(Class<T> optionsClass, String... args) {
//        return parse(optionsClass, PARSER, args);
        return new Args<T>(optionsClass,PARSER).parse(args);
    }
    private Class<T> optionsClass;
    private Map<Class<?>, OptionParser> parser;

    public Args(Class<T> optionsClass, Map<Class<?>, OptionParser> parser) {
        this.optionsClass = optionsClass;
        this.parser = parser;
    }
    public T parse(String... args){
        try {
            List<String> arguments = Arrays.asList(args);
            Constructor<?> constructor = optionsClass.getDeclaredConstructors()[0];
            //为什么这样就可以了？抽取出了一个方法，那么arguments就是获取所有的参数了
            //然后通过stream的方式，每个参数都去调用parseOption的方式得到从arguments中筛选出一个对应的值，
            //如果是-l就返回boolean值，
            //如果是-p,那么也是通过index的方式去获取他后面的参数的，同理，-d也是获取他后面的
            //最后根据顺序，去通过反射去创建对象
            /**为什么更改顺序也可以成功解析？因为constructor.getParameters()获取的顺序是定下来的，所以是按照参数的顺序
             * 来进行访问的，所以你哪个在先哪个在后，是没有关系的，和你的自己定义的那个record是有关系的*/
            Object[] values = Arrays.stream(constructor.getParameters())
                    .map(it -> parseOption(arguments, it)).toArray();
            return (T) constructor.newInstance(values);
        }catch (IllegalOptionException e){
            throw e;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Object parseOption(List<String> arguments, Parameter parameter) {
        if(!parameter.isAnnotationPresent(Option.class)) throw new IllegalOptionException(parameter.getName());
        Option option = parameter.getAnnotation(Option.class);
        //这个就是l,p,d,传的参数是-l,-p,-d,
        Class<?> type = parameter.getType();
        if (!parser.containsKey(type)) {
            throw new UnsupportedOptionTypeException(option.value(), parameter.getType());
        }
        return parser.get(type).parse(arguments, option);
    }




}
