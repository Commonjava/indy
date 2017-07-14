/**
 * Copyright (C) 2013 Red Hat, Inc.
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
package org.commonjava.indy.content.index.conf;

import org.commonjava.indy.conf.IndyConfigInfo;
import org.commonjava.web.config.annotation.ConfigName;
import org.commonjava.web.config.annotation.SectionName;

import javax.enterprise.context.ApplicationScoped;
import java.io.InputStream;

@SectionName( ContentIndexConfig.SECTION )
@ApplicationScoped
public class ContentIndexConfig
        implements IndyConfigInfo
{
    public static final String SECTION = "content-index";

    public static final String AUTH_INDEX_PARAM = "support.authoritative.indexes";

    private boolean authIndex;

    public ContentIndexConfig()
    {
    }

    public ContentIndexConfig( final boolean authIndex )
    {
        this.authIndex = authIndex;
    }

    public boolean isAuthIndex()
    {
        return authIndex;
    }

    @ConfigName( ContentIndexConfig.AUTH_INDEX_PARAM )
    public void setAuthIndex( boolean authIndex )
    {
        this.authIndex = authIndex;
    }

    @Override
    public String getDefaultConfigFileName()
    {
        return "conf.d/content-index.conf";
    }

    @Override
    public InputStream getDefaultConfig()
    {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream( "default-content-index.conf" );
    }
}
