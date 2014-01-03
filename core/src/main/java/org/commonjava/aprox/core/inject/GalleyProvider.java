/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
