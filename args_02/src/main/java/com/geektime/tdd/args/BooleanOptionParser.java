package com.geektime.tdd.args;

import java.util.List;

class BooleanOptionParser implements OptionParser<Boolean> {

    private BooleanOptionParser() {

    }

    @Override
    public Boolean parse(List<String> arguments, Option option) {
//        int index = arguments.indexOf("-" + option.value());
//        //如果没有找到这个flag,默认就是false
//        if(index == -1) return false;
//        //把SingleValue的方法变成默认的，就是包内可见的，这样就可以调用了，
//        List<String> values = SingleValueOptionParser.values(arguments, index);
//        //如果-l test,这样就是多参数了，就是错的
//        if (values.size() > 0) throw new TooManyArgumentsException(option.value());
//        //没有找到就是false,参数多了就是报错，走到这里，肯定就是true了。
//        return SingleValueOptionParser.values(arguments,option,0).map(it->true).orElse(false);
        /* 看看期待存在0个能不能得到值，如果得到了那就是true，不存在就是false*/
        return SingleValueOptionParser.values(arguments, option, 0).isPresent();

    }
}
