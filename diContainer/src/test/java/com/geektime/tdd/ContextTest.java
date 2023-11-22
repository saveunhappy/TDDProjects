package com.geektime.tdd;

import com.geektime.tdd.Context;
import com.geektime.tdd.ContextConfig;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

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
        public void should_bind_type_to_a_specific_instance() throws Exception {
            Component instance = new Component() {
            };
            config.bind(Component.class, instance);
            Context context = config.getContext();
            assertSame(instance, context.get(Component.class).get());

        }

        @ParameterizedTest(name = "supporting {0}")
        @MethodSource
        public void should_bind_type_to_an_injectable_component(Class<? extends Component> componentType) {
            Dependency dependency = new Dependency() {
            };
            config.bind(Dependency.class, dependency);
            config.bind(Component.class, componentType);

            Optional<Component> component = config.getContext().get(Component.class);
            assertTrue(component.isPresent());
            assertSame(dependency,component.get().dependency());
        }
        public static Stream<Arguments> should_bind_type_to_an_injectable_component(){
            return Stream.of(Arguments.of(Named.of("Constructor injection",TypeBinding.ConstructorInjection.class)),
                    Arguments.of(Named.of("Field injection",TypeBinding.FieldInjection.class)),
                    Arguments.of(Named.of("Method injection",TypeBinding.MethodInjection.class)));
        }

        interface Component {
            default Dependency dependency(){
                return null;
            };
        }

        interface Dependency {

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
        class ComponentWithInjectionConstructor implements Component {
            Dependency dependency;

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
            Component component;

            @Inject
            public DependencyDependedOnComponent(Component component) {
                this.component = component;
            }
        }

        class AnotherDependencyDependedOnComponent implements AnotherDependency {

            Component component;

            @Inject
            public AnotherDependencyDependedOnComponent(Component component) {
                this.component = component;
            }
        }

        class DependencyDependedOnAnotherDependency implements Dependency {
            AnotherDependency anotherDependency;

            @Inject
            public DependencyDependedOnAnotherDependency(AnotherDependency anotherDependency) {
                this.anotherDependency = anotherDependency;
            }
        }

    }

}
