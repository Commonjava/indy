/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.commonjava.aprox.depgraph.json;

import com.fasterxml.jackson.core.io.SerializedString;

public final class SerializationConstants
{

    private SerializationConstants()
    {
    }

    public static final SerializedString SOURCE_URIS = new SerializedString( "source-uris" );

    public static final SerializedString SOURCE_URI = new SerializedString( "source-uri" );

    public static final SerializedString POM_LOCATION_URI = new SerializedString( "pom-location-uri" );

    public static final SerializedString PROJECT_VERSION = new SerializedString( "gav" );

    public static final SerializedString GAV = PROJECT_VERSION;

    public static final SerializedString RELATIONSHIP_TYPE = new SerializedString( "rel" );

    public static final SerializedString DECLARING_REF = new SerializedString( "declaring" );

    public static final SerializedString TARGET_REF = new SerializedString( "target" );

    public static final SerializedString INDEX = new SerializedString( "idx" );

    public static final SerializedString MANAGED = new SerializedString( "managed" );

    public static final SerializedString SCOPE = new SerializedString( "scope" );

    public static final SerializedString PLUGIN_REF = new SerializedString( "plugin" );

    public static final SerializedString JSON_VERSION = new SerializedString( "jsonVersion" );

    public static final int CURRENT_JSON_VERSION = 1;

    public static final SerializedString EPROJECT_KEY = new SerializedString( "ekey" );

    public static final SerializedString RELATIONSHIPS = new SerializedString( "relationships" );

    public static final SerializedString EPROFILES = new SerializedString( "eprofiles" );

    public static final SerializedString CYCLES = new SerializedString( "cycles" );

    public static final SerializedString WEB_ROOTS = new SerializedString( "gavs" );

    public static final SerializedString GAVS = WEB_ROOTS;

}
