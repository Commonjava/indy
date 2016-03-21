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
package org.commonjava.indy.subsys.infinispan.conf;

import org.commonjava.indy.conf.IndyConfigInfo;
import org.commonjava.web.config.annotation.ConfigName;
import org.commonjava.web.config.annotation.SectionName;

import java.io.File;
import java.io.InputStream;

/**
 * Created by jdcasey on 3/10/16.
 */
@SectionName("infinispan")
public class InfinispanSubsystemConfig
        implements IndyConfigInfo
{
    public static final String ISPN_XML = "infinispan.xml";

    private File infinispanXml;

    public File getInfinispanXml()
    {
        return infinispanXml;
    }

    @ConfigName( InfinispanSubsystemConfig.ISPN_XML )
    public void setInfinispanXml( File infinispanXml )
    {
        this.infinispanXml = infinispanXml;
    }

    @Override
    public String getDefaultConfigFileName()
    {
        return "infinispan.xml";
    }

    @Override
    public InputStream getDefaultConfig()
    {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream( ISPN_XML );
    }
}
