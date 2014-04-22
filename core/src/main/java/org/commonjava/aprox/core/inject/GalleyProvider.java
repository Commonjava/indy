/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.aprox.core.inject;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;

import org.commonjava.maven.galley.io.NoOpTransferDecorator;
import org.commonjava.maven.galley.nfc.MemoryNotFoundCache;
import org.commonjava.maven.galley.spi.io.TransferDecorator;
import org.commonjava.maven.galley.spi.nfc.NotFoundCache;

@ApplicationScoped
public class GalleyProvider
{

    private TransferDecorator decorator;

    //    private PathGenerator pathgen;

    private NotFoundCache nfc;

    @PostConstruct
    public void setup()
    {
        decorator = new NoOpTransferDecorator();
        //        pathgen = new KeyBasedPathGenerator();
        nfc = new MemoryNotFoundCache();
    }

    //
    //    @Produces
    //    @Default
    //    public PathGenerator getPathGenerator()
    //    {
    //        return pathgen;
    //    }

    @Produces
    @Default
    public TransferDecorator getTransferDecorator()
    {
        return decorator;
    }

    @Produces
    @Default
    public NotFoundCache getNotFoundCache()
    {
        return nfc;
    }

}
