package com.geektime.tdd.args;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ArgsTest {
    //option without value, -b
    @Test
    public void should_split_option_without_value() throws Exception{
        Map<String, String[]> args = Args.toMap("-b");
        assertEquals(1, args.size());
        assertArrayEquals(new String[]{}, args.get("b"));
    }
    //option with value,-p 8080
    //option with values -g this is a list
    //multi options


    @Test
    @Disabled
    public void should_split_args_to_map() throws Exception {
        Map<String, String[]> args = Args.toMap("-b", "-p", "8080", "-d", "/usr/logs");
        assertEquals(3, args.size());
        assertArrayEquals(new String[]{}, args.get("b"));
        assertArrayEquals(new String[]{"8080"}, args.get("p"));
        assertArrayEquals(new String[]{"/usr/logs"}, args.get("d"));
    }

    @Test
    @Disabled
    public void should_split_args_list_to_map() throws Exception {
        Map<String, String[]> args = Args.toMap("-g", "this", "is", "a", "list",
                "-d", "1", "2", "-3", "5");
        assertEquals(2, args.size());
        assertArrayEquals(new String[]{"this", "is", "a", "list"}, args.get("g"));
        assertArrayEquals(new String[]{"1", "2", "-3", "5"}, args.get("d"));
    }
}