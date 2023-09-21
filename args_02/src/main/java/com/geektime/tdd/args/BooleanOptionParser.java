package com.geektime.tdd.args;

import java.util.List;

class BooleanOptionParser implements OptionParser {
    @Override
    public Object parse(List<String> arguments, Option option) {
        int index = arguments.indexOf("-" + option.value());
        //如果只有一个-l，不加(index + 1) < arguments.size()这个，-l后面没有东西，这个是正常的
        //但是去取index+1的值，那就会越界。所以要确保index+1是在参数范围内的。不在参数范围那取值本来也是错误的
        if ((index + 1) < arguments.size() && !arguments.get(index + 1).startsWith("-")) throw new TooManyArgumentsException(option.value());
        return index != -1;
    }
}
