/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.aprox.depgraph.json;

public final class SerializationConstants
{

    private SerializationConstants()
    {
    }

    public static final String SOURCE_URIS = "source-uris";

    public static final String SOURCE_URI = "source-uri";

    public static final String POM_LOCATION_URI = "pom-location-uri";

    public static final String PROJECT_VERSION = "gav";

    public static final String GAV = PROJECT_VERSION;

    public static final String RELATIONSHIP_TYPE = "rel";

    public static final String DECLARING_REF = "declaring";

    public static final String TARGET_REF = "target";

    public static final String INDEX = "idx";

    public static final String MANAGED = "managed";

    public static final String SCOPE = "scope";

    public static final String PLUGIN_REF = "plugin";

    public static final String JSON_VERSION = "jsonVersion";

    public static final int CURRENT_JSON_VERSION = 1;

    public static final String EPROJECT_KEY = "ekey";

    public static final String RELATIONSHIPS = "relationships";

    public static final String EPROFILES = "eprofiles";

    public static final String CYCLES = "cycles";

    public static final String WEB_ROOTS = "gavs";

    public static final String GAVS = WEB_ROOTS;

}
