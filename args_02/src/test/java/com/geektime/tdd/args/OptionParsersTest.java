package com.geektime.tdd.args;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.annotation.Annotation;
import java.util.function.Function;

import static com.geektime.tdd.args.OptionParsersTest.BooleanOptionParserTest.option;
import static java.util.Arrays.*;
import static org.junit.jupiter.api.Assertions.*;

public class OptionParsersTest {
    @Nested
    class UnaryOptionParser {

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
            Function<String, Object> whatever = (it) -> null;
            Object defaultValue = new Object();
            assertSame(defaultValue, OptionParsers.unary(defaultValue, whatever).parse(asList(), option("-p")));
        }

        @Test//Happy path
        public void should_parse_value_if_flag_present() throws Exception {
            Object parsed = new Object();
            //String转换为Object
            Function<String, Object> parse = (it) -> parsed;
            Object whatever = new Object();
            //这个是测试正常情况，我们要测的是Function，所以默认值我们是不关心的，默认是就是whatever
            //经过function的函数，我们默认返回parsed,所以用assertSame，证明是返回了我们想要的
            assertSame(parsed, OptionParsers.unary(whatever, parse)
                    .parse(asList("-p", "8080"), option("p")));
        }
    }

    @Nested
    class BooleanOptionParserTest {


        @Test//Sad path
        public void should_not_accept_extra_argument_for_boolean_option() {
            TooManyArgumentsException e = assertThrows(TooManyArgumentsException.class,
                    () -> OptionParsers.bool().parse(asList("-l", "t"), option("l")));
            assertEquals("l", e.getOption());
        }

        @Test//Default value
        public void should_set_default_value_to_false_if_option_not_present() {
            assertFalse(OptionParsers.bool().parse(asList(), option("l")));
        }

        @Test//Happy path
        public void should_set_value_to_true_if_option_present() {
            assertTrue(OptionParsers.bool().parse(asList("-l"), option("l")));

        }

        static Option option(String value) {
            return new Option() {

                @Override
                public Class<? extends Annotation> annotationType() {
                    return Option.class;
                }

                @Override
                public String value() {
                    return value;
                }
            };
        }


    }

    @Nested
    class ListOptionParser {
        //TODO -g "this" "is" {"this","is"}
        @Test
        public void should_parse_list_value() throws Exception {
            String[] value = OptionParsers.list(String[]::new, String::valueOf)
                    .parse(asList("-g", "this", "is"), option("g"));
            assertArrayEquals(new String[]{"this","is"},value);
        }
        //TODO -default value []
        //TODO -d a throw exception  a不是数字，应该是数字的
    }

    static Option option(String value) {
        return new Option() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return Option.class;
            }

            @Override
            public String value() {
                return value;
            }
        };
    }
}
