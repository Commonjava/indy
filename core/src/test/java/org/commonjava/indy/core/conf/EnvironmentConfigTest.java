/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.core.conf;

import org.commonjava.indy.conf.EnvironmentConfig;
import org.commonjava.indy.test.utils.WeldJUnit4Runner;
import org.commonjava.propulsor.config.ConfigUtils;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created by yma on 2019/3/21.
 */
@RunWith( WeldJUnit4Runner.class )
public class EnvironmentConfigTest
{
    @Inject
    private Instance<EnvironmentConfig> instance;

    @Test
    public void weldInjection_IterateIndyLoggingConfigurators()
    {
        List<String> sections = new ArrayList<>();
        instance.iterator().forEachRemaining( ( instance)->{
            String section = ConfigUtils.getSectionName( instance );
            System.out.printf( "Got instance: %s with section: %s\n", instance, section );
            sections.add( section );
        } );

        System.out.println(sections);
        assertThat( sections.contains( EnvironmentConfig.SECTION_NAME ), equalTo( true ) );
    }
}
