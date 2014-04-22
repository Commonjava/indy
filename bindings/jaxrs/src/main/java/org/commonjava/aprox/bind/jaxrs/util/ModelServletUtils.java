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

import static org.commonjava.web.json.ser.ServletSerializerUtils.fromRequestBody;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.commonjava.aprox.inject.AproxData;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.HostedRepository;
import org.commonjava.aprox.model.RemoteRepository;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.web.json.ser.JsonSerializer;

@ApplicationScoped
public class ModelServletUtils
{

    @Inject
    @AproxData
    private JsonSerializer restSerializer;

    public ArtifactStore storeFromRequestBody( final StoreType st, final HttpServletRequest request )
    {
        switch ( st )
        {
            case hosted:
            {
                return fromRequestBody( request, restSerializer, HostedRepository.class );
            }
            case group:
            {
                return fromRequestBody( request, restSerializer, Group.class );
            }
            default:
            {
                return fromRequestBody( request, restSerializer, RemoteRepository.class );
            }
        }
    }

}
