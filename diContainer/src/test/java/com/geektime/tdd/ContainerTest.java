package com.geektime.tdd;

import jakarta.inject.Inject;
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
            @Test
            public void should_bind_type_to_a_class_with_injection_constructor() throws Exception{
                Dependency dependency = new Dependency() {
                };
                context.bind(Component.class,ComponentWithInjectionConstructor.class);
                context.bind(Dependency.class,dependency);
                Component instance = context.get(Component.class);
                assertNotNull(instance);
                assertSame(dependency,((ComponentWithInjectionConstructor)instance).getDependency());
            }


            //TODO: A->B->C

            @Test
            public void should_bind_type_to_a_class_with_transitive_dependency() throws Exception{
                //这里是先把ComponentWithInjectionConstructor这个构造器带参数的注入了，然后在newInstance
                //的时候，去map(p -> get(p.getType()))这个get就是下面的context.bind(Dependency.class, DependencyWithInjectionConstructor.class);
                //这个bind进去的，放到map中去的那个，就可以获取到了，就可以创建对象了。注意，DependencyWithInjectionConstructor
                //ComponentWithInjectionConstructor这些都是通过反射创建的，都是能创建成功的，不是说接口，没办法创建。
                context.bind(Component.class,ComponentWithInjectionConstructor.class);
                context.bind(Dependency.class, DependencyWithInjectionConstructor.class);
                context.bind(String.class,"dependency String");
                Component instance = context.get(Component.class);
                assertNotNull(instance);
                Dependency dependency = ((ComponentWithInjectionConstructor) instance).getDependency();
                assertNotNull(dependency);
                assertEquals("dependency String",((DependencyWithInjectionConstructor)dependency).getDependency());

            }

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

interface Dependency{

}

class ComponentWithDefaultConstructor implements Component{
    public ComponentWithDefaultConstructor() {
    }
}
class ComponentWithInjectionConstructor implements Component{
    private Dependency dependency;
    @Inject
    public ComponentWithInjectionConstructor(Dependency dependency) {
        this.dependency = dependency;
    }

    public Dependency getDependency() {
        return dependency;
    }
}

class DependencyWithInjectionConstructor implements Dependency{
    String dependency;
    @Inject
    public DependencyWithInjectionConstructor(String dependency) {
        this.dependency = dependency;
    }

    public String getDependency() {
        return dependency;
    }
}