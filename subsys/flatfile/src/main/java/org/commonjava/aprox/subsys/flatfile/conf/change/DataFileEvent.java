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

import org.commonjava.maven.galley.model.Transfer;

public class DataFileEvent
{

    private final Transfer transfer;

    protected DataFileEvent( final Transfer transfer )
    {
        this.transfer = transfer;
    }

    public Transfer getTransfer()
    {
        return transfer;
    }

    public String getExtraInfo()
    {
        return "";
    }

    @Override
    public String toString()
    {
        return String.format( "%s [extra-info=%s, transfer=%s]", getClass().getSimpleName(), getExtraInfo(), transfer );
    }

}
