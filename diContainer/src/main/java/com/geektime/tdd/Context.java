package com.geektime.tdd;

import java.util.Optional;

public interface Context {
    <ComponentType> Optional<ComponentType> get(ComponentRef<ComponentType> ref);

}
