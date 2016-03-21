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
package org.commonjava.indy.depgraph.conf;

import org.commonjava.indy.subsys.datafile.DataFileManager;
import org.commonjava.web.config.ConfigurationException;
import org.commonjava.web.config.ConfigurationListener;
import org.commonjava.web.config.section.ConfigurationSectionListener;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Collections;
import java.util.Map;

/**
 * Created by jdcasey on 3/9/16.
 */
@ApplicationScoped
public class IndyDepgraphConfigListener
        implements ConfigurationListener
{
    @Inject
    private DataFileManager dataFileManager;

    @Inject
    private IndyDepgraphConfig config;

    @Override
    public Map<String, ConfigurationSectionListener<?>> getSectionListeners()
    {
        return Collections.emptyMap();
    }

    @Override
    public void configurationComplete()
            throws ConfigurationException
    {
        config.setDirectories( dataFileManager.getDetachedDataBasedir(), dataFileManager.getDetachedWorkBasedir() );
    }
}
