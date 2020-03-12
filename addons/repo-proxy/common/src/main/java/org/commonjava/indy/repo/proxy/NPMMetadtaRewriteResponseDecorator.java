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
package org.commonjava.indy.repo.proxy;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.commonjava.indy.pkg.PackageTypeConstants;
import org.commonjava.indy.repo.proxy.conf.RepoProxyConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.commonjava.indy.repo.proxy.RepoProxyUtils.extractPath;
import static org.commonjava.indy.repo.proxy.RepoProxyUtils.trace;

/**
 * This response decorator will do following
 * <ul>
 *     <li>Intercept NPM metadata path, like /jquery, /@angular/core or /jquery/package.json</li>
 *     <li>Replacing url which is using the proxy-to repo path with the request repo path in the metadata content, like "tarball:"</li>
 *     <li>And this decorator can be disabled by config "npm.meta.rewrite.enabled", default is enabled</li>
 * </ul>
 */
@ApplicationScoped
public class NPMMetadtaRewriteResponseDecorator
        implements RepoProxyResponseDecorator
{
    private static final Logger logger = LoggerFactory.getLogger( NPMMetadtaRewriteResponseDecorator.class );

    @Inject
    private RepoProxyConfig config;

    private Map<String, String> npmRules;

    private static final String METADATA_NAME = "package.json";

    @PostConstruct
    public void init()
    {
        if ( config.isEnabled() && ( npmRules == null || npmRules.isEmpty() ) )
        {
            npmRules = new HashMap<>();
            for ( Map.Entry<String, String> rule : config.getProxyRules().entrySet() )
            {
                String packageType = rule.getKey().split( "/" )[0];
                if ( packageType != null && packageType.trim().equalsIgnoreCase( PackageTypeConstants.PKG_TYPE_NPM ) )
                {
                    npmRules.put( rule.getKey(), rule.getValue() );
                }
            }
            if ( npmRules.isEmpty() )
            {
                logger.info(
                        "No npm rules defined in Repository Proxy, will not do any response decorating for NPM metadata rewriting further." );
            }
        }
    }

    @Override
    public HttpServletResponse decoratingResponse( HttpServletRequest request, HttpServletResponse response )
            throws IOException
    {
        if ( !config.isEnabled() || !config.isNpmMetaRewriteEnabled() )
        {
            trace( logger,
                   "Addon not enabled or npm meta rewrite not allowed, will not decorate the response for NPM metadata rewriting." );
            return response;
        }

        if ( npmRules.isEmpty() )
        {
            trace( logger, "No npm proxy rules defined, don't decorate for NPM metadata rewriting" );
            return response;
        }

        final String pathInfo = request.getPathInfo();

        for ( Map.Entry<String, String> npmRule : npmRules.entrySet() )
        {
            final String originalRepoPath = npmRule.getKey();
            final boolean ruleInPath = pathInfo.contains( originalRepoPath );
            if ( ruleInPath )
            {
                final String path = extractPath( pathInfo, originalRepoPath );
                if ( isNPMMetaPath( path ) )
                {
                    return new NPMMetadataRewriteResponseWrapper( request, response,
                                                                  Collections.singletonMap( originalRepoPath,
                                                                                            npmRule.getValue() ) );
                }
                else
                {
                    trace( logger, "NPM meta rewrite: {} is not a metadata path", path );
                }
            }
        }
        return response;
    }

    private boolean isNPMMetaPath( final String path )
    {
        if ( StringUtils.isBlank( path ) )
        {
            return false;
        }
        // This is considering the single path for npm standard like "/jquery"
        final boolean isSinglePath = path.split( "/" ).length < 2;
        // This is considering the scoped path for npm standard like "/@type/jquery"
        final boolean isScopedPath = path.startsWith( "@" ) && path.split( "/" ).length < 3;
        // This is considering the package.json file itself
        final boolean isPackageJson = path.trim().endsWith( "/" + METADATA_NAME );

        trace( logger, "path: {}, isSinglePath: {}, isScopedPath: {}, isPackageJson: {}", path, isSinglePath,
               isScopedPath, isPackageJson );
        return isSinglePath || isScopedPath || isPackageJson;
    }

    private static class NPMMetadataRewriteResponseWrapper
            extends HttpServletResponseWrapper
    {
        private HttpServletRequest request;

        private NPMMetadataRewriteOutputStream out;

        /**
         * Constructs a response adaptor wrapping the given response.
         * @throws IllegalArgumentException if the response is null
         * @param response
         * @throws IOException
         */
        public NPMMetadataRewriteResponseWrapper( final HttpServletRequest request, final HttpServletResponse response,
                                                  final Map<String, String> reposReplacing )
                throws IOException
        {
            super( response );
            this.request = request;
            this.out = new NPMMetadataRewriteOutputStream( response.getOutputStream(), reposReplacing );
        }

        @Override
        public PrintWriter getWriter()
                throws IOException
        {
            return new PrintWriter( this.getResponse().getWriter() );
        }

        @Override
        public ServletOutputStream getOutputStream()
                throws IOException
        {
            final ServletOutputStream originalStream = this.getResponse().getOutputStream();
            return this.out;
        }

    }

    private static class NPMMetadataRewriteOutputStream
            extends ServletOutputStream
    {
        private StringBuffer buffer = new StringBuffer();

        private ServletOutputStream originalStream;

        private Map<String, String> reposReplacing;

        private NPMMetadataRewriteOutputStream( final ServletOutputStream originalStream,
                                                final Map<String, String> reposReplacing )
        {
            this.originalStream = originalStream;
            this.reposReplacing = reposReplacing;
        }

        @Override
        public void write( int b )
        {
            buffer.append( (char) b );
        }

        @Override
        public void flush()
                throws IOException
        {
            try
            {
                String content = buffer.toString();
                for ( Map.Entry<String, String> repoReplacing : reposReplacing.entrySet() )
                {
                    final String origin = repoReplacing.getKey();
                    final String proxyTo = repoReplacing.getValue();
                    logger.trace( "Repository Proxy: NPM metadata rewriting: Replacing {} to {}", proxyTo, origin );
                    content = content.replaceAll( proxyTo, origin );
                }
                originalStream.write( content.getBytes() );
                originalStream.flush();
            }
            finally
            {
                buffer = new StringBuffer();
            }
        }

        @Override
        public void close()
                throws IOException
        {
            flush();
            IOUtils.closeQuietly( originalStream );
        }
    }
}
