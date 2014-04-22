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
package org.commonjava.aprox.conf;

import org.commonjava.web.config.section.MapSectionListener;

public abstract class AbstractAproxMapConfig
    extends MapSectionListener
{

    private String sectionName;

    protected AbstractAproxMapConfig()
    {
    }

    protected AbstractAproxMapConfig( final String sectionName )
    {
        this.sectionName = sectionName;
    }

    public String getSectionName()
    {
        return sectionName;
    }

}
