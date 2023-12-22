package com.geektime.tdd;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Scope;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.util.*;
import java.util.function.Function;
import java.util.stream.IntStream;
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
            public void should_retrieve_empty_if_no_matched_qualifier() {
                config.bind(TestComponent.class, new TestComponent() {
                });
                Optional<TestComponent> component = config.getContext().get(ComponentRef.of(TestComponent.class, new SkywalkerLiteral()));
                assertTrue(component.isEmpty());
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
                    Arguments.of(Named.of("Provider in inject Method", MissingDependencyProviderMethod.class)),
                    Arguments.of(Named.of("Scoped", MissingDependencyScoped.class)),
                    Arguments.of(Named.of("Provider Scoped", MissingDependencyProviderScoped.class))
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

        @Singleton
        static class MissingDependencyScoped implements TestComponent {
            @Inject
            Dependency dependency;
        }

        @Singleton
        static class MissingDependencyProviderScoped implements TestComponent {
            @Inject
            Provider<Dependency> dependency;
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

        //        @Test
//        public void should_not_throw_exception_if_cyclic_dependency_via_other_provider() {
//            config.bind(TestComponent.class, CyclicComponentProviderConstructor.class);
//            config.bind(Dependency.class, CyclicDependencyProviderConstructor.class);
//            Context context = config.getContext();
//            assertTrue(context.get(ComponentRef.of(TestComponent.class)).isPresent());
//            CyclicComponentProviderConstructor component = (CyclicComponentProviderConstructor) context.get(ComponentRef.of(TestComponent.class)).get();
//            CyclicDependencyProviderConstructor dependency = (CyclicDependencyProviderConstructor) context.get(ComponentRef.of(Dependency.class)).get();
//            System.out.println(component.name);
//            System.out.println(dependency.name);
//        }
        @Nested
        class WithQualifier {

            @ParameterizedTest
            @MethodSource
            public void should_throw_exception_if_dependency_with_qualifier_not_found(Class<? extends TestComponent> component) {
                config.bind(Dependency.class, new Dependency() {
                });
//            config.bind(Dependency.class,Dependency.class,new SkywalkerLiteral());
                //bind的是TestComponent和@Named,那你取的时候也应该有TestComponent和@Named，
                // 在构造器中，参数是@SkyWalker (Dependency dependency),所以你应该bind一有注解的，这样才能找到
                //比如，config.bind(Dependency.class,Dependency.class,new SkywalkerLiteral());这样才能找到
                config.bind(TestComponent.class, component, new NamedLiteral("Owner"));
                DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () -> config.getContext());
                assertEquals(new Component(TestComponent.class, new NamedLiteral("Owner")), exception.getComponent());
                assertEquals(new Component(Dependency.class, new SkywalkerLiteral()), exception.getDependency());
            }

            public static Stream<Arguments> should_throw_exception_if_dependency_with_qualifier_not_found() {
                return Stream.of(
                        Named.of("Inject Constructor with Qualifier", InjectConstructor.class),
                        Named.of("Inject Field with Qualifier", InjectField.class),
                        Named.of("Inject Method with Qualifier", InjectMethod.class),
                        Named.of("Provider in Inject Constructor with Qualifier", InjectConstructorProvider.class),
                        Named.of("Provider in Inject Field with Qualifier", InjectFieldProvider.class),
                        Named.of("Provider in Inject Method with Qualifier", InjectMethodProvider.class)
                ).map(Arguments::of);
            }

            static class InjectConstructor implements TestComponent {
                @Inject
                public InjectConstructor(@SkyWalker Dependency dependency) {
                }
            }

            static class InjectField implements TestComponent {
                @Inject
                @SkyWalker
                Dependency dependency;
            }

            static class InjectMethod implements TestComponent {
                @Inject
                void install(@SkyWalker Dependency dependency) {
                }
            }

            static class InjectConstructorProvider implements TestComponent {
                @Inject
                public InjectConstructorProvider(@SkyWalker Provider<Dependency> dependency) {
                }
            }

            static class InjectFieldProvider {
                @Inject
                @SkyWalker
                Provider<Dependency> dependency;
            }

            static class InjectMethodProvider {
                @Inject
                void install(@SkyWalker Provider<Dependency> dependency) {
                }
            }

            @ParameterizedTest(name = "{1} -> @SkyWalker({0}) -> @Named(\"ChoseOne\") not cyclic dependencies")
            @MethodSource
            public void should_not_throw_cyclic_exception_if_component_with_same_type_tag_with_different_qualifier(Class<? extends Dependency> skywalker,
                                                                                                                   Class<? extends Dependency> notCyclic) {
                Dependency instance = new Dependency() {
                };
                //A->B->C(instance) 这个时候就不该有循环依赖，这个是正确的
                //A找B，B找C,C找到了，返回了，B完备了，然后A找B也找到了，A也完整了
                config.bind(Dependency.class, notCyclic);
                config.bind(Dependency.class, skywalker, new SkywalkerLiteral());
                config.bind(Dependency.class, instance, new NamedLiteral("ChoseOne"));

                assertDoesNotThrow(() -> config.getContext());
            }

            static class SkywalkerInjectConstructor implements Dependency {
                @Inject
                public SkywalkerInjectConstructor(@jakarta.inject.Named("ChoseOne") Dependency dependency) {
                }
            }

            static class SkywalkerInjectField implements Dependency {
                @Inject
                @jakarta.inject.Named("ChoseOne")
                Dependency dependency;
            }

            static class SkywalkerInjectMethod implements Dependency {
                @Inject
                void install(@jakarta.inject.Named("ChoseOne") Dependency dependency) {
                }
            }

            static class NotCyclicInjectConstructor implements Dependency {
                @Inject
                public NotCyclicInjectConstructor(@SkyWalker Dependency dependency) {
                }
            }


            static class NotCyclicInjectField implements Dependency {
                @Inject
                @SkyWalker
                Dependency dependency;
            }


            static class NotCyclicInjectMethod implements Dependency {
                @Inject
                public void install(@SkyWalker Dependency dependency) {
                }
            }


            public static Stream<Arguments> should_not_throw_cyclic_exception_if_component_with_same_type_tag_with_different_qualifier() {
                List<Arguments> arguments = new ArrayList<>();
                for (Named skywalker : List.of(Named.of("Inject Constructor", SkywalkerInjectConstructor.class),
                        Named.of("Inject Field", SkywalkerInjectField.class),
                        Named.of("Inject Method", SkywalkerInjectMethod.class)))
                    for (Named notCyclic : List.of(Named.of("Inject Constructor", NotCyclicInjectConstructor.class),
                            Named.of("Inject Constructor", NotCyclicInjectField.class),
                            Named.of("Inject Constructor", NotCyclicInjectMethod.class)))
                        arguments.add(Arguments.of(skywalker, notCyclic));
                return arguments.stream();
            }
            // dependency missing if qualifier not match

        }

        @Nested
        public class WithScope {
            static class NoSingleton {

            }

            @Test
            public void should_not_be_singleton_scope_by_default() {
                config.bind(NoSingleton.class, NoSingleton.class);
                Context context = config.getContext();
                assertNotSame(context.get(ComponentRef.of(NoSingleton.class)).get(), context.get(ComponentRef.of(NoSingleton.class)).get());
            }

            @Test
            public void should_bind_component_as_singleton() {
                config.bind(NoSingleton.class, NoSingleton.class, new SingletonLiteral());
                Context context = config.getContext();
                assertSame(context.get(ComponentRef.of(NoSingleton.class)).get(), context.get(ComponentRef.of(NoSingleton.class)).get());
            }

            @Singleton
            static class SingletonAnnotated implements Dependency {

            }

            @Test
            public void should_retrieve_scope_annotation_from_component() {
                config.bind(Dependency.class, SingletonAnnotated.class);
                Context context = config.getContext();
                assertSame(context.get(ComponentRef.of(Dependency.class)).get(), context.get(ComponentRef.of(Dependency.class)).get());
            }

            @Test
            public void should_bind_component_as_customize_scope() {
                config.scope(Pooled.class, PooledProvider::new);
                config.bind(NoSingleton.class, NoSingleton.class, new PooledLiteral());
                Context context = config.getContext();
                List<NoSingleton> instances = IntStream.range(0, 5).mapToObj(i -> context.get(ComponentRef.of(NoSingleton.class)).get()).toList();
                assertEquals(PooledProvider.MAX, new HashSet<>(instances).size());
            }

            @Test
            public void should_throw_exception_if_multi_scope_provided() {
                //这个就应该报错，因为你不能既是单例的又是池化的，只能选一个，否则Provider都不知道怎么选
                assertThrows(IllegalComponentException.class, () -> config.bind(NoSingleton.class, NoSingleton.class, new SingletonLiteral(), new PooledLiteral()));

            }

            //TODO multi scope annotated
            //TODO undefined scope
            @Nested
            public class WithQualifier {
                @Test
                public void should_not_be_singleton_scope_by_default() {
                    config.bind(NoSingleton.class, NoSingleton.class, new SkywalkerLiteral());
                    Context context = config.getContext();
                    assertNotSame(context.get(ComponentRef.of(NoSingleton.class, new SkywalkerLiteral())).get(), context.get(ComponentRef.of(NoSingleton.class, new SkywalkerLiteral())).get());
                }

                @Test
                public void should_bind_component_as_singleton() {
                    config.bind(NoSingleton.class, NoSingleton.class, new SingletonLiteral(), new SkywalkerLiteral());
                    Context context = config.getContext();
                    assertSame(context.get(ComponentRef.of(NoSingleton.class, new SkywalkerLiteral())).get(), context.get(ComponentRef.of(NoSingleton.class, new SkywalkerLiteral())).get());
                }

                @Test
                public void should_retrieve_scope_annotation_from_component() {
                    config.bind(Dependency.class, SingletonAnnotated.class, new SkywalkerLiteral());
                    Context context = config.getContext();
                    assertSame(context.get(ComponentRef.of(Dependency.class, new SkywalkerLiteral())).get(), context.get(ComponentRef.of(Dependency.class, new SkywalkerLiteral())).get());
                }
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
    public int hashCode() {
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

record SingletonLiteral() implements Singleton {
    @Override
    public Class<? extends Annotation> annotationType() {
        return Singleton.class;
    }
}

@Scope
@Documented
@Retention(RUNTIME)
@interface Pooled {
}

record PooledLiteral() implements Pooled {
    @Override
    public Class<? extends Annotation> annotationType() {
        return Pooled.class;
    }
}

class PooledProvider<T> implements ComponentProvider<T> {
    static int MAX = 2;
    private List<T> pool = new ArrayList<>();
    int current;
    private ComponentProvider<T> provider;

    public PooledProvider(ComponentProvider<T> provider) {
        this.provider = provider;
    }

    @Override
    public T get(Context context) {
        if (pool.size() < MAX) {
            pool.add(provider.get(context));
        }
        //最多添加两个，以后就是每次都循环获取，第0个，第1个，第0个，第1个，目前不知道有啥用
        return pool.get(current++ % MAX);
    }

    // 2297 2301
    @Override
    public List<ComponentRef<?>> getDependencies() {
        return provider.getDependencies();
    }
}

