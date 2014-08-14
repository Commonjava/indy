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
package org.commonjava.aprox.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class ApplicationContent
{

    public static final String application_json = "application/json";

    public static final String application_xml = "application/xml";

    public static final String application_zip = "application/zip";

    public static final String text_plain = "text/plain";

    public static final String text_html = "text/html";

    public static String aprox_json = "application/aprox+json";

    public static String aprox_xml = "application/aprox+xml";

    public static String aprox_zip = "application/aprox+zip";

    public static String aprox_plain = "application/aprox+plain";

    public static String aprox_html = "application/aprox+html";

    private ApplicationContent()
    {
    }

    private static final Map<String, String> APROX_ACCEPTS = Collections.unmodifiableMap( new HashMap<String, String>()
    {
        {
            put( application_json, aprox_json );
            put( text_html, aprox_html );
            put( text_plain, aprox_plain );
            put( application_zip, aprox_zip );
            put( application_xml, aprox_xml );
        }

        private static final long serialVersionUID = 1L;
    } );

    private static final Map<String, String> STANDARD_ACCEPTS =
        Collections.unmodifiableMap( new HashMap<String, String>()
        {
            {
                put( aprox_json, application_json );
                put( aprox_html, text_html );
                put( aprox_plain, text_plain );
                put( aprox_zip, application_zip );
                put( aprox_xml, application_xml );
            }

            private static final long serialVersionUID = 1L;
        } );

    public static String getAproxAccept( final String standardAccept )
    {
        return standardAccept == null ? null : APROX_ACCEPTS.get( standardAccept );
    }

    public static String getStandardAccept( final String aproxAccept )
    {
        return aproxAccept == null ? null : STANDARD_ACCEPTS.get( aproxAccept );
    }
}
