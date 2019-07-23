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
package org.commonjava.indy.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class ApplicationContent
{

    public static final String application_json = "application/json";

    public static final String application_javascript = "application/javascript";

    public static final String application_xml = "application/xml";

    public static final String application_zip = "application/zip";

    public static final String text_css = "text/css";

    public static final String text_html = "text/html";

    public static final String text_plain = "text/plain";

    public static String indy_json = "application/indy+json";

    public static String indy_xml = "application/indy+xml";

    public static String indy_zip = "application/indy+zip";

    public static String indy_plain = "application/indy+plain";

    public static String indy_html = "application/indy+html";

    private ApplicationContent()
    {
    }

    private static final Map<String, String> INDY_ACCEPTS = Collections.unmodifiableMap( new HashMap<String, String>()
    {
        {
            put( application_json, indy_json );
            put( text_html, indy_html );
            put( text_plain, indy_plain );
            put( application_zip, indy_zip );
            put( application_xml, indy_xml );
            put( indy_json, indy_json );
            put( indy_html, indy_html );
            put( indy_plain, indy_plain );
            put( indy_zip, indy_zip );
            put( indy_xml, indy_xml );
        }

        private static final long serialVersionUID = 1L;
    } );

    private static final Map<String, String> STANDARD_ACCEPTS =
        Collections.unmodifiableMap( new HashMap<String, String>()
        {
            {
                put( indy_json, application_json );
                put( indy_html, text_html );
                put( indy_plain, text_plain );
                put( indy_zip, application_zip );
                put( indy_xml, application_xml );
                put( application_json, application_json );
                put( text_html, text_html );
                put( text_plain, text_plain );
                put( application_zip, application_zip );
                put( application_xml, application_xml );
            }

            private static final long serialVersionUID = 1L;
        } );

    public static String getIndyAccept( final String standardAccept )
    {
        return standardAccept == null ? null : INDY_ACCEPTS.get( standardAccept );
    }

    public static String getStandardAccept( final String indyAccept )
    {
        return indyAccept == null ? null : STANDARD_ACCEPTS.get( indyAccept );
    }
}
