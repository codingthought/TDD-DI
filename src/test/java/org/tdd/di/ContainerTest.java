package org.tdd.di;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.tdd.di.exception.CycleDependencyNotAllowed;
import org.tdd.di.exception.DependencyNotFoundException;
import org.tdd.di.exception.FinalFieldInjectException;
import org.tdd.di.exception.IllegalComponentException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class ContainerTest {

    private ContainerBuilder containerBuilder;

    @BeforeEach
    public void setup() {
        containerBuilder = new ContainerBuilder();
    }

    @Nested
    class ComponentConstructionTest {

        @Test
        void should_return_the_component_when_get_if_the_component_bind() {
            Component componentImpl = new Component() {
            };
            containerBuilder.bind(Component.class, componentImpl);

            assertSame(componentImpl, containerBuilder.build().get(Component.class).orElse(null));
        }

        @Test
        void should_throw_exception_if_abstract_or_interface() {
            assertThrows(IllegalComponentException.class, () -> new InjectComponentProvider<>(AbstractComponent.class));
            assertThrows(IllegalComponentException.class, () -> new InjectComponentProvider<>(Component.class));
        }

        static abstract class AbstractComponent implements Component {
            public AbstractComponent() {
            }
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
            void should_throw_Exception_when_get_dependency_not_found() {
                containerBuilder.bind(AnotherComponent.class, ComponentWithDependency.class);

                DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () -> containerBuilder.build());
                assertEquals(Component.class, exception.getDependency());
                assertEquals(AnotherComponent.class, exception.getComponent());
            }

            @Test
            void should_return_empty_when_get_if_not_bind() {
                Optional<Component> component = containerBuilder.build().get(Component.class);
                assertTrue(component.isEmpty());
            }

            @Test
            void should_return_Exception_when_bind_if_cycle_dependency() {
                containerBuilder.bind(AnotherComponent.class, AnotherDependentComponent.class)
                        .bind(Component.class, ComponentDependentAnotherComponent.class);

                CycleDependencyNotAllowed exception = assertThrows(CycleDependencyNotAllowed.class, () -> containerBuilder.build());
                List<Class<?>> components = exception.getComponents();
                assertEquals(2, components.size());
                assertTrue(components.contains(AnotherComponent.class));
                assertTrue(components.contains(Component.class));
            }

            @Test
            void should_return_Exception_when_bind_if_transitive_cycle_dependency() {
                containerBuilder.bind(Component.class, ComponentDependentDependency.class)
                        .bind(Dependency.class, ComponentDependentAnother.class)
                        .bind(AnotherComponent.class, AnotherDependentComponent.class);

                CycleDependencyNotAllowed exception = assertThrows(CycleDependencyNotAllowed.class, () -> containerBuilder.build());
                List<Class<?>> components = exception.getComponents();
                assertEquals(3, components.size());
                assertTrue(components.contains(Component.class));
                assertTrue(components.contains(Dependency.class));
                assertTrue(components.contains(AnotherComponent.class));
            }
        }

        @Nested
        class InjectWithFieldTest {
            @Test
            void should_return_component_when_get_if_dependency_has_bind() {
                Dependency dependency = new Dependency() {
                };
                Container container = containerBuilder
                        .bind(ComponentInjectDependencyWithField.class, ComponentInjectDependencyWithField.class)
                        .bind(Dependency.class, dependency)
                        .build();

                ComponentInjectDependencyWithField component = container.get(ComponentInjectDependencyWithField.class).orElse(null);
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
                Dependency dependency = new Dependency() {
                };
                Container container = containerBuilder
                        .bind(SubComponent.class, SubComponent.class)
                        .bind(Dependency.class, dependency)
                        .build();

                SubComponent component = container.get(SubComponent.class).orElse(null);
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
                        () -> containerBuilder.bind(Component.class, ComponentWithFinalField.class).build());
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
                Dependency dependency = new Dependency() {
                };
                Container container = containerBuilder
                        .bind(ComponentInjectDependencyWithMethod.class, ComponentInjectDependencyWithMethod.class)
                        .bind(Dependency.class, dependency)
                        .build();

                ComponentInjectDependencyWithMethod component = container.get(ComponentInjectDependencyWithMethod.class).orElse(null);
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
                Dependency dependency = new Dependency() {
                };
                Container container = containerBuilder
                        .bind(SubComponent.class, SubComponent.class)
                        .bind(Dependency.class, dependency)
                        .build();

                SubComponent component = container.get(SubComponent.class).orElse(null);
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
                Container container = containerBuilder.bind(SubComponentWithAnotherInjectMethod.class, SubComponentWithAnotherInjectMethod.class).build();
                SubComponentWithAnotherInjectMethod component = container.get(SubComponentWithAnotherInjectMethod.class).orElse(null);

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
                Container container = containerBuilder.bind(SubComponentOverrideMethod.class, SubComponentOverrideMethod.class).build();
                SubComponentOverrideMethod component = container.get(SubComponentOverrideMethod.class).orElse(null);

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

    @Nested
    class ComponentSelectionTest {

    }

    @Nested
    class ComponentLifeCycleTest {

    }
}

interface Component {

}

interface Dependency {

}

interface AnotherComponent {

}

class ComponentDependentDependency implements Component {

    @Inject
    public ComponentDependentDependency(Dependency dependency) {
        this.dependency = dependency;
    }

    private final Dependency dependency;
}

class ComponentDependentAnother implements Dependency {


    @Inject
    public ComponentDependentAnother(AnotherComponent dependency) {
        this.dependency = dependency;
    }

    private final AnotherComponent dependency;
}

class ComponentNoDependency implements Component {

}

class ComponentWithDependency implements AnotherComponent {
    @Inject
    public ComponentWithDependency(Component dependency) {
        this.dependency = dependency;
    }

    private Component dependency;

    public Component getDependency() {
        return dependency;
    }
}

class AnotherDependentComponent implements AnotherComponent {
    @Inject
    public AnotherDependentComponent(Component dependency) {
        this.dependency = dependency;
    }

    private Component dependency;

    public Component getDependency() {
        return dependency;
    }
}

class ComponentDependentAnotherComponent implements Component {
    @Inject
    public ComponentDependentAnotherComponent(AnotherComponent dependency) {
        this.dependency = dependency;
    }

    private AnotherComponent dependency;

    public AnotherComponent getDependency() {
        return dependency;
    }
}

class ComponentDependencyString implements Component {
    @Inject
    public ComponentDependencyString(String dependency) {
        this.dependency = dependency;
    }

    private String dependency;

    public String getDependency() {
        return dependency;
    }
}

class ComponentWithMultiInjectConstructor implements Component {
    @Inject
    public ComponentWithMultiInjectConstructor(String name) {
    }

    @Inject
    public ComponentWithMultiInjectConstructor(Integer id) {
    }
}

class ComponentWithNoInjectNorDefaultConstructor implements Component {
    public ComponentWithNoInjectNorDefaultConstructor(String name) {
    }
}