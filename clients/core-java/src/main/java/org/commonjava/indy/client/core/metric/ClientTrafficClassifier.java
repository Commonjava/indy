/**
 * Copyright (C) 2011-2020 Red Hat, Inc. (https://github.com/Commonjava/indy)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.indy.client.core.metric;

import org.apache.http.Header;
import org.apache.http.client.methods.HttpUriRequest;
import org.commonjava.o11yphant.metrics.TrafficClassifier;

import javax.enterprise.inject.Alternative;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.join;
import static org.commonjava.indy.client.core.metric.ClientMetricConstants.CLIENT_CONTENT;
import static org.commonjava.indy.client.core.metric.ClientMetricConstants.CLIENT_FOLO_ADMIN;
import static org.commonjava.indy.client.core.metric.ClientMetricConstants.CLIENT_FOLO_CONTENT;
import static org.commonjava.indy.client.core.metric.ClientMetricConstants.CLIENT_PROMOTE;
import static org.commonjava.indy.client.core.metric.ClientMetricConstants.CLIENT_REPO_MGMT;


@Alternative
public class ClientTrafficClassifier
        implements TrafficClassifier
{

    public static final Set<String> FOLO_RECORD_ENDPOINTS = new HashSet<>( asList( "record", "report" ) );

    @Override
    public List<String> classifyFunctions( String restPath, String method, Map<String, String> headers ) {
        return calculateCachedFunctionClassifiers( restPath, method, headers );
    }

    public List<String> calculateClassifiers( HttpUriRequest request ) {
        Map<String, String> headers = new HashMap<>();
        for ( Header header : request.getAllHeaders() )
        {
            headers.put( header.getName(), header.getValue() );
        }
        return calculateCachedFunctionClassifiers( request.getURI().getPath(), request.getMethod(), headers );
    }

    protected List<String> calculateCachedFunctionClassifiers( String restPath, String method, Map<String, String> headers ) {
        List<String> result = new ArrayList<>();
        String[] pathParts = restPath.split( "/" );

        if ( pathParts.length >= 2 ) {
            String[] classifierParts = new String[ pathParts.length - 1 ];
            System.arraycopy( pathParts, 1, classifierParts, 0, classifierParts.length );

            String restPrefix = join( classifierParts, '/' );
            if ( restPrefix.startsWith( "folo/admin/" ) && FOLO_RECORD_ENDPOINTS.contains( classifierParts[ 3 ] ) ) {
                result = singletonList( CLIENT_FOLO_ADMIN );
            } else if ( restPrefix.startsWith( "folo/track/" ) && classifierParts.length > 6 ) {
                result = singletonList( CLIENT_FOLO_CONTENT );
            } else if ( "admin".equals( classifierParts[ 0 ] ) && "stores".equals( classifierParts[ 1 ] )
                    && classifierParts.length > 2 ) {
                result = singletonList( CLIENT_REPO_MGMT );
            } else if ( ( "content".equals( classifierParts[ 0 ] ) && classifierParts.length > 5 ) ) {
                result = singletonList( CLIENT_CONTENT );
            } else if ( restPrefix.startsWith( "promotion/paths/" ) ) {
                result = singletonList( CLIENT_PROMOTE );
            }
        }
        return result;
    }
}
