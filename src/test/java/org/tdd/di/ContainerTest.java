package org.tdd.di;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
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
import org.tdd.di.exception.UnsupportedTypeException;

import java.lang.reflect.ParameterizedType;
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

                assertSame(componentImpl, containerBuilder.build().get(ContainerBuilder.Ref.of(Component.class)).orElse(null));
            }

            interface Component {
                default Object getDependency() {
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

            static class ConstructorInjectProvider implements Component {
                @Inject
                public ConstructorInjectProvider(Provider<Dependency> dependency) {
                    this.dependency = dependency;
                }

                private final Provider<Dependency> dependency;

                @Override
                public Provider<Dependency> getDependency() {
                    return dependency;
                }
            }

            static class FieldInjectProvider implements Component {
                @Inject
                Provider<Dependency> dependency;

                @Override
                public Provider<Dependency> getDependency() {
                    return dependency;
                }
            }

            static class MethodInjectProvider implements Component {
                Provider<Dependency> dependency;

                @Inject
                public void setDependency(Provider<Dependency> dependency) {
                    this.dependency = dependency;
                }

                @Override
                public Provider<Dependency> getDependency() {
                    return dependency;
                }
            }

            static Stream<Arguments> componentWithDependencyClassProvider() {
                return Stream.of(Arguments.of(Named.of("Constructor Inject", ConstructorInject.class)),
                        Arguments.of(Named.of("Field Inject", FieldInject.class)),
                        Arguments.of(Named.of("Method Inject", MethodInject.class)),
                        Arguments.of(Named.of("Constructor Inject Provider", ConstructorInjectProvider.class)),
                        Arguments.of(Named.of("Field Inject Provider", FieldInjectProvider.class)),
                        Arguments.of(Named.of("Method Inject Provider", MethodInjectProvider.class)));
            }

            @ParameterizedTest(name = "supporting {0}")
            @MethodSource("componentWithDependencyClassProvider")
            void should_bind_type_to_an_injectable_component(Class<? extends Component> componentClass) {
                Dependency dependencyImpl = new Dependency() {
                };
                containerBuilder.bind(Dependency.class, dependencyImpl).bind(Component.class, componentClass);

                Optional<Component> componentOpl = containerBuilder.build().get(ContainerBuilder.Ref.of(Component.class));
                assertTrue(componentOpl.isPresent());

                Object dependency = componentOpl.get().getDependency();
                if (dependency instanceof Provider) {
                    assertSame(dependencyImpl, ((Provider<?>) dependency).get());
                } else {
                    assertSame(dependencyImpl, dependency);
                }
            }

            @Test
            void should_return_empty_when_get_if_type_not_bind() {
                Optional<?> component = containerBuilder.build().get(ContainerBuilder.Ref.of(Component.class));
                assertTrue(component.isEmpty());
            }

            @Test
            void should_return_empty_when_get_if_provider_type_not_bind() throws NoSuchFieldException {
                Optional provider = containerBuilder.build().get(ContainerBuilder.Ref.of(FieldInjectProvider.class.getDeclaredField("dependency").getGenericType()));

                assertTrue(provider.isEmpty());
            }

            @Test
            void should_support_get_bind_type_as_provider() {
                Component instance = new Component() {
                };
                Container container = containerBuilder.bind(Component.class, instance).build();
                ParameterizedType parameterizedType = new TypeWrapper<Provider<Component>>() {
                }.getType();

                Optional<?> provider = container.get(ContainerBuilder.Ref.of(parameterizedType));

                assertTrue(provider.isPresent());
                assertSame(instance, ((Provider<Component>) provider.get()).get());
            }

            abstract class TypeWrapper<T> {
                public ParameterizedType getType() {
                    return (ParameterizedType) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
                }
            }

            @Test
            void should_throw_exception_when_get_unsupported_container_type_as_provider() {

                Component instance = new Component() {
                };
                Container container = containerBuilder.bind(Component.class, instance).build();
                ParameterizedType parameterizedType = new TypeWrapper<List<Component>>() {
                }.getType();

                assertThrows(UnsupportedTypeException.class, () -> container.get(ContainerBuilder.Ref.of(parameterizedType)));
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
            @ParameterizedTest
            @MethodSource("componentMissDependencyClassProvider")
            void should_throw_Exception_when_get_dependency_not_found(Class<? extends BindingTest.Component> componentType) {
                containerBuilder.bind(BindingTest.Component.class, componentType);

                DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () -> containerBuilder.build());
                assertEquals(BindingTest.Component.class, exception.getComponent());
                assertEquals(BindingTest.Dependency.class, exception.getDependency());
            }

            static Stream<Arguments> componentMissDependencyClassProvider() {
                return Stream.of(Arguments.of(Named.of("Constructor Inject", BindingTest.ConstructorInject.class)),
                        Arguments.of(Named.of("Field Inject", BindingTest.FieldInject.class)),
                        Arguments.of(Named.of("Method Inject", BindingTest.MethodInject.class)),
                        Arguments.of(Named.of("Constructor Inject Provider", BindingTest.ConstructorInjectProvider.class)),
                        Arguments.of(Named.of("Field Inject Provider", BindingTest.FieldInjectProvider.class)),
                        Arguments.of(Named.of("Method Inject Provider", BindingTest.MethodInjectProvider.class)));
            }

            interface Component {
            }

            interface Dependency {
            }

            interface AnotherComponent {
            }

            static class ComponentDependentDependency implements Component {
                @Inject
                Dependency dependency;
            }

            static class DependencyDependentComponent implements Dependency {
                @Inject
                Component component;
            }

            static class DependencyDependentAnotherComponent implements Dependency {
                @Inject
                AnotherComponent anotherComponent;
            }

            static class AnotherComponentDependentComponent implements AnotherComponent {
                @Inject
                Component component;
            }

            @Test
            void should_throw_Exception_when_bind_if_cycle_dependency() {
                containerBuilder.bind(Component.class, ComponentDependentDependency.class)
                        .bind(Dependency.class, DependencyDependentComponent.class);

                CycleDependencyNotAllowed exception = assertThrows(CycleDependencyNotAllowed.class, () -> containerBuilder.build());
                List<Class<?>> components = exception.getComponents();
                assertEquals(2, components.size());
                assertTrue(components.contains(Component.class));
                assertTrue(components.contains(Dependency.class));
            }

            @Test
            void should_throw_Exception_when_bind_if_transitive_cycle_dependency() {
                containerBuilder.bind(Component.class, ComponentDependentDependency.class)
                        .bind(Dependency.class, DependencyDependentAnotherComponent.class)
                        .bind(AnotherComponent.class, AnotherComponentDependentComponent.class);

                CycleDependencyNotAllowed exception = assertThrows(CycleDependencyNotAllowed.class, () -> containerBuilder.build());
                List<Class<?>> components = exception.getComponents();
                assertEquals(3, components.size());
                assertTrue(components.contains(Component.class));
                assertTrue(components.contains(Dependency.class));
                assertTrue(components.contains(AnotherComponent.class));
            }

            @Test
            void should_not_throw_Exception_when_bind_if_cycle_dependency_via_provider() {
                Container container = containerBuilder.bind(Component.class, ComponentDependentDependency.class)
                        .bind(Dependency.class, DependencyDependentProviderComponent.class).build();
                Optional<?> component = container.get(ContainerBuilder.Ref.of(Component.class));
                Optional<?> dependency = container.get(ContainerBuilder.Ref.of(Dependency.class));

                assertTrue(component.isPresent());
                assertTrue(dependency.isPresent());
                assertTrue(((ComponentDependentDependency) component.get()).dependency instanceof Dependency);
                assertTrue(((DependencyDependentProviderComponent) dependency.get()).componentProvider.get() instanceof Component);
            }

            static class DependencyDependentProviderComponent implements Dependency {
                @Inject
                Provider<Component> componentProvider;
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
