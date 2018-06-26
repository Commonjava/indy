package org.commonjava.indy.bind.jaxrs;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.commonjava.indy.conf.IndyConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import static org.commonjava.indy.bind.jaxrs.RequestContextConstants.CLIENT_ADDR;
import static org.commonjava.indy.bind.jaxrs.RequestContextConstants.COMPONENT_ID;
import static org.commonjava.indy.bind.jaxrs.RequestContextConstants.EXTERNAL_ID;
import static org.commonjava.indy.bind.jaxrs.RequestContextConstants.INTERNAL_ID;
import static org.commonjava.indy.bind.jaxrs.RequestContextConstants.PREFERRED_ID;

@ApplicationScoped
public class MDCManager
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private IndyConfiguration config;

    public MDCManager() {}

    public void clear()
    {
        MDC.clear();
    }

    private List<String> mdcHeadersList = new ArrayList();

    @PostConstruct
    public void setUp()
    {
        mdcHeadersList.add( COMPONENT_ID ); // default

        String mdcHeaders = config.getMdcHeaders();
        if ( isNotBlank( mdcHeaders ) )
        {
            String[] headers = mdcHeaders.split( "," );
            for ( String s : headers )
            {
                if ( isNotBlank( s ) && !mdcHeadersList.contains( s ) )
                {
                    mdcHeadersList.add( s.trim() );
                }
            }
        }
    }

    public void putExternalID( String externalID )
    {
        if ( externalID == null )
        {
            logger.debug( "No externalId" );
            return;
        }
        String internalID = UUID.randomUUID().toString();
        String preferredID = externalID != null ? externalID : internalID;
        putRequestIDs( internalID, externalID, preferredID );
    }

    public void putRequestIDs( String internalID, String externalID, String preferredID )
    {
        MDC.put( INTERNAL_ID, internalID );
        MDC.put( EXTERNAL_ID, externalID );
        MDC.put( PREFERRED_ID, preferredID );
    }

    public void putUserIP( String userIp )
    {
        MDC.put( CLIENT_ADDR, userIp );
    }

    public void putExtraHeaders( HttpServletRequest request )
    {
        mdcHeadersList.forEach( ( header ) -> MDC.put( header, request.getHeader( header ) ) );
    }

    public void putExtraHeaders( HttpRequest httpRequest )
    {
        mdcHeadersList.forEach( ( header ) -> {
            Header h = httpRequest.getFirstHeader( header );
            if ( h != null )
            {
                MDC.put( header, h.getValue() );
            }
        } );
    }
}
