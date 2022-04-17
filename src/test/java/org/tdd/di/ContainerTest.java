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
