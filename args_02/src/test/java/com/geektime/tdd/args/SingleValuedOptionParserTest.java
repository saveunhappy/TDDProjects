package com.geektime.tdd.args;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.function.Function;

import static com.geektime.tdd.args.BooleanOptionParserTest.option;
import static java.util.Arrays.*;
import static org.junit.jupiter.api.Assertions.*;

public class SingleValuedOptionParserTest {
    @Test//Sad path
    public void should_not_accept_extra_argument_for_single_valued_option() throws Exception {
        TooManyArgumentsException e = assertThrows(TooManyArgumentsException.class, () ->
                OptionParsers.unary(0, Integer::parseInt)
                        .parse(asList("-p", "8080", "8081"), option("p")));
        assertEquals("p", e.getOption());
    }

    @ParameterizedTest//Sad path
    @ValueSource(strings = {"-p -l", "-p"})
    public void should_not_accept_insufficient_argument_for_single_valued_option(String arguments) throws Exception {
        InsufficientException e = assertThrows(InsufficientException.class, () ->
                OptionParsers.unary(0, Integer::parseInt)
                        .parse(asList(arguments.split(" ")), option("p")));
        assertEquals("p", e.getOption());
    }

    @Test//Default value
    public void should_set_default_value_to_0_for_int_option() throws Exception {
        Function<String,Object> whatever = (it) -> null;
        Object defaultValue = new Object();
        assertSame(defaultValue, OptionParsers.unary(defaultValue, whatever).parse(asList(), option("-p")));
    }

    @Test//Happy path
    public void should_parse_value_if_flag_present() throws Exception {
        Object parsed = new Object();
        //String转换为Object
        Function<String,Object> parse = (it) -> parsed;
        Object whatever = new Object();
        //这个是测试正常情况，我们要测的是Function，所以默认值我们是不关心的，默认是就是whatever
        //经过function的函数，我们默认返回parsed,所以用assertSame，证明是返回了我们想要的
        assertSame(parsed, OptionParsers.unary(whatever, parse)
                .parse(asList("-p","8080"), option("p")));
    }

}
