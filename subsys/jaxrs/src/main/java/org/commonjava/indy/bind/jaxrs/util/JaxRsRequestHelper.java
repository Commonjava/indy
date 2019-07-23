/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.indy.bind.jaxrs.util;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.commonjava.indy.util.AcceptInfo;
import org.commonjava.indy.util.AcceptInfoParser;
import org.commonjava.indy.util.ApplicationContent;
import org.commonjava.indy.util.ApplicationHeader;

@ApplicationScoped
public class JaxRsRequestHelper
{
    @Inject
    private AcceptInfoParser parser;

    public AcceptInfo findAccept( final HttpServletRequest request, final String defaultAccept )
    {
        final List<AcceptInfo> accepts = parser.parse( request.getHeaders( ApplicationHeader.accept.key() ) );
        AcceptInfo selectedAccept = null;
        for ( final AcceptInfo accept : accepts )
        {
            final String sa = ApplicationContent.getStandardAccept( accept.getBaseAccept() );
            if ( sa != null )
            {
                selectedAccept = accept;
                break;
            }
        }

        if ( selectedAccept == null )
        {
            selectedAccept = new AcceptInfo( defaultAccept, defaultAccept, parser.getDefaultVersion() );
        }

        return selectedAccept;
    }

}
