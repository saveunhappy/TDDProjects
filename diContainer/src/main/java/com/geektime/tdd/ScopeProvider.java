package com.geektime.tdd;

interface ScopeProvider {
    ComponentProvider<?> create(ComponentProvider<?> componentProvider);
}
