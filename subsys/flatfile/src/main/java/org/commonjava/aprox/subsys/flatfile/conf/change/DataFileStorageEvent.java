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
package org.commonjava.aprox.subsys.flatfile.conf.change;

import org.commonjava.maven.galley.model.TransferOperation;
import org.commonjava.maven.galley.model.Transfer;

public class DataFileStorageEvent
    extends DataFileEvent
{

    final TransferOperation type;

    public DataFileStorageEvent( final TransferOperation type, final Transfer transfer )
    {
        super( transfer );
        this.type = type;
    }

    public TransferOperation getType()
    {
        return type;
    }

    @Override
    public String getExtraInfo()
    {
        return "type=" + type.name();
    }

}
