package com.geektime.tdd;

import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;

class ContainerTest {

    ContextConfig config;

    @BeforeEach
    public void setup() {
        config = new ContextConfig();
    }

    @Nested
    public class DependenciesSelection {



        @Nested
        public class Qualifier {

        }
    }

    @Nested
    public class LifecycleManagement {

    }
}


interface Component {
    default Dependency dependency() {
        return null;
    }

    ;
}

interface Dependency {

}

interface AnotherDependency {

}


