package com.geektime.tdd;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertSame;

class ContainerTest {
    interface Component{

    }


    @Nested
    public class ComponentConstruction{
        //TODO: instance
        @Test
        public void should_bind_type_to_a_specific_instance() throws Exception{
            Context context = new Context();

            Component instance = new Component() {
            };
            context.bind(Component.class,instance);
            //现在是失败的案例
            assertSame(instance,context.get(Component.class));

        }

        //TODO: abstract class
        //TODO: interface
        @Nested
        public class ConstructorInjection{
            //TODO: no args constructor
            //TODO: with dependencies
            //TODO: A->B->C

        }

        @Nested
        public class FieldInjection{

        }

        @Nested
        public class MethodInjection{

        }





    }

    @Nested
    public class DependenciesSelection{

    }

    @Nested
    public class LifecycleManagement{

    }
}