package com.geektime.tdd.args;

import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

class SingleValueOptionParser<T> implements OptionParser<T> {
    Function<String, T> valueParser;

    T defaultValue;


    public SingleValueOptionParser(T defaultValue, Function<String, T> valueParser) {
        this.defaultValue = defaultValue;
        this.valueParser = valueParser;
    }

    @Override
    public T parse(List<String> arguments, Option option) {
        int index = arguments.indexOf("-" + option.value());

        if (index == -1) return defaultValue;
        //这个返回的是下一个-l/-p/-d的索引，因为IntStream返回的就是Int值
        int followingFlag = IntStream.range(index + 1, arguments.size())
                .filter(it -> arguments.get(it).startsWith("-"))
                .findFirst().orElse(arguments.size());
        //如果找到了刚开始的那个flag，然后找到接下来的那个flag，如果存在，比如size等于1，那么就是一个参数，是正常的
        //如果第一个标志后面紧跟着一个标志，那么就是返回的参数的个数，第一个flag的位置是0，index + 1就是1，如果中间没有参数，
        // 比如-p -l，并且起始位置是从Index + 1开始的，index是flag的位置，加1就是下一个的位置，
        // 那么.filter(it -> arguments.get(it).startsWith("-"))获取到的下一个的位置就是 -l的位置，所以following的位置就是1
        //这个就是从1到1的区间，那就是0，小于1，就是参数不够，如果是-p,那么找不到arguments.get(it).startsWith("-")，
        // 还是返回个数，1个，那还是一样的，所以，这个就是参数不够
        //如果是 -p 8080 8081 -d /usr/log,范围是从1-3,那么就是两个参数，那么就是多参数了
        List<String> values = arguments.subList(index + 1, followingFlag);

        if (values.size() < 1) throw new InsufficientException(option.value());
        if (values.size() > 1) throw new TooManyArgumentsException(option.value());
        return valueParser.apply(arguments.get(index + 1));
    }

}
