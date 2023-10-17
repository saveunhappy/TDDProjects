package com.geektime.tdd.args;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Args {
    public static Map<String, String[]> toMap(String... args) {

        Map<String, String[]> result = new HashMap<>();
        String option = null;
        List<String> values = new ArrayList<>();
        for (String arg : args) {
            if (arg.matches("^-[a-zA-z]+$")) {
                //-p
                option = arg;
            } else {
                //8080
                values.add(arg);
            }
        }
        result.put(option.substring(1),values.toArray(String[]::new));
        return result;
    }
}
