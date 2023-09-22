package com.geektime.tdd.args;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static com.geektime.tdd.args.BooleanOptionParserTest.option;
import static java.util.Arrays.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SingleValuedOptionParserTest {
    @Test
    public void should_not_accept_extra_argument_for_single_valued_option() throws Exception{
        TooManyArgumentsException e = assertThrows(TooManyArgumentsException.class,()->
                new SingleValueOptionParser<>(Integer::parseInt)
                        .parse(asList("-p","8080","8081"),option("p")));
        assertEquals("p",e.getOption());
    }
}
