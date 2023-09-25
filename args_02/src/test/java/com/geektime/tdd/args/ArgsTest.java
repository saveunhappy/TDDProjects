package com.geektime.tdd.args;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArgsTest {
    // -l -p 8080 -d /usr/logs
    //[-l],[-p,8080],[-d,/usr/logs]
    //{-l:[],-p:[8080],-d:[/usr/logs]}

    @Test
    public void should_parse_multi_options() throws Exception {
        MultiOptions options = Args.parse(MultiOptions.class, "-d", "/usr/logs", "-p", "8080", "-l");
        assertTrue(options.logging());
        assertEquals(8080, options.port());
        assertEquals("/usr/logs", options.directory());
    }

    @Test
    public void should_throw_illegal_option_exception_if_annotation_not_present() throws Exception {
        IllegalOptionException e = assertThrows(IllegalOptionException.class, () -> Args.parse(OptionWithoutAnnotation.class, "-d", "/usr/logs", "-p", "8080", "-l"));
        assertEquals("port",e.getParameter());
    }

    record OptionWithoutAnnotation(@Option("l") boolean logging, int port, @Option("d") String directory) {
    }
    //sad path:
    // -bool -l t / -l t f/
    // -int  -p/ -p 8080 8081
    // -string -d/ -d /usr/logs /usr/packages
    //default value
    // -bool : false
    // -int : 0
    // -string: ""

    @Test
    public void should_example2() throws Exception {
        ListOptions options = Args.parse(ListOptions.class, "-g", "this", "is", "a", "list", "-d", "1", "2", "-3", "5");
        assertArrayEquals(new String[]{"this", "is", "a", "list"}, options.group());
        assertArrayEquals(new int[]{1, 2, -3, 5}, options.decimals());

    }

    record MultiOptions(@Option("l") boolean logging, @Option("p") int port, @Option("d") String directory) {

    }

    record ListOptions(@Option("g") String[] group, @Option("d") int[] decimals) {

    }
}