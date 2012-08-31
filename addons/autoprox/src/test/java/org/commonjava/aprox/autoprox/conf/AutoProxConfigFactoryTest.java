package org.commonjava.aprox.autoprox.conf;

import static org.apache.commons.io.FileUtils.write;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.List;

import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class AutoProxConfigFactoryTest
{

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Test
    public void loadWithProxyBaseOnly()
        throws Exception
    {
        final String proxyBase = "http://foo.bar/baz";
        final String confContent = "[repository]\nbase.url = " + proxyBase;

        final File conf = temp.newFile( "autoprox.conf" );
        write( conf, confContent );

        final AutoProxConfiguration config = new AutoProxConfigFactory( conf ).getConfiguration();

        assertThat( config, notNullValue() );
        assertThat( config.isEnabled(), equalTo( true ) );

        assertThat( config.getRepo(), notNullValue() );
        assertThat( config.getRepo()
                          .getBaseUrl(), equalTo( proxyBase ) );

        assertThat( config.getDeploy(), notNullValue() );
        assertThat( config.getDeploy()
                          .isDeployEnabled(), equalTo( false ) );

        assertThat( config.getGroup(), notNullValue() );
        assertThat( config.getGroup()
                          .getExtraConstituents(), nullValue() );
    }

    @Test
    public void loadDisabled()
        throws Exception
    {
        final String proxyBase = "http://foo.bar/baz";
        final String confContent = "enabled = false\n\n[repository]\nbase.url = " + proxyBase;

        final File conf = temp.newFile( "autoprox.conf" );
        write( conf, confContent );

        final AutoProxConfiguration config = new AutoProxConfigFactory( conf ).getConfiguration();

        assertThat( config, notNullValue() );
        assertThat( config.isEnabled(), equalTo( false ) );

        assertThat( config.getRepo(), notNullValue() );
        assertThat( config.getRepo()
                          .getBaseUrl(), equalTo( proxyBase ) );

        assertThat( config.getDeploy(), notNullValue() );
        assertThat( config.getDeploy()
                          .isDeployEnabled(), equalTo( false ) );

        assertThat( config.getGroup(), notNullValue() );
        assertThat( config.getGroup()
                          .getExtraConstituents(), nullValue() );
    }

    @Test
    public void loadWithDeploymentCreationEnabled()
        throws Exception
    {
        final String proxyBase = "http://foo.bar/baz";
        final String confContent = "[repository]\nbase.url = " + proxyBase + "\n\n[deploy]\nenabled = true";

        final File conf = temp.newFile( "autoprox.conf" );
        write( conf, confContent );

        final AutoProxConfiguration config = new AutoProxConfigFactory( conf ).getConfiguration();

        assertThat( config, notNullValue() );
        assertThat( config.isEnabled(), equalTo( true ) );

        assertThat( config.getRepo(), notNullValue() );
        assertThat( config.getRepo()
                          .getBaseUrl(), equalTo( proxyBase ) );

        assertThat( config.getDeploy(), notNullValue() );
        assertThat( config.getDeploy()
                          .isDeployEnabled(), equalTo( true ) );

        assertThat( config.getGroup(), notNullValue() );
        assertThat( config.getGroup()
                          .getExtraConstituents(), nullValue() );
    }

    @Test
    public void loadWithGroupAppend_TwoRepos()
        throws Exception
    {
        final String proxyBase = "http://foo.bar/baz";
        final String confContent =
            "[repository]\nbase.url = " + proxyBase + "\n\n[group]\nappend.constituents = <first,repository:second";

        final File conf = temp.newFile( "autoprox.conf" );
        write( conf, confContent );

        final AutoProxConfiguration config = new AutoProxConfigFactory( conf ).getConfiguration();

        assertThat( config, notNullValue() );
        assertThat( config.isEnabled(), equalTo( true ) );

        assertThat( config.getRepo(), notNullValue() );
        assertThat( config.getRepo()
                          .getBaseUrl(), equalTo( proxyBase ) );

        assertThat( config.getDeploy(), notNullValue() );
        assertThat( config.getDeploy()
                          .isDeployEnabled(), equalTo( false ) );

        assertThat( config.getGroup(), notNullValue() );

        final List<StoreKey> constituents = config.getGroup()
                                                  .getExtraConstituents();
        assertThat( constituents, notNullValue() );
        assertThat( constituents.size(), equalTo( 2 ) );

        int idx = 0;
        StoreKey key = constituents.get( idx );
        assertThat( key.getType(), equalTo( StoreType.repository ) );
        assertThat( key.getName(), equalTo( "first" ) );

        idx++;
        key = constituents.get( idx );
        assertThat( key.getType(), equalTo( StoreType.repository ) );
        assertThat( key.getName(), equalTo( "second" ) );
    }

}
