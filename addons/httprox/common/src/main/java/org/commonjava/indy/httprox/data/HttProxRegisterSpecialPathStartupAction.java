/**
 * Copyright (C) 2011-2018 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.httprox.data;

import org.apache.commons.lang.StringUtils;
import org.commonjava.indy.action.IndyLifecycleException;
import org.commonjava.indy.action.StartupAction;
import org.commonjava.indy.httprox.conf.HttproxConfig;
import org.commonjava.maven.galley.io.SpecialPathSet;
import org.commonjava.maven.galley.model.FilePatternMatcher;
import org.commonjava.maven.galley.model.SpecialPathInfo;
import org.commonjava.maven.galley.spi.io.SpecialPathManager;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.commonjava.maven.galley.io.SpecialPathConstants.PKG_TYPE_GENERIC_HTTP;

/**
 * Created by ruhan on 4/17/17.
 */
public class HttProxRegisterSpecialPathStartupAction
                implements StartupAction
{
    @Inject
    private HttproxConfig config;

    @Inject
    private SpecialPathManager specialPathManager;

    @Override
    public String getId()
    {
        return "HttProx register special path";
    }

    @Override
    public void start() throws IndyLifecycleException
    {
        specialPathManager.registerSpecialPathSet( new HttpProxSpecialPathSet() );
    }

    @Override
    public int getStartupPriority()
    {
        return 0;
    }

    private class HttpProxSpecialPathSet
                    implements SpecialPathSet
    {
        final List<SpecialPathInfo> notCachableSpecialPaths = new ArrayList<>();

        HttpProxSpecialPathSet()
        {
            String s = config.getNoCachePatterns();
            if ( isNotBlank( s ) )
            {
                for ( String pattern : s.split( "," ) ) // split by comma
                {
                    String trimmed = pattern.trim();
                    if ( isNotBlank( trimmed ) )
                    {
                        notCachableSpecialPaths.add( SpecialPathInfo.from( new FilePatternMatcher( trimmed ) )
                                                                    .setCachable( false )
                                                                    .build() );
                    }
                }
            }
        }

        @Override
        public List<SpecialPathInfo> getSpecialPathInfos()
        {
            return notCachableSpecialPaths;
        }

        @Override
        public void registerSpecialPathInfo( SpecialPathInfo pathInfo )
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deregisterSpecialPathInfo( SpecialPathInfo pathInfo )
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getPackageType()
        {
            return PKG_TYPE_GENERIC_HTTP;
        }
    }
}
