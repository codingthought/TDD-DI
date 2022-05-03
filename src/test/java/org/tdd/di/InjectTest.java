package org.tdd.di;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.tdd.di.exception.FinalFieldInjectException;
import org.tdd.di.exception.IllegalComponentException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Nested
class InjectTest {

    private ContainerBuilder containerBuilder;

    @BeforeEach
    public void setup() {
        containerBuilder = new ContainerBuilder();
    }

    @Nested
    class InjectWithConstructorTest {

        @Test
        void should_return_a_component_when_get_if_the_bind_component_no_dependency() {
            containerBuilder.bind(Component.class, ComponentNoDependency.class);
            Container container = containerBuilder.build();

            Component component = container.get(Component.class).orElse(null);
            assertNotNull(component);
            assertTrue(component instanceof Component);
        }

        @Test
        void should_return_a_correct_component_when_get_if_the_bind_component_has_dependency() {
            containerBuilder.bind(AnotherComponent.class, ComponentWithDependency.class)
                    .bind(Component.class, ComponentNoDependency.class);

            AnotherComponent component = containerBuilder.build().get(AnotherComponent.class).orElse(null);

            assertNotNull(component);
            assertTrue(component instanceof ComponentWithDependency);
            assertNotNull(((ComponentWithDependency) component).getDependency());
        }

        @Test
        void should_return_a_correct_component_when_get_if_bind_transitive_dependency_component() {
            containerBuilder.bind(AnotherComponent.class, ComponentWithDependency.class)
                    .bind(Component.class, ComponentDependencyString.class)
                    .bind(String.class, "dependency");

            AnotherComponent component = containerBuilder.build().get(AnotherComponent.class).orElse(null);

            assertNotNull(component);
            assertTrue(component instanceof ComponentWithDependency);

            Component dependency = ((ComponentWithDependency) component).getDependency();
            assertNotNull(dependency);

            assertEquals("dependency", ((ComponentDependencyString) dependency).getDependency());
        }

        @Test
        void should_throw_Exception_when_bind_if_multi_inject_constructor_provided() {
            assertThrows(IllegalComponentException.class, ()
                    -> containerBuilder.bind(Component.class, ComponentWithMultiInjectConstructor.class));
        }

        @Test
        void should_throw_Exception_when_bind_if_no_inject_nor_default_constructor_provided() {
            assertThrows(IllegalComponentException.class,
                    () -> containerBuilder.bind(Component.class, ComponentWithNoInjectNorDefaultConstructor.class));
        }

        @Test
        void should_return_empty_when_get_if_not_bind() {
            Optional<Component> component = containerBuilder.build().get(Component.class);
            assertTrue(component.isEmpty());
        }
    }

    @Nested
    class InjectWithFieldTest {
        @Test
        void should_return_component_when_get_if_dependency_has_bind() {
            Dependency dependency = new Dependency() {
            };
            Container container = containerBuilder
                    .bind(InjectWithFieldTest.ComponentInjectDependencyWithField.class, InjectWithFieldTest.ComponentInjectDependencyWithField.class)
                    .bind(Dependency.class, dependency)
                    .build();

            InjectWithFieldTest.ComponentInjectDependencyWithField component = container.get(InjectWithFieldTest.ComponentInjectDependencyWithField.class).orElse(null);
            assertNotNull(component);
            assertSame(dependency, component.dependency);
        }

        static class ComponentInjectDependencyWithField implements Component {
            @Inject
            Dependency dependency;
        }

        static class SubComponent extends InjectWithFieldTest.ComponentInjectDependencyWithField {
        }

        @Test
        void should_inject_supper_class_fields_when_get_subclass_component() {
            Dependency dependency = new Dependency() {
            };
            Container container = containerBuilder
                    .bind(InjectWithFieldTest.SubComponent.class, InjectWithFieldTest.SubComponent.class)
                    .bind(Dependency.class, dependency)
                    .build();

            InjectWithFieldTest.SubComponent component = container.get(InjectWithFieldTest.SubComponent.class).orElse(null);
            assertNotNull(component);
            assertSame(dependency, component.dependency);
        }

        @Test
        void should_return_filed_dependency_when_get_dependencies_from_provider() {
            InjectComponentProvider<InjectWithFieldTest.ComponentInjectDependencyWithField> provider = new InjectComponentProvider<>(InjectWithFieldTest.ComponentInjectDependencyWithField.class);

            assertArrayEquals(new Class[]{Dependency.class}, provider.getDependencies().toArray());
        }

        @Test
        void should_throw_exception_when_inject_field_is_final() {
            FinalFieldInjectException exception = assertThrows(FinalFieldInjectException.class,
                    () -> containerBuilder.bind(Component.class, InjectWithFieldTest.ComponentWithFinalField.class).build());
            assertEquals(InjectWithFieldTest.ComponentWithFinalField.class, exception.getComponent());
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
            Dependency dependency = new Dependency() {
            };
            Container container = containerBuilder
                    .bind(InjectWithMethodTest.ComponentInjectDependencyWithMethod.class, InjectWithMethodTest.ComponentInjectDependencyWithMethod.class)
                    .bind(Dependency.class, dependency)
                    .build();

            InjectWithMethodTest.ComponentInjectDependencyWithMethod component = container.get(InjectWithMethodTest.ComponentInjectDependencyWithMethod.class).orElse(null);
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

        static class SubComponent extends InjectWithMethodTest.ComponentInjectDependencyWithMethod {
            boolean subInjected = false;

            @Override
            @Inject
            public void inject() {
                subInjected = true;
            }
        }

        @Test
        void should_call_sub_class_inject_method_only_if_override_supper_method() {
            Dependency dependency = new Dependency() {
            };
            Container container = containerBuilder
                    .bind(InjectWithMethodTest.SubComponent.class, InjectWithMethodTest.SubComponent.class)
                    .bind(Dependency.class, dependency)
                    .build();

            InjectWithMethodTest.SubComponent component = container.get(InjectWithMethodTest.SubComponent.class).orElse(null);
            assertNotNull(component);
            assertSame(dependency, component.dependency);
            assertFalse(component.injected);
            assertTrue(component.subInjected);
        }

        @Test
        void should_return_method_inject_dependencies_when_get_dependencies_from_provider() {
            InjectComponentProvider<InjectWithMethodTest.ComponentInjectDependencyWithMethod> provider = new InjectComponentProvider<>(InjectWithMethodTest.ComponentInjectDependencyWithMethod.class);

            assertArrayEquals(new Class[]{Dependency.class}, provider.getDependencies().toArray());
        }

        @Test
        void should_call_supper_inject_method_before_sub_inject_method() {
            Container container = containerBuilder.bind(InjectWithMethodTest.SubComponentWithAnotherInjectMethod.class, InjectWithMethodTest.SubComponentWithAnotherInjectMethod.class).build();
            InjectWithMethodTest.SubComponentWithAnotherInjectMethod component = container.get(InjectWithMethodTest.SubComponentWithAnotherInjectMethod.class).orElse(null);

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

        static class SubComponentWithAnotherInjectMethod extends InjectWithMethodTest.SuperComponentWithInjectMethod {
            int subInjectCall = 0;

            @Inject
            public void subInject() {
                subInjectCall = super.superInjectCall + 1;
            }
        }

        static class SubComponentOverrideMethod extends InjectWithMethodTest.SuperComponentWithInjectMethod {
            int subInjectCall = 0;

            @Override
            public void supperInject() {
                subInjectCall++;
            }
        }

        @Test
        void should_not_call_inject_method_when_sub_override_and_no_inject() {
            Container container = containerBuilder.bind(InjectWithMethodTest.SubComponentOverrideMethod.class, InjectWithMethodTest.SubComponentOverrideMethod.class).build();
            InjectWithMethodTest.SubComponentOverrideMethod component = container.get(InjectWithMethodTest.SubComponentOverrideMethod.class).orElse(null);

            assertNotNull(component);
            assertEquals(0, component.superInjectCall);
            assertEquals(0, component.subInjectCall);
        }

        @Test
        void should_throw_exception_when_build_inject_method_defined_class_param() {
            assertThrows(IllegalComponentException.class, () -> new InjectComponentProvider<>(InjectWithMethodTest.ComponentWithInjectMethodWithTypeParam.class));
        }

        static class ComponentWithInjectMethodWithTypeParam {
            @Inject
            public <T> void inject(T type) {
            }
        }
    }
}
