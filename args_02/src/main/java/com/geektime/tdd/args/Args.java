package com.geektime.tdd.args;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Args {
    @SuppressWarnings("unchecked")
    public static <T> T parse(Class<T> optionsClass, String... args) {
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
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Object parseOption(List<String> arguments, Parameter parameter) {
        //这个就是l,p,d,传的参数是-l,-p,-d,
        Class<?> type = parameter.getType();
        return PARSER.get(type).parse(arguments, parameter.getAnnotation(Option.class));
    }

    private static Map<Class<?>,OptionParser> PARSER = Map.of(
            boolean.class,new BooleanOptionParser(),
            int.class, new IntOptionParser(),
            String.class, StringOptionParser.createStringOptionParser());


}
