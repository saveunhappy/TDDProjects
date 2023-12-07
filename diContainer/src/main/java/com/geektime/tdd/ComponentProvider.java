package com.geektime.tdd;

import java.util.List;

import static java.util.List.of;

interface ComponentProvider<T> {
    T get(Context context);

    default List<Context.Ref> getDependencies(){
        return of();
    }

}
