package org.tdd.di;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.tdd.di.exception.CycleDependencyNotAllowed;
import org.tdd.di.exception.DependencyNotFoundException;
import org.tdd.di.exception.IllegalComponentException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class ContainerTest {

    private ContainerBuilder containerBuilder;

    @BeforeEach
    public void setup() {
        containerBuilder = new ContainerBuilder();
    }

    @Nested
    class ComponentBindTest {
        @Nested
        class BindingTest {
            @Test
            void should_bind_type_to_an_instance() {
                Component componentImpl = new Component() {
                };
                containerBuilder.bind(Component.class, componentImpl);

                assertSame(componentImpl, containerBuilder.build().get(Component.class).orElse(null));
            }

            interface Component {
                default Dependency getDependency() {
                    return null;
                }
            }

            interface Dependency {
            }

            static class ConstructorInject implements Component {
                @Inject
                public ConstructorInject(Dependency dependency) {
                    this.dependency = dependency;
                }

                private final Dependency dependency;

                @Override
                public Dependency getDependency() {
                    return dependency;
                }
            }
            static class FieldInject implements Component {
                @Inject
                Dependency dependency;

                @Override
                public Dependency getDependency() {
                    return dependency;
                }
            }
            static class MethodInject implements Component {
                Dependency dependency;

                @Inject
                public void setDependency(Dependency dependency) {
                    this.dependency = dependency;
                }
                @Override
                public Dependency getDependency() {
                    return dependency;
                }
            }

            static Stream<Arguments> ComponentWithDependencyClassProvider() {
                return Stream.of(Arguments.of(Named.of("Constructor Inject", ConstructorInject.class)),
                        Arguments.of(Named.of("Field Inject", FieldInject.class)),
                        Arguments.of(Named.of("Method Inject", MethodInject.class)));
            }

            @ParameterizedTest(name = "supporting {0}")
            @MethodSource("ComponentWithDependencyClassProvider")
            void should_bind_type_to_an_injectable_component(Class<? extends Component> componentClass) {
                Dependency dependency = new Dependency() {
                };
                containerBuilder.bind(Dependency.class, dependency).bind(Component.class, componentClass);

                Optional<Component> componentOpl = containerBuilder.build().get(Component.class);
                assertTrue(componentOpl.isPresent());
                assertSame(dependency, componentOpl.get().getDependency());
            }

            @Test
            void should_return_empty_when_get_if_type_not_bind() {
                Optional<Component> component = containerBuilder.build().get(Component.class);
                assertTrue(component.isEmpty());
            }

            @Test
            void should_throw_exception_when_build_if_bind_abstract_or_interface() {
                assertThrows(IllegalComponentException.class, () -> containerBuilder.bind(Component.class, AbstractComponent.class).build());
                assertThrows(IllegalComponentException.class, () -> containerBuilder.bind(Component.class, Component.class).build());
            }

            static abstract class AbstractComponent implements Component {
                public AbstractComponent() {
                }
            }

        }

        @Nested
        class DependencyCheckTest {
            @Test
            void should_throw_Exception_when_get_dependency_not_found() {
                containerBuilder.bind(AnotherComponent.class, ComponentWithDependency.class);

                DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () -> containerBuilder.build());
                assertEquals(Component.class, exception.getDependency());
                assertEquals(AnotherComponent.class, exception.getComponent());
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

    public Dependency getDependency() {
        return dependency;
    }
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