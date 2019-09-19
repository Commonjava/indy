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

import org.apache.commons.io.FileUtils;
import org.commonjava.indy.conf.IndyConfigFactory;
import org.commonjava.indy.conf.IndyConfigInfo;
import org.commonjava.indy.conf.IndyConfiguration;
import org.commonjava.indy.test.utils.WeldJUnit4Runner;
import org.commonjava.propulsor.config.ConfigUtils;
import org.commonjava.propulsor.config.ConfigurationException;
import org.commonjava.propulsor.config.section.ConfigurationSectionListener;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.commonjava.indy.conf.IndyConfiguration.PROP_NODE_ID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created by jdcasey on 3/9/16.
 */
@RunWith( WeldJUnit4Runner.class )
public class DefaultIndyConfigFactoryTest
{

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Inject
    private Instance<IndyConfigInfo> instance;

    @Test
    public void weldInjection_IterateIndyConfigurators()
    {
        List<String> sections = new ArrayList<>();
        instance.iterator().forEachRemaining( ( instance)->{
            String section = ConfigUtils.getSectionName( instance );
            System.out.printf( "Got instance: %s with section: %s\n", instance, section );
            sections.add( section );
        } );

        System.out.println(sections);
        assertThat( sections.contains( ConfigurationSectionListener.DEFAULT_SECTION ), equalTo( true ) );
        assertThat( sections.contains( "flatfiles" ), equalTo( true ) );
        assertThat( sections.contains( "ui" ), equalTo( true ) );
        assertThat( sections.contains( "storage-default" ), equalTo( true ) );
    }

    @Inject
    private DefaultIndyConfigFactory factory;

    @Inject
    private IndyConfiguration config;

    @Test
    public void weldInjection_writeDefaults()
            throws IOException, ConfigurationException
    {
        File dir = temp.newFolder( "indy-config" );
        dir.mkdirs();

        factory.writeDefaultConfigs( dir );

        assertThat( new File( dir, "main.conf" ).exists(), equalTo( true ) );
    }

    @Test
    public void readNodeIdAs_USER_FromEnvarExpression()
            throws IOException, ConfigurationException
    {
        File dir = temp.newFolder( "indy-config" );
        File mainConf = new File( dir, "main.conf" );
        FileUtils.write( mainConf, PROP_NODE_ID + "=${env.USER}" );

        String user = System.getenv( "USER" );
        factory.load( mainConf.getAbsolutePath() );
        if ( user != null ) // because in some case USER is not in env and it won't be populated
        {
            assertThat( config.getNodeId(), equalTo( user ) );
        }
    }

}
