package org.commonjava.aprox.bind.jaxrs.util;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.commonjava.aprox.util.AcceptInfo;
import org.commonjava.aprox.util.AcceptInfo.AcceptInfoParser;
import org.commonjava.aprox.util.ApplicationContent;
import org.commonjava.aprox.util.ApplicationHeader;

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
