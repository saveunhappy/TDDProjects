package com.geektime.tdd;

import java.lang.reflect.Type;
import java.util.List;

import static java.util.List.of;

interface ComponentProvider<T> {
    T get(Context context);

    default List<Context.Ref> getDependenciesRef(){
        return of();
    }

}
