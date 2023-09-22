package com.geektime.tdd.args;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;

import static com.geektime.tdd.args.BooleanOptionParserTest.option;
import static java.util.Arrays.*;
import static org.junit.jupiter.api.Assertions.*;

public class SingleValuedOptionParserTest {
    @Test
    public void should_not_accept_extra_argument_for_single_valued_option() throws Exception {
        TooManyArgumentsException e = assertThrows(TooManyArgumentsException.class, () ->
                new SingleValueOptionParser<>(Integer::parseInt)
                        .parse(asList("-p", "8080", "8081"), option("p")));
        assertEquals("p", e.getOption());
    }

    @ParameterizedTest
    @ValueSource(strings = {"-p -l", "-p"})
    public void should_not_accept_insufficient_argument_for_single_valued_option(String arguments) throws Exception {
        InsufficientException e = assertThrows(InsufficientException.class, () ->
                new SingleValueOptionParser<>(Integer::parseInt)
                        .parse(asList(arguments.split(" ")), option("p")));
        assertEquals("p", e.getOption());
    }

    @Test
    public void should_set_default_value_to_0_for_int_option() throws Exception {
        assertEquals(0, new SingleValueOptionParser<>(Integer::parseInt).parse(asList(), option("-p")));
    }
}
