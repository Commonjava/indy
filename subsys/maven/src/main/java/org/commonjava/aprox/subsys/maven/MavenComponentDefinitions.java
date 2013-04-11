package org.commonjava.aprox.subsys.maven;

import java.util.Set;

public interface MavenComponentDefinitions
    extends Iterable<MavenComponentDefinition<?, ?>>
{

    Set<MavenComponentDefinition<?, ?>> getComponents();

}
