package org.tdd.di;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tdd.di.exception.FinalFieldInjectException;
import org.tdd.di.exception.IllegalComponentException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InjectTest {

    @Mock
    private Dependency dependency;
    @Mock(lenient = true)
    private Container container;

    @BeforeEach
    public void setup() {
        when(container.get(eq(Dependency.class))).thenReturn(Optional.of(dependency));
    }

    @Nested
    class InjectWithConstructorTest {

        @Test
        void should_return_a_component_when_get_if_the_bind_component_no_dependency() {
            Component component = new InjectComponentProvider<>(ComponentNoDependency.class).getFrom(container);
            assertNotNull(component);
            assertTrue(component instanceof Component);
        }

        @Test
        void should_return_a_correct_component_when_get_if_the_bind_component_has_dependency() {
            when(container.get(Component.class)).thenReturn(Optional.of(mock(ComponentNoDependency.class)));

            AnotherComponent component = new InjectComponentProvider<>(ComponentWithDependency.class).getFrom(container);

            assertNotNull(component);
            assertTrue(component instanceof ComponentWithDependency);
            assertNotNull(((ComponentWithDependency) component).getDependency());
        }

        @Test
        void should_return_a_correct_component_when_get_if_bind_transitive_dependency_component() {
            when(container.get(Component.class)).thenReturn(Optional.of(new ComponentDependencyString("dependency")));

            AnotherComponent component = new InjectComponentProvider<>(ComponentWithDependency.class).getFrom(container);

            assertNotNull(component);
            assertTrue(component instanceof ComponentWithDependency);

            Component dependency = ((ComponentWithDependency) component).getDependency();
            assertNotNull(dependency);

            assertEquals("dependency", ((ComponentDependencyString) dependency).getDependency());
        }

        @Test
        void should_throw_Exception_when_bind_if_multi_inject_constructor_provided() {
            assertThrows(IllegalComponentException.class, () ->
                    new InjectComponentProvider<>(ComponentWithMultiInjectConstructor.class));
        }

        @Test
        void should_throw_Exception_when_bind_if_no_inject_nor_default_constructor_provided() {
            assertThrows(IllegalComponentException.class, () ->
                    new InjectComponentProvider<>(ComponentWithNoInjectNorDefaultConstructor.class));
        }

    }

    @Nested
    class InjectWithFieldTest {
        @Test
        void should_return_component_when_get_if_dependency_has_bind() {
            ComponentInjectDependencyWithField component = new InjectComponentProvider<>(ComponentInjectDependencyWithField.class).getFrom(container);
            assertNotNull(component);
            assertSame(dependency, component.dependency);
        }

        static class ComponentInjectDependencyWithField implements Component {
            @Inject
            Dependency dependency;
        }

        static class SubComponent extends ComponentInjectDependencyWithField {
        }

        @Test
        void should_inject_supper_class_fields_when_get_subclass_component() {
            SubComponent component = new InjectComponentProvider<>(SubComponent.class).getFrom(container);
            assertNotNull(component);
            assertSame(dependency, component.dependency);
        }

        @Test
        void should_return_filed_dependency_when_get_dependencies_from_provider() {
            InjectComponentProvider<ComponentInjectDependencyWithField> provider = new InjectComponentProvider<>(ComponentInjectDependencyWithField.class);

            assertArrayEquals(new Class[]{Dependency.class}, provider.getDependencies().toArray());
        }

        @Test
        void should_throw_exception_when_inject_field_is_final() {
            FinalFieldInjectException exception = assertThrows(FinalFieldInjectException.class,
                    () -> new InjectComponentProvider<>(ComponentWithFinalField.class).getFrom(container));
            assertEquals(ComponentWithFinalField.class, exception.getComponent());
            assertEquals("dependency", exception.getFieldName());
        }

        static class ComponentWithFinalField implements Component {
            @Inject
            private final Dependency dependency;

            ComponentWithFinalField() {
                dependency = null;
            }
        }
    }

    @Nested
    class InjectWithMethodTest {
        @Test
        void should_return_component_when_get_if_dependency_has_bind() {
            ComponentInjectDependencyWithMethod component = new InjectComponentProvider<>(ComponentInjectDependencyWithMethod.class).getFrom(container);
            assertNotNull(component);
            assertSame(dependency, component.dependency);
            // 通过 Inject 标注的无参数方法，会被调用。
            assertTrue(component.injected);
        }

        static class ComponentInjectDependencyWithMethod implements Component {
            boolean injected = false;

            @Inject
            public void setDependency(Dependency dependency) {
                this.dependency = dependency;
            }

            Dependency dependency;

            @Inject
            public void inject() {
                this.injected = true;
            }
        }

        static class SubComponent extends ComponentInjectDependencyWithMethod {
            boolean subInjected = false;

            @Override
            @Inject
            public void inject() {
                subInjected = true;
            }
        }

        @Test
        void should_call_sub_class_inject_method_only_if_override_supper_method() {
            SubComponent component = new InjectComponentProvider<>(SubComponent.class).getFrom(container);
            assertNotNull(component);
            assertSame(dependency, component.dependency);
            assertFalse(component.injected);
            assertTrue(component.subInjected);
        }

        @Test
        void should_return_method_inject_dependencies_when_get_dependencies_from_provider() {
            InjectComponentProvider<ComponentInjectDependencyWithMethod> provider = new InjectComponentProvider<>(ComponentInjectDependencyWithMethod.class);

            assertArrayEquals(new Class[]{Dependency.class}, provider.getDependencies().toArray());
        }

        @Test
        void should_call_supper_inject_method_before_sub_inject_method() {
            SubComponentWithAnotherInjectMethod component = new InjectComponentProvider<>(SubComponentWithAnotherInjectMethod.class).getFrom(container);

            assertNotNull(component);
            assertEquals(1, component.superInjectCall);
            assertEquals(2, component.subInjectCall);
        }

        static class SuperComponentWithInjectMethod {
            int superInjectCall = 0;

            @Inject
            public void supperInject() {
                superInjectCall++;
            }
        }

        static class SubComponentWithAnotherInjectMethod extends SuperComponentWithInjectMethod {
            int subInjectCall = 0;

            @Inject
            public void subInject() {
                subInjectCall = super.superInjectCall + 1;
            }
        }

        static class SubComponentOverrideMethod extends SuperComponentWithInjectMethod {
            int subInjectCall = 0;

            @Override
            public void supperInject() {
                subInjectCall++;
            }
        }

        @Test
        void should_not_call_inject_method_when_sub_override_and_no_inject() {

            SubComponentOverrideMethod component = new InjectComponentProvider<>(SubComponentOverrideMethod.class).getFrom(container);

            assertNotNull(component);
            assertEquals(0, component.superInjectCall);
            assertEquals(0, component.subInjectCall);
        }

        @Test
        void should_throw_exception_when_build_inject_method_defined_class_param() {
            assertThrows(IllegalComponentException.class, () -> new InjectComponentProvider<>(ComponentWithInjectMethodWithTypeParam.class));
        }

        static class ComponentWithInjectMethodWithTypeParam {
            @Inject
            public <T> void inject(T type) {
            }
        }
    }
}
