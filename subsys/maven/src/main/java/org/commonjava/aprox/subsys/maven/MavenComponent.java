package org.commonjava.aprox.subsys.maven;

public @interface MavenComponent
{

    Class<?> role();

    String hint();

}
