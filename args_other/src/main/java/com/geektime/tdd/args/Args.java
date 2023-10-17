package com.geektime.tdd.args;

import java.util.HashMap;
import java.util.Map;

public class Args {
    public static Map<String,String[]> toMap(String...args){

        Map<String,String[]> result = new HashMap<>();
        for (String arg : args) {
            if(arg.matches("^-[a-zA-z]+$")){
                result.put(arg.substring(1),new String[]{});
            }
        }
        return result;
    }
}
