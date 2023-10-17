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
                if(option != null) {
                    //如果这个option不是空的就添加进去，之前是直接在最后添加，所以只有一个key
                    //这个时候就有的话就添加一个，不会之前的时候最后才添加一个key。
                    result.put(option.substring(1),values.toArray(String[]::new));
                }
                option = arg;
                //values需要清空，解析一次弄一次
                values = new ArrayList<>();
            } else {
                //8080
                values.add(arg);
            }
        }
        result.put(option.substring(1),values.toArray(String[]::new));
        return result;
    }
}
