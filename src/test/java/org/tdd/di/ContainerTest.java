package org.tdd.di;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.tdd.di.exception.IllegalComponentException;

import static org.junit.jupiter.api.Assertions.*;

public class ContainerTest {

    private Container container;

    @BeforeEach
    void setUp() {
        container = new Container();
    }

    @Nested
    class ComponentConstructionTest {

        @Test
        void should_return_the_component_when_get_if_the_component_bind() {
            Component componentImpl = new Component() {
            };
            container.bind(Component.class, componentImpl);

            assertSame(componentImpl, container.get(Component.class));
        }

        @Nested
        class InjectWithConstructorTest {

            @Test
            void should_return_a_component_when_get_if_the_bind_component_no_dependency() {
                container.bind(Component.class, ComponentNoDependency.class);

                assertNotNull(container.get(Component.class));
                assertTrue(container.get(Component.class) instanceof Component);
            }

            @Test
            void should_return_a_correct_component_when_get_if_the_bind_component_has_dependency() {
                container.bind(AnotherComponent.class, ComponentWithDependency.class);
                container.bind(Component.class, ComponentNoDependency.class);

                AnotherComponent component = container.get(AnotherComponent.class);

                assertNotNull(component);
                assertTrue(component instanceof ComponentWithDependency);
                assertNotNull(((ComponentWithDependency) component).getDependency());
            }

            @Test
            void should_return_a_correct_component_when_get_if_bind_transitive_dependency_component() {
                container.bind(AnotherComponent.class, ComponentWithDependency.class);
                container.bind(Component.class, ComponentDependencyString.class);
                container.bind(String.class, "dependency");

                AnotherComponent component = container.get(AnotherComponent.class);

                assertNotNull(component);
                assertTrue(component instanceof ComponentWithDependency);

                Component dependency = ((ComponentWithDependency) component).getDependency();
                assertNotNull(dependency);

                assertEquals("dependency", ((ComponentDependencyString) dependency).getDependency());
            }

            @Test
            void should_throw_Exception_when_bind_if_multi_inject_constructor_provided() {
                assertThrows(IllegalComponentException.class, () ->
                        container.bind(Component.class, ComponentWithMultiInjectConstructor.class));
            }

            @Test
            void should_throw_Exception_when_bind_if_no_inject_nor_default_constructor_provided() {
                assertThrows(IllegalComponentException.class, () ->
                        container.bind(Component.class, ComponentWithNoInjectNorDefaultConstructor.class));
            }

            private class ComponentWithDependency implements AnotherComponent {
                @Inject
                public ComponentWithDependency(Component dependency) {
                    this.dependency = dependency;
                }

                private Component dependency;

                public Component getDependency() {
                    return dependency;
                }
            }

            private class ComponentDependencyString implements Component {
                @Inject
                public ComponentDependencyString(String dependency) {
                    this.dependency = dependency;
                }

                private String dependency;

                public String getDependency() {
                    return dependency;
                }
            }

            private class ComponentWithMultiInjectConstructor implements Component {
                @Inject
                public ComponentWithMultiInjectConstructor(String name) {
                }

                @Inject
                public ComponentWithMultiInjectConstructor(Integer id) {
                }
            }

            private class ComponentWithNoInjectNorDefaultConstructor implements Component {
                public ComponentWithNoInjectNorDefaultConstructor(String name) {
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

interface AnotherComponent {

}

class ComponentNoDependency implements Component {

}
