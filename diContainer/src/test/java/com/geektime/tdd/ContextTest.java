package com.geektime.tdd;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.stream.Stream;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.*;

@Nested
public class ContextTest {
    ContextConfig config;

    @BeforeEach
    public void setup() {
        config = new ContextConfig();
    }

    @Nested
    class TypeBinding {
        @Test
        public void should_bind_type_to_a_specific_instance() {
            Component instance = new Component() {
            };
            config.bind(Component.class, instance);
            Context context = config.getContext();
            assertSame(instance, context.get(ComponentRef.of(Component.class)).get());

        }

        @ParameterizedTest(name = "supporting {0}")
        @MethodSource
        public void should_bind_type_to_an_injectable_component(Class<? extends Component> componentType) {
            Dependency dependency = new Dependency() {
            };
            config.bind(Dependency.class, dependency);
            config.bind(Component.class, componentType);

            Context context = config.getContext();
            Optional<Component> component = context.get(ComponentRef.of(Component.class));
            assertTrue(component.isPresent());
            assertSame(dependency, component.get().dependency());
        }

        public static Stream<Arguments> should_bind_type_to_an_injectable_component() {
            return Stream.of(Arguments.of(Named.of("Constructor injection", ConstructorInjection.class)),
                    Arguments.of(Named.of("Field injection", FieldInjection.class)),
                    Arguments.of(Named.of("Method injection", MethodInjection.class)));
        }

        interface Component {
            default Dependency dependency() {
                return null;
            }

        }

        interface AnotherDependency {

        }

        static class ConstructorInjection implements Component {
            Dependency dependency;

            @Inject
            public ConstructorInjection(Dependency dependency) {
                this.dependency = dependency;
            }

            @Override
            public Dependency dependency() {
                return dependency;
            }
        }

        static class FieldInjection implements Component {
            @Inject
            Dependency dependency;

            @Override
            public Dependency dependency() {
                return dependency;
            }
        }

        static class MethodInjection implements Component {
            Dependency dependency;

            @Inject
            void install(Dependency dependency) {
                this.dependency = dependency;
            }

            @Override
            public Dependency dependency() {
                return dependency;
            }
        }

        @Test
        public void should_retrieve_empty_for_unbind_type() {
            Context context = config.getContext();
            Optional<Component> component = context.get(ComponentRef.of(Component.class));
            assertTrue(component.isEmpty());
        }

        //Context
        @Test
        public void should_retrieve_bind_type_as_provider() {
            Component instance = new Component() {
            };
            config.bind(Component.class, instance);
            Context context = config.getContext();
            Provider<Component> provider = context.get(new ComponentRef<Provider<Component>>() {
            }).get();
            assertSame(instance, provider.get());
        }

        @Test
        public void should_retrieve_bind_type_as_unsupported_container() {
            Component instance = new Component() {
            };
            config.bind(Component.class, instance);
            Context context = config.getContext();

//            assertFalse(context.get(Context.Ref.of(type)).isPresent());
            //还是没有变，因为如果是List类型，不支持，返回的就是Optional.empty，isPresent就是false
            assertFalse(context.get(new ComponentRef<List<Component>>() {
            }).isPresent());
        }

        @Nested
        public class WithQualifier {
            @Test
            public void should_bind_instance_with_multi_qualifiers() {
                Component instance = new Component() {
                };
                //原来是要写成config.bind(Component.class,instance,@Named("ChosenOne"));但是java不允许，所以还是继承
                config.bind(Component.class, instance, new NamedLiteral("ChosenOne"), new SkywalkerLiteral());
                Context context = config.getContext();
                Component choseOne = context.get(ComponentRef.of(Component.class, new NamedLiteral("ChosenOne"))).get();
                Component skywalker = context.get(ComponentRef.of(Component.class, new SkywalkerLiteral())).get();
                assertSame(instance, choseOne);
                assertSame(instance, skywalker);
            }

            @Test
            public void should_bind_component_with_multi_qualifier() {
                Dependency dependency = new Dependency() {
                };
                config.bind(Dependency.class, dependency);
                config.bind(ConstructorInjection.class, ConstructorInjection.class, new NamedLiteral("ChosenOne"), new SkywalkerLiteral());
                Context context = config.getContext();
                ConstructorInjection choseOne = context.get(ComponentRef.of(ConstructorInjection.class, new NamedLiteral("ChosenOne"))).get();
                ConstructorInjection skywalker = context.get(ComponentRef.of(ConstructorInjection.class, new SkywalkerLiteral())).get();
                assertSame(dependency, choseOne.dependency);
                assertSame(dependency, skywalker.dependency);
            }

            @Test
            public void should_throw_exception_if_illegal_qualifier_given_to_instance() {
                Component instance = new Component() {
                };
                assertThrows(IllegalComponentException.class, () -> config.bind(Component.class, instance, new TestLiteral()));
            }

            @Test
            public void should_throw_exception_if_illegal_qualifier_given_to_component() {
                assertThrows(IllegalComponentException.class, () -> config.bind(ConstructorInjection.class, ConstructorInjection.class, new TestLiteral()));
            }
            //TODO Provider

        }
    }

    @Nested
    public class DependencyCheck {
        @ParameterizedTest
        @MethodSource
        public void should_throw_exception_if_dependency_not_found(Class<? extends TestComponent> component) {
            config.bind(TestComponent.class, component);
            DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class,
                    () -> config.getContext());
            //这个就是哪个组件找不到哪个依赖，具体看checkDependencies这个方法
            //Field和Method找不到可以去看getDependency()这个方法，它是把构造器的参数
            //字段，还有方法的参数都添加进去了，进行concat
            assertEquals(Dependency.class, exception.getDependency().type());
            assertEquals(TestComponent.class, exception.getComponent().type());

        }

        public static Stream<Arguments> should_throw_exception_if_dependency_not_found() {
            return Stream.of(Arguments.of(Named.of("inject Constructor", MissingDependencyConstructor.class)),
                    Arguments.of(Named.of("inject Field", MissingDependencyField.class)),
                    Arguments.of(Named.of("inject Method", MissingDependencyMethod.class)),
                    Arguments.of(Named.of("Provider in inject Constructor", MissingDependencyProviderConstructor.class)),
                    Arguments.of(Named.of("Provider in inject Field", MissingDependencyProviderField.class)),
                    Arguments.of(Named.of("Provider in inject Method", MissingDependencyProviderMethod.class))
            );
        }

        static class MissingDependencyConstructor implements TestComponent {
            @Inject
            public MissingDependencyConstructor(Dependency dependency) {

            }
        }

        static class MissingDependencyField implements TestComponent {
            @Inject
            Dependency dependency;
        }

        static class MissingDependencyMethod implements TestComponent {
            @Inject
            void install(Dependency dependency) {

            }
        }

        static class MissingDependencyProviderConstructor implements TestComponent {
            @Inject
            public MissingDependencyProviderConstructor(Provider<Dependency> dependency) {
            }
        }

        static class MissingDependencyProviderField implements TestComponent {
            @Inject
            Provider<Dependency> dependency;
        }

        static class MissingDependencyProviderMethod implements TestComponent {
            @Inject
            void install(Provider<Dependency> dependency) {
            }
        }

        @ParameterizedTest(name = "cyclic dependency between {0} and {1}")
        @MethodSource
        public void should_throw_exception_if_cyclic_dependencies_found(Class<? extends TestComponent> component,
                                                                        Class<? extends Dependency> dependency) {
            //注意，这里是使用的bind，那么排列组合的时候，先说构造器，先去找Component，找到依赖Dependency
            //然后先去实例化Dependency，发现Dependency依赖Component，循环依赖了。
            //如果是构造器和字段的组合呢？构造器依赖Dependency，Dependency的字段是Component，标注了@Inject
            //在checkDependency的时候还是会循环依赖
            config.bind(TestComponent.class, component);
            config.bind(Dependency.class, dependency);

            CyclicDependenciesFoundException exception = assertThrows(CyclicDependenciesFoundException.class,
                    () -> config.getContext());
            List<Class<?>> classes = Arrays.asList(exception.getComponents());
            assertEquals(2, classes.size());
            assertTrue(classes.contains(TestComponent.class));
            assertTrue(classes.contains(Dependency.class));
        }


        public static Stream<Arguments> should_throw_exception_if_cyclic_dependencies_found() {
            List<Arguments> arguments = new ArrayList<>();
            List<Named<? extends Class<? extends TestComponent>>> componentInjections =
                    List.of(Named.of("Inject Constructor", CyclicComponentInjectConstructor.class),
                            Named.of("Inject Field", CyclicComponentInjectField.class),
                            Named.of("Inject Method", CyclicComponentInjectMethod.class));
            List<Named<? extends Class<? extends Dependency>>> dependencyInjections =
                    List.of(Named.of("Inject Constructor", CyclicDependencyInjectConstructor.class),
                            Named.of("Inject Field", CyclicDependencyInjectField.class),
                            Named.of("Inject Method", CyclicDependencyInjectMethod.class));
            for (Named component : componentInjections) {
                for (Named dependency : dependencyInjections) {
                    //每次传入的参数就是一个Arguments，所以list里面这么多也是一个一个去依赖的
                    arguments.add(Arguments.of(component, dependency));
                }
            }
            return arguments.stream();
        }

        static class CyclicComponentInjectConstructor implements TestComponent {
            String name = "111";

            @Inject
            public CyclicComponentInjectConstructor(Dependency dependency) {
            }
        }

        static class CyclicComponentInjectField implements TestComponent {
            @Inject
            Dependency dependency;
        }

        static class CyclicComponentInjectMethod implements TestComponent {
            @Inject
            void inject(Dependency dependency) {

            }
        }

        static class CyclicDependencyInjectConstructor implements Dependency {
            @Inject
            public CyclicDependencyInjectConstructor(TestComponent component) {
            }
        }

        static class CyclicDependencyInjectField implements Dependency {
            @Inject
            TestComponent component;
        }

        static class CyclicDependencyInjectMethod implements Dependency {
            @Inject
            void inject(TestComponent component) {

            }
        }

        @ParameterizedTest(name = "indirect cyclic dependency between {0} and {1}")
        @MethodSource
        public void should_throw_exception_if_transitive_cyclic_dependencies_found(Class<? extends TestComponent> component,
                                                                                   Class<? extends Dependency> dependency,
                                                                                   Class<? extends AnotherDependency> anotherDependency) {
            config.bind(TestComponent.class, CyclicComponentInjectConstructor.class);
            config.bind(Dependency.class, IndirectCyclicDependencyInjectConstructor.class);
            config.bind(AnotherDependency.class, IndirectCyclicAnotherDependencyInjectConstructor.class);
            CyclicDependenciesFoundException exception = assertThrows(CyclicDependenciesFoundException.class,
                    () -> config.getContext());
            List<Class<?>> classes = Arrays.asList(exception.getComponents());
            assertEquals(3, classes.size());
            assertTrue(classes.contains(TestComponent.class));
            assertTrue(classes.contains(Dependency.class));
            assertTrue(classes.contains(AnotherDependency.class));
        }

        public static Stream<Arguments> should_throw_exception_if_transitive_cyclic_dependencies_found() {
            List<Arguments> arguments = new ArrayList<>();
            List<Named<? extends Class<? extends TestComponent>>> componentInjections =
                    List.of(Named.of("Inject Constructor", CyclicComponentInjectConstructor.class),
                            Named.of("Inject Field", CyclicComponentInjectField.class),
                            Named.of("Inject Method", CyclicComponentInjectMethod.class));
            List<Named<? extends Class<? extends Dependency>>> dependencyInjections =
                    List.of(Named.of("Inject Constructor", IndirectCyclicDependencyInjectConstructor.class),
                            Named.of("Inject Field", IndirectCyclicDependencyInjectField.class),
                            Named.of("Inject Method", IndirectCyclicDependencyInjectMethod.class));
            List<Named<? extends Class<? extends AnotherDependency>>> anotherDependencyInjections =
                    List.of(Named.of("Inject Constructor", IndirectCyclicAnotherDependencyInjectConstructor.class),
                            Named.of("Inject Field", IndirectCyclicAnotherDependencyInjectField.class),
                            Named.of("Inject Method", IndirectCyclicAnotherDependencyInjectMethod.class));
            for (Named component : componentInjections) {
                for (Named dependency : dependencyInjections) {
                    for (Named anotherDependency : anotherDependencyInjections) {
                        //每次传入的参数就是一个Arguments，所以list里面这么多也是一个一个去依赖的
                        arguments.add(Arguments.of(component, dependency, anotherDependency));
                    }
                }
            }
            return arguments.stream();
        }

        static class IndirectCyclicDependencyInjectConstructor implements Dependency {
            @Inject
            public IndirectCyclicDependencyInjectConstructor(AnotherDependency anotherDependency) {
            }
        }

        static class IndirectCyclicDependencyInjectField implements Dependency {
            @Inject
            AnotherDependency anotherDependency;
        }

        static class IndirectCyclicDependencyInjectMethod implements Dependency {
            @Inject
            void inject(AnotherDependency anotherDependency) {

            }
        }

        static class IndirectCyclicAnotherDependencyInjectConstructor implements AnotherDependency {
            @Inject
            public IndirectCyclicAnotherDependencyInjectConstructor(TestComponent component) {
            }
        }

        static class IndirectCyclicAnotherDependencyInjectField implements AnotherDependency {
            @Inject
            TestComponent component;
        }

        static class IndirectCyclicAnotherDependencyInjectMethod implements AnotherDependency {
            @Inject
            void inject(TestComponent component) {

            }
        }

        static class CyclicDependencyProviderConstructor implements Dependency {
            String name = "dependency";
            Provider<TestComponent> component;

            @Inject
            public CyclicDependencyProviderConstructor(Provider<TestComponent> component) {
                this.component = component;
            }
        }

        static class CyclicComponentProviderConstructor implements TestComponent {
            String name = "component";
            Provider<Dependency> dependency;

            @Inject
            public CyclicComponentProviderConstructor(Provider<Dependency> dependency) {
                this.dependency = dependency;
            }
        }

        @Test
        public void should_not_throw_exception_if_cyclic_dependency_via_provider() {
            config.bind(TestComponent.class, CyclicComponentInjectConstructor.class);
            config.bind(Dependency.class, CyclicDependencyProviderConstructor.class);
            Context context = config.getContext();
            assertTrue(context.get(ComponentRef.of(TestComponent.class)).isPresent());
        }

        @Test
        public void should_not_throw_exception_if_cyclic_dependency_via_other_provider() {
            config.bind(TestComponent.class, CyclicComponentProviderConstructor.class);
            config.bind(Dependency.class, CyclicDependencyProviderConstructor.class);
            Context context = config.getContext();
            assertTrue(context.get(ComponentRef.of(TestComponent.class)).isPresent());
            CyclicComponentProviderConstructor component = (CyclicComponentProviderConstructor) context.get(ComponentRef.of(TestComponent.class)).get();
            CyclicDependencyProviderConstructor dependency = (CyclicDependencyProviderConstructor) context.get(ComponentRef.of(Dependency.class)).get();
            System.out.println(component.name);
            System.out.println(dependency.name);
        }

        @Nested
        public class WithQualifier {
            @Test
            public void should_throw_exception_if_dependency() {
                config.bind(Dependency.class, new Dependency() {
                });
                config.bind(InjectConstructor.class, InjectConstructor.class, new NamedLiteral("Owner"));
                DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () -> config.getContext());
                assertEquals(new Component(InjectConstructor.class, new NamedLiteral("Owner")), exception.getComponent());
                assertEquals(new Component(Dependency.class, new SkywalkerLiteral()), exception.getDependency());
            }

            static class InjectConstructor {
                @Inject
                public InjectConstructor(@SkyWalker Dependency dependency) {
                }

            }
            //  check cyclic dependencies with qualifier
            // A -> A 这个是循环依赖，但是A -> @Skywalker A就不是循环依赖了，因为现在注解也是bind的一部分，
            // 就是说现在注解和这个类一起组合作为key，那么你就不应该找到

            // A -> @Skywalker A -> @Named A   传递依赖比较多，直接用这个
            static class SkywalkerDependency implements Dependency {
                @Inject
                public SkywalkerDependency(@jakarta.inject.Named("ChosenOne") Dependency dependency) {
                }
            }

            static class NotCyclicDependency implements Dependency {
                @Inject
                public NotCyclicDependency(@SkyWalker Dependency dependency) {

                }
            }

            @Test
            public void should_not_throw_cyclic_exception_if_component_with_same_type_tag_with_different_qualifier() {
                Dependency instance = new Dependency() {
                };
                //A->B->C(instance) 这个时候就不该有循环依赖，这个是正确的
                config.bind(Dependency.class, NotCyclicDependency.class);
                config.bind(Dependency.class, SkywalkerDependency.class, new SkywalkerLiteral());
                config.bind(Dependency.class, instance, new NamedLiteral("ChosenOne"));
                assertDoesNotThrow(() -> config.getContext());

            }
        }
    }


}

record NamedLiteral(String value) implements jakarta.inject.Named {
    @Override
    public Class<? extends Annotation> annotationType() {
        return jakarta.inject.Named.class;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof jakarta.inject.Named named) return Objects.equals(value, named.value());
        return false;
    }
    @Override
    public int hashCode(){
        return "value".hashCode() * 127 ^ value.hashCode();
    }

}

@java.lang.annotation.Documented
@java.lang.annotation.Retention(RUNTIME)
@jakarta.inject.Qualifier
@interface SkyWalker {
}

record SkywalkerLiteral() implements SkyWalker {
    @Override
    public Class<? extends Annotation> annotationType() {
        return SkyWalker.class;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SkyWalker;
    }
}

record TestLiteral() implements Test {
    @Override
    public Class<? extends Annotation> annotationType() {
        return Test.class;
    }
}


