package com.geektime.tdd;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.*;

import static org.junit.Assert.*;

class ContainerTest {

    Context context;

    @BeforeEach
    public void setup(){
        context = new Context();
    }

    @Nested
    public class ComponentConstruction{
        //TODO: instance
        @Test
        public void should_bind_type_to_a_specific_instance() throws Exception{
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
            //TODO: no args constructor 无依赖的组件应该通过默认构造函数生成组件实例
            @Test
            public void should_bind_type_to_a_class_with_default_constructor() throws Exception{
                context.bind(Component.class,ComponentWithDefaultConstructor.class);
                //所以这个get的时候获取的就是ComponentWithDefaultConstructor的实例，传的是class,所以就是去newInstance
                Component instance = context.get(Component.class);
                assertNotNull(instance);
                //确保确实是根据ComponentWithDefaultConstructor这个Class通过newInstance构造的
                assertTrue(instance instanceof ComponentWithDefaultConstructor);

            }
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
interface Component{

}
class ComponentWithDefaultConstructor implements Component{
    public ComponentWithDefaultConstructor() {
    }
}