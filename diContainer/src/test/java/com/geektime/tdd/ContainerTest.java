package com.geektime.tdd;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContainerTest {

    ContextConfig config;

    @BeforeEach
    public void setup() {
        config = new ContextConfig();
    }

    @Nested
    public class ComponentConstruction {
        //TODO: instance
        @Test
        public void should_bind_type_to_a_specific_instance() throws Exception {
            Component instance = new Component() {
            };
            config.bind(Component.class, instance);
            Context context = config.getContext();
            assertSame(instance, context.get(Component.class).get());

        }

        //TODO: abstract class
        //TODO: interface

        @Test
        public void should_return_empty_if_component_not_defined() throws Exception {
            Optional<Component> component = config.getContext().get(Component.class);
            assertTrue(component.isEmpty());
        }

        @Nested
        public class ConstructorInjection {
            //TODO: no args constructor 无依赖的组件应该通过默认构造函数生成组件实例
            @Test
            public void should_bind_type_to_a_class_with_default_constructor() throws Exception {
                config.bind(Component.class, ComponentWithDefaultConstructor.class);
                Component instance = config.getContext().get(Component.class).get();
                assertNotNull(instance);
                //确保确实是根据ComponentWithDefaultConstructor这个Class通过newInstance构造的
                assertTrue(instance instanceof ComponentWithDefaultConstructor);

            }

            //TODO: with dependencies
            @Test
            public void should_bind_type_to_a_class_with_injection_constructor() throws Exception {
                Dependency dependency = new Dependency() {
                };
                config.bind(Component.class, ComponentWithInjectionConstructor.class);
                config.bind(Dependency.class, dependency);
                Component instance = config.getContext().get(Component.class).get();
                assertNotNull(instance);
                assertSame(dependency, ((ComponentWithInjectionConstructor) instance).getDependency());
            }


            //TODO: A->B->C

            @Test
            public void should_bind_type_to_a_class_with_transitive_dependency() throws Exception {
                //这里是先把ComponentWithInjectionConstructor这个构造器带参数的注入了，然后在newInstance
                //的时候，去map(p -> get(p.getType()))这个get就是下面的context.bind(Dependency.class, DependencyWithInjectionConstructor.class);
                //这个bind进去的，放到map中去的那个，就可以获取到了，就可以创建对象了。注意，DependencyWithInjectionConstructor
                //ComponentWithInjectionConstructor这些都是通过反射创建的，都是能创建成功的，不是说接口，没办法创建。
                config.bind(Component.class, ComponentWithInjectionConstructor.class);
                config.bind(Dependency.class, DependencyWithInjectionConstructor.class);
                config.bind(String.class, "dependency String");
                Component instance = config.getContext().get(Component.class).get();
                assertNotNull(instance);
                Dependency dependency = ((ComponentWithInjectionConstructor) instance).getDependency();
                assertNotNull(dependency);
                assertEquals("dependency String", ((DependencyWithInjectionConstructor) dependency).getDependency());

            }
            //TODO multi inject constructors

            @Test
            public void should_throw_exception_if_multi_inject_constructors_provided() throws Exception {
                assertThrows(IllegalComponentException.class, () -> {
                    config.bind(Component.class, ComponentWithMultiInjectionConstructor.class);
                });
            }

            //TODO no default constructor and inject constructor
            @Test
            public void should_throw_exception_if_no_inject_nor_default_constructor_provided() throws Exception {
                assertThrows(IllegalComponentException.class, () ->
                        config.bind(Component.class, ComponentWithoutInjectionConstructorNorDefaultConstructor.class));
            }

            //TODO dependency not exist
            @Test
            public void should_throw_exception_if_dependency_not_found() throws Exception {
                config.bind(Component.class, ComponentWithInjectionConstructor.class);
                DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () ->
                        config.getContext());
                assertEquals(Dependency.class, exception.getDependency());
                assertEquals(Component.class, exception.getComponent());

            }

            @Test
            public void should_throw_exception_if_transitive_dependency_not_found() throws Exception {
                config.bind(Component.class, ComponentWithInjectionConstructor.class);
                config.bind(Dependency.class, DependencyWithInjectionConstructor.class);
                DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () ->
                        config.getContext());
                assertEquals(String.class, exception.getDependency());
                assertEquals(Dependency.class, exception.getComponent());

            }

            @Test
            public void should_throw_exception_if_cyclic_dependencies_found() throws Exception {
                config.bind(Component.class, ComponentWithInjectionConstructor.class);
                config.bind(Dependency.class, DependencyDependedOnComponent.class);

                CyclicDependenciesFoundException exception = assertThrows(CyclicDependenciesFoundException.class,
                        () -> config.getContext());
                List<Class<?>> classes = Arrays.asList(exception.getComponents());
                assertEquals(2, classes.size());
                assertTrue(classes.contains(Component.class));
                assertTrue(classes.contains(Dependency.class));
            }

            @Test
            public void should_throw_exception_if_transitive_cyclic_dependencies_found() throws Exception {
                config.bind(Component.class, ComponentWithInjectionConstructor.class);
                config.bind(Dependency.class, DependencyDependedOnAnotherDependency.class);
                config.bind(AnotherDependency.class, AnotherDependencyDependedOnComponent.class);
                CyclicDependenciesFoundException exception = assertThrows(CyclicDependenciesFoundException.class,
                        () -> config.getContext());
                List<Class<?>> classes = Arrays.asList(exception.getComponents());
                assertEquals(3, classes.size());
                assertTrue(classes.contains(Component.class));
                assertTrue(classes.contains(Dependency.class));
                assertTrue(classes.contains(AnotherDependency.class));

            }
        }

        @Nested
        public class FieldInjection {
            class ComponentWithFieldInjection {
                @Inject
                Dependency dependency;
            }

            //TODO inject field
            @Test
            public void should_inject_dependency_via_field() {
                Dependency dependency = new Dependency() {
                };
                config.bind(Dependency.class, dependency);
                config.bind(ComponentWithFieldInjection.class, ComponentWithFieldInjection.class);

                ComponentWithFieldInjection component = config.getContext().get(ComponentWithFieldInjection.class).get();
                assertSame(dependency, component.dependency);

            }

            @Test
            public void should_create_component_with_injection_field() {
                Context context = mock(Context.class);
                Dependency dependency = mock(Dependency.class);
                when(context.get(eq(Dependency.class))).thenReturn(Optional.of(dependency));
                //这样只会就会找到@Inject标注的方法，目前是只有方法，
                ConstructorInjectionProvider<ComponentWithFieldInjection> provider = new ConstructorInjectionProvider<>(ComponentWithFieldInjection.class);
                //get的时候应该能获取到对应的依赖，因为里面有Dependency的方法，如果依赖没有那么也是就报错了，所以这里是最终要实现的
                //根据字段注入
                ComponentWithFieldInjection component = provider.get(context);
                assertSame(dependency, component.dependency);
            }
            //TODO throw exception if field is final


            //TODO throw exception if dependency not found
            //TODO throw exception if cyclic dependency
            //TODO provided dependency information for field injection


            @Test
            public void should_throw_exception_when_field_dependency_missing() {
                //端到端的方式
                config.bind(ComponentWithFieldInjection.class, ComponentWithFieldInjection.class);
                assertThrows(DependencyNotFoundException.class, () -> config.getContext());
            }

            @Test
            public void should_include_field_dependency_in_dependencies() {
                //类的测试，
                ConstructorInjectionProvider<ComponentWithFieldInjection> provider = new ConstructorInjectionProvider<>(ComponentWithFieldInjection.class);
                //注意看getDependency()的实现，就是根据Constructor的参数是什么类型就添加到这个List中去
                //为什么要这样写测试？因为如果测试这个ConstructorInjectionProvider的实现里面没有去抛出
                //循环依赖或者依赖找不到的，我们只能知道他的依赖是什么，就是构造器的参数，所以循环依赖和
                //依赖找不到只能去使用这个
                assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependency().toArray());
            }

            class DependencyWithFieldInjection implements Dependency {
                @Inject
                ComponentWithFieldInjection component;
            }

            @Test
            public void should_throw_exception_when_field_has_cyclic_dependencies() {
                config.bind(ComponentWithFieldInjection.class, ComponentWithFieldInjection.class);
                config.bind(Dependency.class, DependencyWithFieldInjection.class);
                assertThrows(CyclicDependenciesFoundException.class, () -> config.getContext());
            }
            @Test
            public void should_throw_exception_when_field_has_cyclic_dependencies_() {
                ConstructorInjectionProvider<ComponentWithFieldInjection> provider = new ConstructorInjectionProvider<>(ComponentWithFieldInjection.class);
                //注意看getDependency()的实现，就是根据Constructor的参数是什么类型就添加到这个List中去
                //为什么要这样写测试？因为如果测试这个ConstructorInjectionProvider的实现里面没有去抛出
                //循环依赖或者依赖找不到的，我们只能知道他的依赖是什么，就是构造器的参数，所以循环依赖和
                //依赖找不到只能去使用这个
                assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependency().toArray());
            }

        }

        @Nested
        public class MethodInjection {


        }


    }

    @Nested
    public class DependenciesSelection {

    }

    @Nested
    public class LifecycleManagement {

    }
}


interface Component {

}

interface Dependency {

}

interface AnotherDependency {

}

class ComponentWithDefaultConstructor implements Component {
    public ComponentWithDefaultConstructor() {
    }
}

class ComponentWithInjectionConstructor implements Component {
    private Dependency dependency;

    @Inject
    public ComponentWithInjectionConstructor(Dependency dependency) {
        this.dependency = dependency;
    }

    public Dependency getDependency() {
        return dependency;
    }
}

class ComponentWithMultiInjectionConstructor implements Component {
    @Inject
    public ComponentWithMultiInjectionConstructor(String name, Double value) {

    }

    @Inject
    public ComponentWithMultiInjectionConstructor(String name) {

    }
}


class ComponentWithoutInjectionConstructorNorDefaultConstructor implements Component {
    public ComponentWithoutInjectionConstructorNorDefaultConstructor(String name) {

    }
}

class DependencyWithInjectionConstructor implements Dependency {
    String dependency;

    @Inject
    public DependencyWithInjectionConstructor(String dependency) {
        this.dependency = dependency;
    }

    public String getDependency() {
        return dependency;
    }
}

class DependencyDependedOnComponent implements Dependency {
    private Component component;

    @Inject
    public DependencyDependedOnComponent(Component component) {
        this.component = component;
    }
}

class AnotherDependencyDependedOnComponent implements AnotherDependency {

    private Component component;

    @Inject
    public AnotherDependencyDependedOnComponent(Component component) {
        this.component = component;
    }
}

class DependencyDependedOnAnotherDependency implements Dependency {
    private AnotherDependency anotherDependency;

    @Inject
    public DependencyDependedOnAnotherDependency(AnotherDependency anotherDependency) {
        this.anotherDependency = anotherDependency;
    }
}