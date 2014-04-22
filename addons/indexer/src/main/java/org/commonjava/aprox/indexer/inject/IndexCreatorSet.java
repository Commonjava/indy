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
package org.commonjava.aprox.indexer.inject;

import java.util.List;

import org.apache.maven.index.context.IndexCreator;

public class IndexCreatorSet
{

    private final List<IndexCreator> creators;

    public IndexCreatorSet( final List<IndexCreator> creators )
    {
        this.creators = creators;
    }

    public List<IndexCreator> getCreators()
    {
        return creators;
    }

}
