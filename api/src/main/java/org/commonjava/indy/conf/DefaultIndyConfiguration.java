/**
 * Copyright (C) 2011-2017 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.conf;

import org.commonjava.web.config.annotation.ConfigName;
import org.commonjava.web.config.annotation.SectionName;
import org.commonjava.web.config.section.ConfigurationSectionListener;

import javax.enterprise.context.ApplicationScoped;
import java.io.File;
import java.io.InputStream;

import static org.apache.commons.lang.StringUtils.isEmpty;

@SectionName( ConfigurationSectionListener.DEFAULT_SECTION )
@ApplicationScoped
public class DefaultIndyConfiguration
    implements IndyConfiguration, IndyConfigInfo
{

    public static final int DEFAULT_PASSTHROUGH_TIMEOUT_SECONDS = 300;

    public static final int DEFAULT_NOT_FOUND_CACHE_TIMEOUT_SECONDS = 300;

    public static final int DEFAULT_REQUEST_TIMEOUT_SECONDS = 30; // 5 seconds (previous) is crazy on most normal networks

    public static final int DEFAULT_STORE_DISABLE_TIMEOUT_SECONDS = 1800; // 30 minutes

    public static final int DEFAULT_NFC_EXPIRATION_SWEEP_MINUTES = 30;

    public static final int DEFAULT_NFC_MAX_RESULT_SET_SIZE = 10000; // 10,000

    private Integer passthroughTimeoutSeconds;

    private Integer notFoundCacheTimeoutSeconds;

    private Integer requestTimeoutSeconds;

    private Integer storeDisableTimeoutSeconds;

    private Integer nfcExpirationSweepMinutes;

    private Integer nfcMaxResultSetSize;

    public DefaultIndyConfiguration()
    {
    }

    @Override
    public int getPassthroughTimeoutSeconds()
    {
        return passthroughTimeoutSeconds == null ? DEFAULT_PASSTHROUGH_TIMEOUT_SECONDS : passthroughTimeoutSeconds;
    }

    @ConfigName( "passthrough.timeout" )
    public void setPassthroughTimeoutSeconds( final int seconds )
    {
        passthroughTimeoutSeconds = seconds;
    }

    @ConfigName( "nfc.timeout" )
    public void setNotFoundCacheTimeoutSeconds( final int seconds )
    {
        notFoundCacheTimeoutSeconds = seconds;
    }

    @Override
    public int getNotFoundCacheTimeoutSeconds()
    {
        return notFoundCacheTimeoutSeconds == null ? DEFAULT_NOT_FOUND_CACHE_TIMEOUT_SECONDS : notFoundCacheTimeoutSeconds;
    }

    @Override
    public int getRequestTimeoutSeconds()
    {
        return requestTimeoutSeconds == null ? DEFAULT_REQUEST_TIMEOUT_SECONDS : requestTimeoutSeconds;
    }

    @ConfigName( "request.timeout" )
    public void setRequestTimeoutSeconds( final Integer requestTimeoutSeconds )
    {
        this.requestTimeoutSeconds = requestTimeoutSeconds;
    }

    @Override
    public int getStoreDisableTimeoutSeconds()
    {
        return storeDisableTimeoutSeconds == null ? DEFAULT_STORE_DISABLE_TIMEOUT_SECONDS : storeDisableTimeoutSeconds;
    }

    @ConfigName( "nfc.sweep.minutes" )
    public void setDefaultNfcExpirationSweepMinutes( final int minutes )
    {
        this.nfcExpirationSweepMinutes = minutes;
    }

    @ConfigName( "nfc.maxresultsetsize" )
    public void setDefaultNfcMaxResultSetSize( final int size )
    {
        this.nfcMaxResultSetSize = size;
    }

    @Override
    public int getNfcExpirationSweepMinutes()
    {
        return nfcExpirationSweepMinutes == null ? DEFAULT_NFC_EXPIRATION_SWEEP_MINUTES : nfcExpirationSweepMinutes;
    }

    @Override
    public int getNfcMaxResultSetSize()
    {
        return nfcMaxResultSetSize == null ? DEFAULT_NFC_MAX_RESULT_SET_SIZE : nfcMaxResultSetSize;
    }

    @Override
    public File getIndyHomeDir()
    {
        return getSyspropDir( IndyConfigFactory.INDY_HOME_PROP );
    }

    @Override
    public File getIndyConfDir()
    {
        return getSyspropDir( IndyConfigFactory.CONFIG_DIR_PROP );
    }

    private File getSyspropDir( final String property )
    {
        String dir = System.getProperty( property );
        return isEmpty(dir) ? null : new File( dir );
    }

    @ConfigName( "store.disable.timeout" )
    public void setStoreDisableTimeoutSeconds( final Integer storeDisableTimeoutSeconds )
    {
        this.storeDisableTimeoutSeconds = storeDisableTimeoutSeconds;
    }

    @Override
    public String getDefaultConfigFileName()
    {
        return IndyConfigInfo.APPEND_DEFAULTS_TO_MAIN_CONF;
    }

    @Override
    public InputStream getDefaultConfig()
    {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream( "default-main.conf" );
    }

}
