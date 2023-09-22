package com.geektime.tdd.args;

import java.util.List;
import java.util.function.Function;

class SingleValueOptionParser<T> implements OptionParser<T> {
    Function<String, T> valueParser;

    T defaultValue;


    public SingleValueOptionParser(Function<String, T> valueParser) {
        this.valueParser = valueParser;
    }

    public SingleValueOptionParser(T defaultValue,Function<String, T> valueParser) {
        this.defaultValue = defaultValue;
        this.valueParser = valueParser;
    }

    @Override
    public T parse(List<String> arguments, Option option) {
        int index = arguments.indexOf("-" + option.value());

        if(index == -1) return defaultValue;
        //如果索引+1，按这里说就是1，size也是1，那么就说明只有一个参数，那么也报错
        if(index + 1 == arguments.size() || arguments.get(index + 1).startsWith("-")) throw new InsufficientException(option.value());
        // -p 8080 8081 这个index就是-p的index,所以如果是索引加2，就是获取多的那个参数，没有超过传的参数的情况，size是个数
        //就是3，index +2就是2
        //那么就获取到了，如果获取到了，并且不是以为-p开头，那么就是参数多了
        if((index + 2) < arguments.size() && !arguments.get(index + 2).startsWith("-")) throw new TooManyArgumentsException(option.value());
        return valueParser.apply(arguments.get(index + 1));
    }

}
