package com.geektime.tdd;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Nested
public class InjectTest {
    ContextConfig config;

    @BeforeEach
    public void setup() {
        config = new ContextConfig();
    }

    @Nested
    public class ConstructorInjection {
        @Test
        public void should_bind_type_to_a_class_with_default_constructor() throws Exception {
            config.bind(Component.class, ComponentWithDefaultConstructor.class);
            Component instance = config.getContext().get(Component.class).get();
            assertNotNull(instance);
            //确保确实是根据ComponentWithDefaultConstructor这个Class通过newInstance构造的
            assertTrue(instance instanceof ComponentWithDefaultConstructor);

        }

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

        @Test
        public void should_throw_exception_if_multi_inject_constructors_provided() {
            assertThrows(IllegalComponentException.class, () ->
                    new ConstructorInjectionProvider<>((Class<? extends Component>) ComponentWithMultiInjectionConstructor.class)
            );
        }

        @Test
        public void should_throw_exception_if_no_inject_nor_default_constructor_provided() {
            assertThrows(IllegalComponentException.class, () ->
                    new ConstructorInjectionProvider<>((Class<? extends Component>) ComponentWithoutInjectionConstructorNorDefaultConstructor.class));
        }

        @Test
        public void should_include_dependency_from_inject_constructor() {
            ConstructorInjectionProvider<ComponentWithInjectionConstructor> provider = new ConstructorInjectionProvider<>(ComponentWithInjectionConstructor.class);
            assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependency().toArray());
        }

    }

    @Nested
    public class FieldInjection {
        static class ComponentWithFieldInjection {
            @Inject
            Dependency dependency;
        }

        static class SubclassWithFieldInjection extends ComponentWithFieldInjection {
        }

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
        public void should_inject_dependency_via_superclass_inject_field() throws Exception {
            Dependency dependency = new Dependency() {
            };
            config.bind(Dependency.class, dependency);
            config.bind(SubclassWithFieldInjection.class, SubclassWithFieldInjection.class);
            SubclassWithFieldInjection component = config.getContext().get(SubclassWithFieldInjection.class).get();
            assertSame(dependency, component.dependency);
        }


        static class FinalInjectField {
            @Inject
            final Dependency dependency = null;
        }

        @Test
        public void should_throw_exception_if_inject_field_is_final() {
            assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(FinalInjectField.class));
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

    }

    @Nested
    public class MethodInjection {
        static class InjectMethodWithNoDependency {
            boolean called = false;

            @Inject
            void install() {
                this.called = true;
            }
        }

        @Test
        public void should_call_inject_method_even_if_no_dependency_declared() throws Exception {
            config.bind(InjectMethodWithNoDependency.class, InjectMethodWithNoDependency.class);
            InjectMethodWithNoDependency component = config.getContext().get(InjectMethodWithNoDependency.class).get();
            assertTrue(component.called);
        }

        static class InjectMethodWithDependency {
            Dependency dependency;

            @Inject
            void install(Dependency dependency) {
                this.dependency = dependency;
            }
        }

        @Test
        public void should_inject_dependency_via_inject_method() throws Exception {
            Dependency dependency = new Dependency() {
            };
            config.bind(Dependency.class, dependency);
            config.bind(InjectMethodWithDependency.class, InjectMethodWithDependency.class);
            InjectMethodWithDependency component = config.getContext().get(InjectMethodWithDependency.class).get();
            assertEquals(dependency, component.dependency);
        }

        static class SuperClassWithInjectMethod {
            int superCalled = 0;

            @Inject
            void install() {
                superCalled++;
            }
        }

        static class SubClassWithInjectMethod extends SuperClassWithInjectMethod {
            int subCalled = 0;

            //注意，@inject标注的名字不能和父类的相同啊，否则永远调用的是子类的。
            @Inject
            void installAnother() {
                subCalled = superCalled + 1;
            }
        }

        @Test
        public void should_inject_dependencies_via_inject_method_from_superclass() throws Exception {
            config.bind(SubClassWithInjectMethod.class, SubClassWithInjectMethod.class);
            SubClassWithInjectMethod component = config.getContext().get(SubClassWithInjectMethod.class).get();
            //如果是先是子后是父，那么刚开始，superCalled是0，superCalled + 1是1，然后再调用父，父是0，加1还是1，就该都是1
            //如果先是父后是子，那么父先加了，是1，然后子的superCalled是1,1 + 1就是2
            assertEquals(1, component.superCalled);
            assertEquals(2, component.subCalled);
        }


        static class SubClassOverrideSuperClassWithInject extends SuperClassWithInjectMethod {
            //注意，@inject标注的名字不能和父类的相同啊，否则永远调用的是子类的。
            @Inject
            void install() {
                super.install();
            }
        }

        @Test
        public void should_only_call_once_if_subclass_override_inject_method_with_inject() throws Exception {
            config.bind(SubClassOverrideSuperClassWithInject.class, SubClassOverrideSuperClassWithInject.class);
            SubClassOverrideSuperClassWithInject component = config.getContext().get(SubClassOverrideSuperClassWithInject.class).get();
            assertEquals(1, component.superCalled);
        }

        static class SubClassOverrideSuperClassWithNoInject extends SuperClassWithInjectMethod {
            void install() {
                super.install();
            }
        }

        @Test
        public void should_not_call_inject_method_if_override_with_no_inject() throws Exception {
            config.bind(SubClassOverrideSuperClassWithNoInject.class, SubClassOverrideSuperClassWithNoInject.class);
            SubClassOverrideSuperClassWithNoInject component = config.getContext().get(SubClassOverrideSuperClassWithNoInject.class).get();
            assertEquals(0, component.superCalled);
        }

        @Test
        public void should_include_dependencies_from_inject_method() throws Exception {
            ConstructorInjectionProvider<InjectMethodWithDependency> provider = new ConstructorInjectionProvider<>(InjectMethodWithDependency.class);
            assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependency().toArray());
        }

        static class InjectMethodWithTypeParameter {
            @Inject
            <T> void install() {

            }
        }

        @Test
        public void should_throw_exception_if_inject_method_has_type_parameter() {
            assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(InjectMethodWithTypeParameter.class));
        }

    }

}
