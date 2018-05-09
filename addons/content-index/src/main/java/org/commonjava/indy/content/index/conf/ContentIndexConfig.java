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

    private static final Boolean DEFAULT_AUTHORITATIVE_INDEXES = Boolean.FALSE;

    private Boolean authoritativeIndex;

    public ContentIndexConfig()
    {
    }

    public ContentIndexConfig( final boolean authIndex )
    {
        this.authoritativeIndex = authIndex;
    }

    public Boolean isAuthoritativeIndex()
    {
        return authoritativeIndex == null ? DEFAULT_AUTHORITATIVE_INDEXES : authoritativeIndex;
    }

    @ConfigName( ContentIndexConfig.AUTH_INDEX_PARAM )
    public void setAuthoritativeIndex( Boolean authoritativeIndex )
    {
        this.authoritativeIndex = authoritativeIndex;
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
