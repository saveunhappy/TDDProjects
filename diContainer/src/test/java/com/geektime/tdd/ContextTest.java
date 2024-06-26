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
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.*;

@Nested
class ContextTest {

    private ContextConfig config;
    TestComponent instance;
    Dependency dependency;

    @BeforeEach
    void setUp() {
        config = new ContextConfig();
        instance = new TestComponent() {
        };
        dependency = new Dependency() {
        };
    }


    @Nested
    class TypeBindingTest {
        @Test
        void should_bind_type_a_specific_instance() {

            config.instance(TestComponent.class, instance);
            assertSame(instance, config.getContext().get(ComponentRef.of(TestComponent.class)).get());
        }

        @ParameterizedTest(name = "supporting {0}")
        @MethodSource
        void should_bind_type_an_injectable_component(Class<? extends TestComponent> componentType) {
            config.instance(Dependency.class, dependency);
            config.component(TestComponent.class, componentType);
            Optional<TestComponent> component = config.getContext().get(ComponentRef.of(TestComponent.class));
            assertTrue(component.isPresent());
            assertSame(dependency, component.get().dependency());
        }

        public static Stream<Arguments> should_bind_type_an_injectable_component() {
            return Stream.of(Arguments.of(Named.of("Constructor Injection", ConstructionInjection.class)),
                    Arguments.of(Named.of("Field Injection", FiledInjection.class)),
                    Arguments.of(Named.of("Method Injection", MethodInjection.class)));
        }

        static class ConstructionInjection implements TestComponent {
            private Dependency dependency;

            @Inject
            public ConstructionInjection(Dependency dependency) {
                this.dependency = dependency;
            }

            @Override
            public Dependency dependency() {
                return dependency;
            }
        }

        static class FiledInjection implements TestComponent {
            @Inject
            Dependency dependency;

            @Override
            public Dependency dependency() {
                return dependency;
            }
        }

        static class MethodInjection implements TestComponent {
            private Dependency dependency;

            @Inject
            public void install(Dependency dependency) {
                this.dependency = dependency;
            }


            @Override
            public Dependency dependency() {
                return dependency;
            }
        }

        @Test
        void should_return_empty_if_component_does_not_exist() {
            assertTrue(config.getContext().get(ComponentRef.of(TestComponent.class)).isEmpty());
        }

        @Test
        void should_retrieve_bind_type_as_provider() {
            config.instance(TestComponent.class, instance);

            Provider<TestComponent> provider = config.getContext().get(new ComponentRef<Provider<TestComponent>>() {
            }).get();
            assertSame(instance, provider.get());
        }

        @Test
        void should_not_retrieve_bind_type_as_unsupported_container() {
            config.instance(TestComponent.class, instance);


            assertFalse(config.getContext().get(new ComponentRef<List<TestComponent>>() {
            }).isPresent());
        }

        @Test
        void should_throw_exception_if_bind_duplicate_component_by_instance() {
            config.instance(TestComponent.class, instance);
            TestComponent another = new TestComponent() {
            };
            assertThrows(ContextConfig.ContextConfigException.class, () -> config.instance(TestComponent.class, another));
        }

        @Test
        void should_throw_exception_if_bind_duplicate_component_by_component() {
            config.component(TestComponent.class, ConstructionInjection.class);
            assertThrows(ContextConfig.ContextConfigException.class, () -> config.component(TestComponent.class, MethodInjection.class));
        }

        @Nested
        class WithQualifier {
            @Test
            void should_bind_instance_with_multi_qualifiers() {

                config.instance(TestComponent.class, instance, new NamedLiteral("ChosenOne"), new SkywalkerLiteral());
                TestComponent chosenOne = config.getContext().get(ComponentRef.of(TestComponent.class, new NamedLiteral("ChosenOne"))).get();
                TestComponent skywalker = config.getContext().get(ComponentRef.of(TestComponent.class, new SkywalkerLiteral())).get();
                assertSame(instance, chosenOne);
                assertSame(instance, skywalker);
            }


            @Test
            void should_bind_component_with_multi_qualifiers() {
                config.instance(Dependency.class, dependency);
                config.component(TestComponent.class, ConstructionInjection.class, new NamedLiteral("ChosenOne"), new SkywalkerLiteral());

                TestComponent chosenOne = config.getContext().get(ComponentRef.of(TestComponent.class, new NamedLiteral("ChosenOne"))).get();
                TestComponent skywalker = config.getContext().get(ComponentRef.of(TestComponent.class, new SkywalkerLiteral())).get();
                assertSame(dependency, chosenOne.dependency());
                assertSame(dependency, skywalker.dependency());
            }

            @Test
            void should_retrieve_bind_type_as_provider() {
                config.instance(TestComponent.class, instance, new NamedLiteral("ChosenOne"), new SkywalkerLiteral());

                Provider<TestComponent> provider = config.getContext().get(new ComponentRef<Provider<TestComponent>>(new SkywalkerLiteral()) {
                }).get();
                assertSame(instance, provider.get());
            }

            @Test
            void should_retrieve_empty_if_no_matched_qualifiers() {
                config.instance(TestComponent.class, instance);

                Optional<Provider<TestComponent>> component = config.getContext().get(new ComponentRef<Provider<TestComponent>>(new SkywalkerLiteral()) {
                });
                assertTrue(component.isEmpty());
            }

            @Test
            void should_throw_exception_if_illegal_qualifier_given_to_instance() {
                assertThrows(ContextConfig.ContextConfigException.class, () -> config.instance(TestComponent.class, instance, new TestLiteral()));
            }

            @Test
            void should_throw_exception_if_illegal_qualifier_given_to_component() {
                ContextConfig.ContextConfigException exception = assertThrows(ContextConfig.ContextConfigException.class, () -> config.component(TestComponent.class, InjectionTest.ConstructorInjectionTest.InjectConstructor.class, new TestLiteral()));
                assertEquals("Unqualified annotations: TestLiteral[] of class com.geektime.tdd.InjectionTest$ConstructorInjectionTest$InjectConstructor",
                        exception.getMessage());

            }
        }
    }

    @Nested
    class WithScope {
        static class NotSingleton implements TestComponent {
        }

        @Singleton
        static class SingletonScope implements TestComponent {
        }

        @Test
        void should_not_singleton_scope_by_default() {
            config.component(TestComponent.class, NotSingleton.class);
            Context context = config.getContext();
            assertNotSame(context.get(ComponentRef.of(TestComponent.class)).get(), context.get(ComponentRef.of(TestComponent.class)).get());
        }

        @Test
        void should_bind_component_with_customize_scope() {
            config.scope(Pooled.class, PooledProvider::new);
            config.component(TestComponent.class, NotSingleton.class, new PooledLiteral());
            Context context = config.getContext();
            Set<TestComponent> components = IntStream.range(0, 5).mapToObj(i -> context.get(ComponentRef.of(TestComponent.class)).get()).collect(Collectors.toSet());
            assertEquals(PooledProvider.MAX, components.size());
        }

        @Test
        void should_bind_component_as_singleton_scoped() {
            config.component(TestComponent.class, NotSingleton.class, new SingletonLiteral());
            Context context = config.getContext();
            assertSame(context.get(ComponentRef.of(TestComponent.class)).get(), context.get(ComponentRef.of(TestComponent.class)).get());
        }

        @Singleton
        static class SingletonAnnotated implements TestComponent {
        }

        @Test
        void should_retrieve_scope_annotation_from_component() {
            config.component(TestComponent.class, SingletonAnnotated.class);
            Context context = config.getContext();
            assertSame(context.get(ComponentRef.of(TestComponent.class)).get(), context.get(ComponentRef.of(TestComponent.class)).get());

        }

        @Test
        void should_throw_exception_if_multi_scope_provided() {
            ContextConfig.ContextConfigException exception = assertThrows(ContextConfig.ContextConfigException.class, () -> config.component(TestComponent.class, NotSingleton.class, new SingletonLiteral(), new PooledLiteral()));
            assertEquals("Unqualified annotations: SingletonLiteral[] , PooledLiteral[] of class com.geektime.tdd.ContextTest$WithScope$NotSingleton",
                    exception.getMessage());
        }

        @Singleton
        @Pooled
        static class MultiScopeAnnotation implements TestComponent {

        }

        @Test
        void should_throw_exception_if_multi_scope_annotated() {
            ContextConfig.ContextConfigException exception = assertThrows(ContextConfig.ContextConfigException.class, () -> config.component(TestComponent.class, MultiScopeAnnotation.class));
            assertEquals("Unqualified annotations: @jakarta.inject.Singleton() , @com.geektime.tdd.Pooled() of class com.geektime.tdd.ContextTest$WithScope$MultiScopeAnnotation",
                    exception.getMessage());
        }


        @Test
        void should_throw_exception_if_scope_undefined() {
            ContextConfig.ContextConfigException exception = assertThrows(ContextConfig.ContextConfigException.class, () -> config.component(TestComponent.class, NotSingleton.class, new PooledLiteral()));
            assertEquals("Unknown scope: interface com.geektime.tdd.Pooled",
                    exception.getMessage());
        }

        @Nested
        class WithQualifier {

            @Test
            void should_not_singleton_scope_by_default() {
                config.component(TestComponent.class, NotSingleton.class, new SkywalkerLiteral());
                Context context = config.getContext();
                assertNotSame(context.get(ComponentRef.of(TestComponent.class, new SkywalkerLiteral())).get(), context.get(ComponentRef.of(TestComponent.class, new SkywalkerLiteral())).get());
            }


            @Test
            void should_bind_component_as_singleton_scoped() {
                config.component(TestComponent.class, NotSingleton.class, new SingletonLiteral(), new SkywalkerLiteral());
                Context context = config.getContext();
                assertSame(context.get(ComponentRef.of(TestComponent.class, new SkywalkerLiteral())).get(), context.get(ComponentRef.of(TestComponent.class, new SkywalkerLiteral())).get());
            }

            @Test
            void should_retrieve_scope_annotation_from_component() {
                config.component(TestComponent.class, SingletonAnnotated.class, new SkywalkerLiteral());
                Context context = config.getContext();
                assertSame(context.get(ComponentRef.of(TestComponent.class, new SkywalkerLiteral())).get(), context.get(ComponentRef.of(TestComponent.class, new SkywalkerLiteral())).get());

            }


        }

    }

    @Nested
    class DependencyCheckTest {

        @ParameterizedTest
        @MethodSource
        void should_throw_exception_if_dependency_not_found(Class<TestComponent> component) {
            config.component(TestComponent.class, component);
            assertThrows(ContextConfig.ContextConfigError.class,
                    () -> config.getContext());
        }

        public static Stream<Arguments> should_throw_exception_if_dependency_not_found() {
            return Stream.of(
                    Arguments.of(Named.of("Inject Constructor", MissingDependencyConstructor.class)),
                    Arguments.of(Named.of("Inject Field", MissingDependencyField.class)),
                    Arguments.of(Named.of("Inject Method", MissingDependencyMethod.class)),
                    Arguments.of(Named.of("Provider in Inject Constructor", MissingDependencyProviderConstructor.class)),
                    Arguments.of(Named.of("Provider in Inject Field", MissingDependencyProviderField.class)),
                    Arguments.of(Named.of("Provider in Inject Method", MissingDependencyProviderMethod.class)),
                    Arguments.of(Named.of("Scoped", MissingDependencyScoped.class)),
                    Arguments.of(Named.of("Provider Scoped", MissingDependencyProviderScoped.class)));
        }

        static class MissingDependencyConstructor implements TestComponent {
            private Dependency dependency;

            @Inject
            public MissingDependencyConstructor(final Dependency dependency) {
            }
        }

        static class MissingDependencyField implements TestComponent {
            @Inject
            private Dependency dependency;
        }

        static class MissingDependencyMethod implements TestComponent {
            private Dependency dependency;

            @Inject
            public void install(final Dependency dependency) {
            }
        }

        static class MissingDependencyProviderConstructor implements TestComponent {
            @Inject
            public MissingDependencyProviderConstructor(Provider<Dependency> dependencyProvider) {
            }
        }

        static class MissingDependencyProviderField implements TestComponent {
            @Inject
            Provider<Dependency> dependencyProvider;
        }


        static class MissingDependencyProviderMethod implements TestComponent {
            @Inject
            void install(Provider<Dependency> dependencyProvider) {

            }
        }


        @Singleton
        static class MissingDependencyScoped implements TestComponent {
            @Inject
            void install(Dependency dependencyProvider) {

            }
        }


        @Singleton
        static class MissingDependencyProviderScoped implements TestComponent {
            @Inject
            void install(Provider<Dependency> dependencyProvider) {

            }
        }


        @ParameterizedTest(name = "cyclic dependency between {0} and {1}")
        @MethodSource
        void should_throw_exception_if_cycle_dependencies_found(Class<TestComponent> component, Class<Dependency> dependency) {
            config.component(TestComponent.class, component);
            config.component(Dependency.class, dependency);
            ContextConfig.ContextConfigError exception = assertThrows(ContextConfig.ContextConfigError.class,
                    () -> config.getContext());
        }

        public static Stream<Arguments> should_throw_exception_if_cycle_dependencies_found() {
            List<Arguments> arguments = new ArrayList<>();
            for (final Named component : List.of(Named.of("Inject Constructor", CyclicComponentInjectConstructor.class),
                    Named.of("Inject Field", CyclicComponentInjectField.class),
                    Named.of("Inject Method", CyclicComponentInjectMethod.class))) {
                for (final Named dependency : List.of(Named.of("Inject Constructor", CyclicDependencyInjectConstructor.class),
                        Named.of("Inject Field", CyclicDependencyInjectField.class),
                        Named.of("Inject Method", CyclicDependencyInjectMethod.class))) {
                    arguments.add(Arguments.of(component, dependency));
                }
            }
            return arguments.stream();
        }


        static class CyclicComponentInjectConstructor implements TestComponent {
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
            void install(Dependency dependency) {

            }
        }

        static class CyclicDependencyInjectConstructor implements Dependency {
            @Inject
            void CyclicDependencyInjectConstructor(TestComponent component) {

            }
        }

        static class CyclicDependencyInjectField implements Dependency {
            @Inject
            TestComponent component;
        }

        static class CyclicDependencyInjectMethod implements Dependency {
            @Inject
            void install(TestComponent component) {
            }
        }


        @ParameterizedTest(name = "indirect cyclic dependency between {0}, {1} and {2}")
        @MethodSource
        void should_throw_exception_if_transitive_cyclic_dependency(Class<TestComponent> component,
                                                                    Class<Dependency> dependency,
                                                                    Class<AnotherDependency> anotherDependency) {
            config.component(TestComponent.class, component);
            config.component(Dependency.class, dependency);
            config.component(AnotherDependency.class, anotherDependency);
            ContextConfig.ContextConfigError exception = assertThrows(ContextConfig.ContextConfigError.class,
                    () -> config.getContext());

//            List<Class<?>> components = List.of(exception.getComponents());
//            assertEquals(3, components.size());
//            assertTrue(components.contains(TestComponent.class));
//            assertTrue(components.contains(Dependency.class));
//            assertTrue(components.contains(AnotherDependency.class));
        }

        public static Stream<Arguments> should_throw_exception_if_transitive_cyclic_dependency() {
            List<Arguments> arguments = new ArrayList<>();

            for (final Named component : List.of(Named.of("Inject Constructor", CyclicComponentInjectConstructor.class),
                    Named.of("Inject Field", CyclicComponentInjectField.class),
                    Named.of("Inject Method", CyclicComponentInjectMethod.class))) {
                for (final Named dependency : List.of(Named.of("Inject Constructor", IndirectCyclicDependencyInjectConstructor.class),
                        Named.of("Inject Field", IndirectCyclicDependencyInjectField.class),
                        Named.of("Inject Method", IndirectCyclicDependencyInjectMethod.class))) {
                    for (final Named anotherDependency : List.of(Named.of("Inject Constructor", IndirectCyclicAnotherDependencyInjectConstructor.class),
                            Named.of("Inject Field", IndirectCyclicAnotherDependencyInjectField.class),
                            Named.of("Inject Method", IndirectCyclicAnotherDependencyInjectMethod.class))) {
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
            void install(AnotherDependency anotherDependency) {
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
            void install(TestComponent component) {
            }
        }

        static class CyclicDependencyProviderConstructor implements Dependency {
            @Inject
            public CyclicDependencyProviderConstructor(Provider<TestComponent> componentProvider) {
            }
        }

        @Test
        void should_not_throw_exception_if_cyclic_dependency_via_provider_constructor() {
            config.component(TestComponent.class, CyclicComponentInjectConstructor.class);
            config.component(Dependency.class, CyclicDependencyProviderConstructor.class);
            assertTrue(config.getContext().get(ComponentRef.of(Dependency.class)).isPresent());
        }

        @Nested
        class WithQualifier {

            @ParameterizedTest
            @MethodSource
            void should_throw_exception_if_dependency_with_qualifier_not_found(Class<? extends TestComponent> component) {
                config.instance(Dependency.class, dependency);
                config.component(TestComponent.class, component, new NamedLiteral("Owner"));
                assertThrows(ContextConfig.ContextConfigError.class, () -> config.getContext());

            }

            static Stream<Arguments> should_throw_exception_if_dependency_with_qualifier_not_found() {
                return Stream.of(Named.of("Inject Constructor with Qualifier", InjectionConstructor.class),
                        Named.of("Inject Field with Qualifier", InjectionField.class),
                        Named.of("Inject Method with Qualifier", InjectionMethod.class),
                        Named.of("Provider in inject Constructor with Qualifier", InjectionConstructorProvider.class),
                        Named.of("Provider in inject Field with Qualifier", InjectionFieldProvider.class),
                        Named.of("Provider in inject Method with Qualifier", InjectionMethodProvider.class)).map(Arguments::of);
            }

            static class InjectionConstructor {
                @Inject
                public InjectionConstructor(@Skywalker Dependency dependency) {
                }
            }

            static class InjectionConstructorProvider {
                private Provider<Dependency> dependency;

                @Inject
                public InjectionConstructorProvider(@Skywalker Provider<Dependency> dependency) {
                    this.dependency = dependency;
                }
            }

            static class InjectionField {
                @Skywalker
                @Inject
                Dependency dependency;
            }

            static class InjectionFieldProvider {
                @Skywalker
                @Inject
                Provider<Dependency> dependency;
            }


            static class InjectionMethod {
                @Inject
                void install(@Skywalker Dependency dependency) {
                }
            }

            static class InjectionMethodProvider {
                @Inject
                void install(@Skywalker Dependency dependency) {
                }
            }

            static class SkywalkerInjectConstructor implements Dependency {
                @Inject
                public SkywalkerInjectConstructor(@jakarta.inject.Named("ChosenOne") Dependency dependency) {
                }
            }

            static class SkywalkerInjectField implements Dependency {
                @jakarta.inject.Named("ChosenOne")
                @Inject
                Dependency dependency;
            }

            static class SkywalkerInjectMethod implements Dependency {
                @Inject
                void install(@jakarta.inject.Named("ChosenOne") Dependency dependency) {

                }
            }

            static class NoCyclicInjectConstructor implements Dependency {
                @Inject
                public NoCyclicInjectConstructor(@Skywalker Dependency dependency) {
                }
            }

            static class NoCyclicInjectField implements Dependency {
                @Skywalker
                @Inject
                Dependency dependency;
            }

            static class NoCyclicInjectMethod implements Dependency {
                @Inject
                void install(@Skywalker Dependency dependency) {
                }
            }

            @ParameterizedTest(name = "{1} -> @Skywalker({0}) -> @Named(\"ChosenOne\") not cyclic dependencies")
            @MethodSource
            void should_not_throw_cyclic_dependency_exception_if_component_with_same_type_but_tagged_with_different_qualifier(Class<? extends Dependency> skywalker,
                                                                                                                              Class<? extends Dependency> noCyclic) {
                config.instance(Dependency.class, dependency, new NamedLiteral("ChosenOne"));
                config.component(Dependency.class, skywalker, new SkywalkerLiteral());
                config.component(Dependency.class, noCyclic);

                assertDoesNotThrow(() -> config.getContext());
            }

            static Stream<Arguments> should_not_throw_cyclic_dependency_exception_if_component_with_same_type_but_tagged_with_different_qualifier() {
                List<Arguments> arguments = new ArrayList<>();

                for (Named skywalker : List.of(Named.of("Inject Constructor", SkywalkerInjectConstructor.class),
                        Named.of("Inject Field", SkywalkerInjectField.class),
                        Named.of("Inject Method", SkywalkerInjectMethod.class))) {
                    for (Named noCyclic : List.of(Named.of("Inject Constructor", NoCyclicInjectConstructor.class),
                            Named.of("Inject Field", NoCyclicInjectField.class),
                            Named.of("Inject Method", NoCyclicInjectMethod.class))) {
                        arguments.add(Arguments.of(skywalker, noCyclic));
                    }
                }
                return arguments.stream();
            }
        }
    }

    @Nested
    class DSL {
        interface Api {

        }

        static class Implementation implements Api {

        }

        @Test
        void should_bind_instance_as_its_declaration_type() {
            Implementation instance = new Implementation();
            config.from(new Config() {
                final Implementation implementation = instance;
            });
            Context context = config.getContext();
            Implementation implementation = context.get(ComponentRef.of(Implementation.class)).get();
            assertSame(instance, implementation);
        }

        @Test
        void should_bind_component_as_its_own_type() {
            config.from(new Config() {
                Implementation implementation;
            });
            Context context = config.getContext();
            assertTrue(context.get(ComponentRef.of(Implementation.class)).isPresent());
        }

        @Test
        void should_bind_instance_using_export_type() {
            Implementation instance = new Implementation();
            config.from(new Config() {
                @Export(Api.class)
                Implementation implementation = instance;
            });

            Context context = config.getContext();
            assertSame(instance, context.get(ComponentRef.of(Api.class)).get());
        }

        @Test
        void should_bind_component_using_export_type() {
            config.from(new Config() {
                @Export(Api.class)
                Implementation implementation;
            });

            Context context = config.getContext();
            assertTrue(context.get(ComponentRef.of(Api.class)).isPresent());
        }

        @Test
        void should_bind_instance_with_qualifier() {
            Implementation instance = new Implementation();
            config.from(new Config() {
                @Skywalker
                Api implementation = instance;
            });

            Context context = config.getContext();
            assertSame(instance, context.get(ComponentRef.of(Api.class, new SkywalkerLiteral())).get());
        }

        @Test
        void should_bind_instance_with_provider() {
            Api instance = new Implementation();
            config.from(new Config() {
                Api implementation = instance;
            });


            Context context = config.getContext();
            Provider<Api> provider = context.get(new ComponentRef<Provider<Api>>() {
            }).get();
            assertSame(instance, provider.get());
        }


        @Test
        void should_bind_instance_with_scope() {
            Api instance = new Implementation();
            config.from(new Config() {
                @Singleton
                Api implementation = instance;
            });

            Context context = config.getContext();
            assertSame(context.get(ComponentRef.of(Api.class)).get(), context.get(ComponentRef.of(Api.class)).get());
        }

        @Test
        void should_bind_component_with_qualifier() {
            config.from(new Config() {
                @Skywalker
                Implementation implementation;
            });

            Context context = config.getContext();
            assertTrue(context.get(ComponentRef.of(Implementation.class, new SkywalkerLiteral())).isPresent());
        }

        @Test
        void should_bind_component_with_provider() {
            config.from(new Config() {
                Implementation implementation;
            });

            Context context = config.getContext();
            Provider<Implementation> provider = context.get(new ComponentRef<Provider<Implementation>>() {
            }).get();
            assertNotNull(provider.get());
        }

        @Test
        void should_bind_component_with_scope() {
            config.from(new Config() {
                @Singleton
                Implementation implementation;
            });

            Context context = config.getContext();
            assertSame(context.get(ComponentRef.of(Implementation.class)).get(), context.get(ComponentRef.of(Implementation.class)).get());
        }
    }


}