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
package org.commonjava.aprox.dotmaven.jaxrs;

import javax.enterprise.context.ApplicationScoped;
import javax.servlet.annotation.WebServlet;

import org.commonjava.web.dav.servlet.WebdavServlet;

@WebServlet( name = "dotMavenDAV", urlPatterns = { "/mavdav/*" } )
@ApplicationScoped
public class DotMavenServlet
    extends WebdavServlet
{

    private static final long serialVersionUID = 1L;

}
