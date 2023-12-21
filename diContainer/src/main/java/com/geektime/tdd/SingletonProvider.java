package com.geektime.tdd;

import java.util.List;

class SingletonProvider<T> implements ComponentProvider<T> {
    private T singleton;
    private ComponentProvider<T> provider;

    public SingletonProvider(ComponentProvider<T> provider) {
        this.provider = provider;
    }

    @Override
    public T get(Context context) {
        //第一次进入这里肯定是null，还是反射去创建，第二次进入就有值了，那么就取出来，就相当于是singleton了
        if (singleton == null) {
            singleton = provider.get(context);
        }
        return singleton;
    }

    @Override
    public List<ComponentRef<?>> getDependencies() {
        return provider.getDependencies();
    }
}
