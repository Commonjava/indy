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

import java.util.Collections;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;

import org.commonjava.maven.galley.io.ChecksummingTransferDecorator;
import org.commonjava.maven.galley.io.checksum.Md5GeneratorFactory;
import org.commonjava.maven.galley.io.checksum.Sha1GeneratorFactory;
import org.commonjava.maven.galley.model.TransferOperation;
import org.commonjava.maven.galley.spi.io.TransferDecorator;

@ApplicationScoped
public class GalleyProvider
{

    private TransferDecorator decorator;

    @PostConstruct
    public void setup()
    {
        decorator =
            new ChecksummingTransferDecorator( Collections.singleton( TransferOperation.GENERATE ),
                                               new Md5GeneratorFactory(), new Sha1GeneratorFactory() );
    }

    @Produces
    @Default
    public TransferDecorator getTransferDecorator()
    {
        return decorator;
    }

}
