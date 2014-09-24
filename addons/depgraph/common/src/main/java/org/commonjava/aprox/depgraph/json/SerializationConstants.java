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
