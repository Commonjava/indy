package org.commonjava.aprox.subsys.maven;

public class MavenComponentDefinition<I, T extends I>
{

    private final Class<I> compClass;

    private final Class<T> implClass;

    private String overriddenHint;

    private final String hint;

    public MavenComponentDefinition( final Class<I> compClass, final String overriddenHint, final Class<T> implClass,
                                     final String hint )
    {
        this.compClass = compClass;
        this.implClass = implClass;
        this.overriddenHint = overriddenHint;
        this.hint = hint;
    }

    public MavenComponentDefinition( final Class<I> compClass, final Class<T> implClass, final String hint )
    {
        this.compClass = compClass;
        this.implClass = implClass;
        this.hint = hint;
    }

    public Class<I> getComponentClass()
    {
        return compClass;
    }

    public Class<T> getImplementationClass()
    {
        return implClass;
    }

    public String getHint()
    {
        return hint;
    }

    public String getOverriddenHint()
    {
        return overriddenHint;
    }

}
