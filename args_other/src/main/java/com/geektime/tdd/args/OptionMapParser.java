package com.geektime.tdd.args;

public interface OptionMapParser<T> {
    T parse(String[] values);
}