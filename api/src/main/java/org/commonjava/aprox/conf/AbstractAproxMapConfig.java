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
package org.commonjava.aprox.conf;

import org.commonjava.web.config.section.MapSectionListener;

/**
 * Abstract class designed to make it simpler to create a subsystem or add-on configuration that has no specific set of configuration parameters. 
 * Instead, it knows how to accommodate a flexible mapping of key=value pairs into a usable configuration.
 */
public abstract class AbstractAproxMapConfig
    extends MapSectionListener
    implements AproxConfigInfo
{

    private String sectionName;

    protected AbstractAproxMapConfig()
    {
    }

    protected AbstractAproxMapConfig( final String sectionName )
    {
        this.sectionName = sectionName;
    }

    /**
     * Return the name of the configuration subsection that pertains to this configuration class.
     */
    @Override
    public String getSectionName()
    {
        return sectionName;
    }

}
