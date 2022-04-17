package org.tdd.di;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ContainerTest {

    @Nested
    class ComponentConstruction {

        @Test
        void should_return_the_component_when_get_if_the_component_bind() {
            Container container = new Container();
            Component componentImpl = new Component(){};
            container.bind(Component.class, componentImpl);

            assertSame(componentImpl, container.get(Component.class));
        }

        @Test
        void should_return_a_component_when_get_if_the_bind_component_no_dependency() {
            Container container = new Container();
            container.bind(Component.class, ComponentNoDependency.class);

            assertNotNull(container.get(Component.class));
            assertTrue(container.get(Component.class) instanceof Component);
        }
    }


    @Nested
    class ComponentSelection {

    }

    @Nested
    class ComponentLifeCycle {

    }
}

interface Component {

}

class ComponentNoDependency implements Component {

}