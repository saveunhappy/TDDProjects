package com.geektime.tdd;

import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Nested
public class InjectTest {
    private final Dependency dependency = mock(Dependency.class);
    private final Context context = mock(Context.class);

    @BeforeEach
    public void setup() {
        when(context.get(eq(Dependency.class))).thenReturn(Optional.of(dependency));
    }

    @Nested
    public class ConstructorInjection {

        static class DefaultConstructor{

        }
        @Test
        public void should_call_default_constructor_if_no_inject_constructor() {
            DefaultConstructor instance =
                    new ConstructorInjectionProvider<>(DefaultConstructor.class)
                            .get(context);

            assertNotNull(instance);
        }

        @Test
        public void should_inject_dependency_via_inject_constructor() {


            ComponentWithInjectionConstructor instance = new ConstructorInjectionProvider<>(ComponentWithInjectionConstructor.class).get(context);
            assertNotNull(instance);
            assertSame(dependency, instance.getDependency());
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
            ComponentWithFieldInjection component = new ConstructorInjectionProvider<>(ComponentWithFieldInjection.class).get(context);
            assertSame(dependency, component.dependency);

        }

        @Test
        public void should_inject_dependency_via_superclass_inject_field() throws Exception {

            SubclassWithFieldInjection component = new ConstructorInjectionProvider<>(SubclassWithFieldInjection.class).get(context);
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

            InjectMethodWithNoDependency component = new ConstructorInjectionProvider<>(InjectMethodWithNoDependency.class).get(context);
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
            InjectMethodWithDependency component = new ConstructorInjectionProvider<>(InjectMethodWithDependency.class).get(context);
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

            SubClassWithInjectMethod component = new ConstructorInjectionProvider<>(SubClassWithInjectMethod.class).get(context);
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
        public void should_only_call_once_if_subclass_override_inject_method_with_inject() {

            SubClassOverrideSuperClassWithInject component = new ConstructorInjectionProvider<>(SubClassOverrideSuperClassWithInject.class).get(context);
            assertEquals(1, component.superCalled);
        }

        static class SubClassOverrideSuperClassWithNoInject extends SuperClassWithInjectMethod {
            void install() {
                super.install();
            }
        }

        @Test
        public void should_not_call_inject_method_if_override_with_no_inject() {

            SubClassOverrideSuperClassWithNoInject component = new ConstructorInjectionProvider<>(SubClassOverrideSuperClassWithNoInject.class).get(context);
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
