package org.commonjava.aprox.core.inject;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;

import org.commonjava.aprox.filer.KeyBasedPathGenerator;
import org.commonjava.maven.galley.io.NoOpTransferDecorator;
import org.commonjava.maven.galley.spi.io.PathGenerator;
import org.commonjava.maven.galley.spi.io.TransferDecorator;

@ApplicationScoped
public class GalleyProvider
{

    private TransferDecorator decorator;

    private PathGenerator pathgen;

    @PostConstruct
    public void setup()
    {
        decorator = new NoOpTransferDecorator();
        pathgen = new KeyBasedPathGenerator();
    }

    @Produces
    @Default
    public PathGenerator getPathGenerator()
    {
        return pathgen;
    }

    @Produces
    @Default
    public TransferDecorator getTransferDecorator()
    {
        return decorator;
    }

}
