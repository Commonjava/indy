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
package org.commonjava.aprox.bind.jaxrs.util;

import org.commonjava.aprox.util.UriFormatter;
import org.commonjava.maven.galley.util.PathUtils;

public class JaxRsUriFormatter
    implements UriFormatter
{

    @Override
    public String formatAbsolutePathTo( final String base, final String... parts )
    {
        String path = PathUtils.normalize( base, PathUtils.normalize( parts ) );
        if ( !path.startsWith( "/" ) )
        {
            path = "/" + path;
        }

        return path;
    }

}
