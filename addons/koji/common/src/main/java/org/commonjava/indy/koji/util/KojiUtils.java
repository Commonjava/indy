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
package org.commonjava.indy.koji.util;

import com.redhat.red.build.koji.model.xmlrpc.KojiBuildInfo;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.ObjectBasedValueSource;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;
import org.commonjava.indy.koji.conf.IndyKojiConfig;
import org.commonjava.indy.koji.content.KojiRepositoryCreator;
import org.commonjava.indy.koji.data.DefaultKojiRepoNameParser;
import org.commonjava.indy.koji.data.KojiRepoNameParser;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.subsys.template.IndyGroovyException;
import org.commonjava.indy.subsys.template.ScriptEngine;
import org.commonjava.atlas.maven.ident.ref.ProjectVersionRef;
import org.commonjava.atlas.maven.ident.util.ArtifactPathInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.net.MalformedURLException;
import java.util.regex.Pattern;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.commonjava.maven.galley.util.UrlUtils.buildUrl;

/**
 * Created by ruhan on 3/23/18.
 */
@ApplicationScoped
public class KojiUtils
{

    private static final String KOJI_REPO_CREATOR_SCRIPT = "koji-repo-creator.groovy";

    private static final String KOJI_REPO_NAME_PARSER_SCRIPT = "koji-repo-name-parser.groovy";

    @Inject
    private IndyKojiConfig config;

    @Inject
    private ScriptEngine scriptEngine;

    private KojiRepoNameParser kojiRepoNameParser;

    @PostConstruct
    public void setup()
    {
        KojiRepoNameParser parser =
                        createKojiRepoOperator( KojiRepoNameParser.class, KOJI_REPO_NAME_PARSER_SCRIPT, false );
        if ( parser == null )
        {
            kojiRepoNameParser = new DefaultKojiRepoNameParser();
        }
        else
        {
            kojiRepoNameParser = parser;
        }
    }

    public String formatStorageUrl( final String root, final KojiBuildInfo buildInfo )
                    throws MalformedURLException
    {
        String url;

        String volume = buildInfo.getVolumeName();
        if ( isDefaultVolume( volume ) )
        {
            url = buildUrl( root );
        }
        else
        {
            url = buildUrl( root, "vol", volume );
        }

        url = buildUrl( url, "packages", buildInfo.getName(), buildInfo.getVersion(), buildInfo.getRelease(), "maven" );
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.info( "Using Koji URL: {}", url );

        return url;
    }

    public boolean isDefaultVolume( String volume )
    {
        return isBlank( volume ) || "DEFAULT".equals( volume );
    }

    public String getBuildNvr( StoreKey storeKey )
    {
        return kojiRepoNameParser.parse( storeKey.getName() );
    }

    public boolean isBinaryBuild( KojiBuildInfo build )
    {
        return build.getTaskId() == null;
    }

    public String getRepositoryName( final KojiBuildInfo build )
    {
        StringSearchInterpolator interpolator = new StringSearchInterpolator();
        interpolator.addValueSource( new ObjectBasedValueSource( build ) );

        try
        {
            return interpolator.interpolate(
                            isBinaryBuild( build ) ? config.getBinayNamingFormat() : config.getNamingFormat() );
        }
        catch ( InterpolationException e )
        {
            throw new RuntimeException( "Cannot resolve expressions in Koji configuration.", e );
        }
    }

    public KojiRepositoryCreator createRepoCreator()
    {
        return createKojiRepoOperator( KojiRepositoryCreator.class, KOJI_REPO_CREATOR_SCRIPT, true );
    }

    private <T> T createKojiRepoOperator( Class<T> type, String scriptName, boolean mandatory )
    {
        T ret = null;
        try
        {
            ret = scriptEngine.parseStandardScriptInstance( ScriptEngine.StandardScriptType.store_creators, scriptName,
                                                            type );
        }
        catch ( IndyGroovyException e )
        {
            if ( mandatory )
            {
                Logger logger = LoggerFactory.getLogger( getClass() );
                logger.error( String.format( "Cannot create Koji operator instance: %s. Disabling Koji support.",
                                             e.getMessage() ), e );
                config.setEnabled( false );
            }
        }
        return ret;
    }

    public boolean isVersionSignatureAllowedWithPath( String path )
    {

        final ArtifactPathInfo pathInfo = ArtifactPathInfo.parse( path );

        // skip those files without standard GAV format path
        if ( pathInfo == null )
        {
            return true;
        }

        ProjectVersionRef versionRef = pathInfo.getProjectId();
        return isVersionSignatureAllowedWithVersion( versionRef.getVersionStringRaw() );
    }

    public boolean isVersionSignatureAllowedWithVersion( String version )
    {
        final String versionFilter = config.getVersionFilter();

        if ( versionFilter == null )
        {
            return true;
        }

        if ( Pattern.compile( versionFilter ).matcher( version ).matches())
        {
            return true;
        }
        return false;
    }

}
