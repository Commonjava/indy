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
package org.commonjava.aprox.stats;

import javax.inject.Singleton;

import org.commonjava.web.json.ser.JsonAdapters;

@Singleton
@JsonAdapters( AProxVersioningAdapter.class )
public class AProxVersioning
{
    
    private static final String APP_VERSION = "@project.version@";
    private static final String APP_BUILDER = "@user.name@";
    private static final String APP_COMMIT_ID = "@buildNumber@";
    private static final String APP_TIMESTAMP = "@timestamp@";
    
    public String getVersion()
    {
        return APP_VERSION;
    }

    public String getBuilder()
    {
        return APP_BUILDER;
    }

    public String getCommitId()
    {
        return APP_COMMIT_ID;
    }
    
    public String getTimestamp()
    {
        return APP_TIMESTAMP;
    }

}
