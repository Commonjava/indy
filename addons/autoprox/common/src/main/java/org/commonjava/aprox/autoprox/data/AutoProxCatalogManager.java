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
package org.commonjava.aprox.autoprox.data;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.aprox.audit.ChangeSummary;
import org.commonjava.aprox.autoprox.conf.AutoProxConfig;
import org.commonjava.aprox.autoprox.rest.dto.CatalogDTO;
import org.commonjava.aprox.autoprox.rest.dto.RuleDTO;
import org.commonjava.aprox.autoprox.util.ScriptRuleParser;
import org.commonjava.aprox.model.core.Group;
import org.commonjava.aprox.model.core.HostedRepository;
import org.commonjava.aprox.model.core.RemoteRepository;
import org.commonjava.aprox.subsys.datafile.DataFile;
import org.commonjava.aprox.subsys.datafile.DataFileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class AutoProxCatalogManager
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private DataFileManager ffManager;

    @Inject
    private AutoProxConfig apConfig;

    @Inject
    private ScriptRuleParser ruleParser;

    private List<RuleMapping> ruleMappings;

    private boolean enabled;

    protected AutoProxCatalogManager()
    {
    }

    public AutoProxCatalogManager( final DataFileManager ffManager, final AutoProxConfig apConfig,
                                   final ScriptRuleParser ruleParser )
        throws AutoProxRuleException
    {
        this.ffManager = ffManager;
        this.apConfig = apConfig;
        this.ruleParser = ruleParser;
        parseRules();
    }

    @PostConstruct
    public void cdiInit()
    {
        try
        {
            parseRules();
        }
        catch ( final AutoProxRuleException e )
        {
            logger.error( "Failed to parse autoprox rule: " + e.getMessage(), e );
        }
    }

    public synchronized void parseRules()
        throws AutoProxRuleException
    {
        if ( !apConfig.isEnabled() )
        {
            this.enabled = false;
            this.ruleMappings = Collections.emptyList();

            logger.info( "Autoprox is disabled." );
            return;
        }


        final List<RuleMapping> ruleMappings = new ArrayList<RuleMapping>();

        final DataFile dataDir = ffManager.getDataFile( apConfig.getBasedir() );
        logger.info( "Scanning {} for autoprox rules...", dataDir );
        if ( dataDir.exists() )
        {
            final DataFile[] scripts = dataDir.listFiles( new FileFilter()
            {
                @Override
                public boolean accept( final File pathname )
                {
                    logger.info( "Checking for autoprox script in: {}", pathname );
                    return pathname.getName()
                                   .endsWith( ".groovy" );
                }
            } );

            for ( final DataFile script : scripts )
            {
                logger.info( "Reading autoprox rule from: {}", script );
                final RuleMapping rule = ruleParser.parseRule( script );
                if ( rule != null )
                {
                    ruleMappings.add( rule );
                }
            }
        }

        this.ruleMappings = ruleMappings;
        this.enabled = true;
    }

    public CatalogDTO toDTO()
    {
        final List<RuleDTO> rules = new ArrayList<>();
        for ( final RuleMapping mapping : new ArrayList<>( ruleMappings ) )
        {
            rules.add( mapping.toDTO() );
        }

        return new CatalogDTO( enabled, rules );
    }

    public List<RuleMapping> getRuleMappings()
    {
        return ruleMappings;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled( final boolean enabled )
    {
        this.enabled = enabled;
    }

    public RuleMapping getRuleMappingMatching( final String name )
    {
        //        logger.info( "Called via:\n  {}", join( Thread.currentThread()
        //                                                      .getStackTrace(), "\n  " ) );
        for ( final RuleMapping mapping : getRuleMappings() )
        {
            logger.info( "Checking rule: '{}' for applicability to name: '{}'", mapping.getScriptName(), name );
            if ( mapping.matchesName( name ) )
            {
                return mapping;
            }
        }

        logger.info( "No AutoProx rule found for: '{}'", name );

        return null;
    }

    public AutoProxRule getRuleMatching( final String name )
    {
        final RuleMapping mapping = getRuleMappingMatching( name );
        return mapping == null ? null : mapping.getRule();
    }

    public RemoteRepository createRemoteRepository( final String name )
        throws AutoProxRuleException
    {
        final AutoProxRule rule = getRuleMatching( name );
        try
        {
            return rule == null ? null : rule.createRemoteRepository( name );
        }
        catch ( final MalformedURLException e )
        {
            throw new AutoProxRuleException( "Invalid URL genenerated for: '%s'. Reason: %s", e, name, e.getMessage() );
        }
        catch ( final Exception e )
        {
            throw new AutoProxRuleException( "Failed to create remote repository for: %s. Reason: %s", e, name,
                                             e.getMessage() );
        }
    }

    public HostedRepository createHostedRepository( final String name )
        throws AutoProxRuleException
    {
        final AutoProxRule rule = getRuleMatching( name );
        try
        {
            return rule == null ? null : rule.createHostedRepository( name );
        }
        catch ( final Exception e )
        {
            throw new AutoProxRuleException( "Failed to create remote repository for: %s. Reason: %s", e, name,
                                             e.getMessage() );
        }
    }

    public Group createGroup( final String name )
        throws AutoProxRuleException
    {
        final AutoProxRule rule = getRuleMatching( name );
        try
        {
            return rule == null ? null : rule.createGroup( name );
        }
        catch ( final Exception e )
        {
            throw new AutoProxRuleException( "Failed to create remote repository for: %s. Reason: %s", e, name,
                                             e.getMessage() );
        }
    }

    public String getRemoteValidationUrl( final String name )
        throws AutoProxRuleException
    {
        final AutoProxRule rule = getRuleMatching( name );
        try
        {
            return rule == null ? null : rule.getRemoteValidationPath();
        }
        catch ( final Exception e )
        {
            throw new AutoProxRuleException( "Failed to create remote repository for: %s. Reason: %s", e, name,
                                             e.getMessage() );
        }
    }

    public RemoteRepository createValidationRemote( final String name )
        throws AutoProxRuleException
    {
        final AutoProxRule rule = getRuleMatching( name );
        try
        {
            return rule == null || !rule.isValidationEnabled() ? null : rule.createValidationRemote( name );
        }
        catch ( final MalformedURLException e )
        {
            throw new AutoProxRuleException( "Invalid URL genenerated for: '%s'. Reason: %s", e, name, e.getMessage() );
        }
        catch ( final Exception e )
        {
            throw new AutoProxRuleException( "Failed to create remote repository for: %s. Reason: %s", e, name,
                                             e.getMessage() );
        }
    }

    public boolean isValidationEnabled( final String name )
    {
        final AutoProxRule rule = getRuleMatching( name );
        return rule != null && rule.isValidationEnabled();
    }

    public synchronized RuleMapping removeRuleNamed( final String name, final ChangeSummary changelog )
        throws AutoProxRuleException
    {
        RuleMapping mapping = null;
        for ( final Iterator<RuleMapping> mappingIt = ruleMappings.iterator(); mappingIt.hasNext(); )
        {
            final RuleMapping m = mappingIt.next();
            if ( m.getScriptName()
                  .equals( name ) )
            {
                mappingIt.remove();
                mapping = m;
                break;
            }
        }

        if ( mapping == null )
        {
            return null;
        }

        final DataFile dataDir = ffManager.getDataFile( apConfig.getBasedir() );
        if ( !dataDir.exists() )
        {
            dataDir.mkdirs();
        }

        final DataFile scriptFile = dataDir.getChild( name + ".groovy" );
        if ( scriptFile.exists() )
        {
            try
            {
                scriptFile.delete( changelog );

                return mapping;
            }
            catch ( final IOException e )
            {
                throw new AutoProxRuleException( "Failed to delete rule: %s to: %s. Reason: %s", e, name, scriptFile,
                                                 e.getMessage() );
            }
        }

        return null;
    }

    public synchronized RuleMapping storeRule( final String name, final String spec, final ChangeSummary changelog )
        throws AutoProxRuleException
    {
        final RuleMapping mapping = ruleParser.parseRule( spec, name );
        final int idx = ruleMappings.indexOf( mapping );
        if ( idx > -1 )
        {
            final RuleMapping existing = ruleMappings.get( idx );
            if ( mapping.getSpecification()
                        .equals( existing.getSpecification() ) )
            {
                return existing;
            }

            ruleMappings.set( idx, mapping );
        }
        else
        {
            ruleMappings.add( mapping );
            Collections.sort( ruleMappings );
        }

        final DataFile dataDir = ffManager.getDataFile( apConfig.getBasedir() );
        if ( !dataDir.exists() )
        {
            dataDir.mkdirs();
        }

        final DataFile scriptFile = dataDir.getChild( name + ".groovy" );
        try
        {
            scriptFile.writeString( spec, changelog );
        }
        catch ( final IOException e )
        {
            throw new AutoProxRuleException( "Failed to write rule: %s to: %s. Reason: %s", e, name, scriptFile,
                                             e.getMessage() );
        }

        return mapping;
    }

    public synchronized RuleMapping getRuleNamed( final String name )
    {
        for ( final RuleMapping mapping : ruleMappings )
        {
            if ( mapping.getScriptName()
                        .equals( name ) )
            {
                return mapping;
            }
        }

        return null;
    }

}
