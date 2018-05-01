/**
 * Copyright (C) 2011-2018 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.autoprox.data;

import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.autoprox.conf.AutoProxConfig;
import org.commonjava.indy.autoprox.rest.dto.CatalogDTO;
import org.commonjava.indy.autoprox.rest.dto.RuleDTO;
import org.commonjava.indy.autoprox.util.ScriptRuleParser;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.subsys.datafile.DataFile;
import org.commonjava.indy.subsys.datafile.DataFileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@ApplicationScoped
public class AutoProxCatalogManager
{

    public static final String AUTOPROX_ORIGIN = "autoprox";

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
        if ( !checkEnabled() )
        {
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
                    logger.debug( "Checking for autoprox script in: {}", pathname );
                    return pathname.getName().endsWith( ".groovy" );
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

    private boolean checkEnabled()
    {
        if ( !apConfig.isEnabled() )
        {
            this.enabled = false;
            this.ruleMappings = new ArrayList<>();

            logger.debug( "Autoprox is disabled." );
            return false;
        }

        return true;
    }

    public CatalogDTO toDTO()
    {
        if ( !checkEnabled() )
        {
            return null;
        }

        final List<RuleDTO> rules = new ArrayList<>();
        for ( final RuleMapping mapping : new ArrayList<>( ruleMappings ) )
        {
            rules.add( mapping.toDTO() );
        }

        return new CatalogDTO( enabled, rules );
    }

    public List<RuleMapping> getRuleMappings()
    {
        if ( !checkEnabled() )
        {
            return null;
        }

        return ruleMappings;
    }

    public boolean isEnabled()
    {
        return checkEnabled() && enabled;
    }

    public void setEnabled( final boolean enabled )
    {
        this.enabled = enabled;
    }

    public RuleMapping getRuleMappingMatching( final StoreKey key )
    {
        if ( !checkEnabled() )
        {
            return null;
        }

        //        logger.info( "Called via:\n  {}", join( Thread.currentThread()
        //                                                      .getStackTrace(), "\n  " ) );
        for ( final RuleMapping mapping : getRuleMappings() )
        {
            logger.debug( "Checking rule: '{}' for applicability to name: '{}'", mapping.getScriptName(), key );
            if ( mapping.matches( key ) )
            {
                logger.info( "Using rule: '{}'", mapping.getScriptName() );
                return mapping;
            }
        }

        logger.info( "No AutoProx rule found for: '{}'", key );

        return null;
    }

    public AutoProxRule getRuleMatching( final StoreKey key )
    {
        if ( !checkEnabled() )
        {
            return null;
        }

        final RuleMapping mapping = getRuleMappingMatching( key );
        return mapping == null ? null : mapping.getRule();
    }

    public RemoteRepository createRemoteRepository( final StoreKey key )
            throws AutoProxRuleException
    {
        if ( !checkEnabled() )
        {
            throw new AutoProxRuleException( "AutoProx is disabled" );
        }

        final AutoProxRule rule = getRuleMatching( key );
        try
        {
            if ( rule != null )
            {
                RemoteRepository repo = rule.createRemoteRepository( key );
                repo.setMetadata( ArtifactStore.METADATA_ORIGIN, AUTOPROX_ORIGIN );
                return repo;
            }

            return null;
        }
        catch ( final MalformedURLException e )
        {
            throw new AutoProxRuleException( "Invalid URL genenerated for: '%s'. Reason: %s", e, key, e.getMessage() );
        }
        catch ( final Exception e )
        {
            throw new AutoProxRuleException( "Failed to create remote repository for: %s. Reason: %s", e, key,
                                             e.getMessage() );
        }
    }

    public HostedRepository createHostedRepository( final StoreKey key )
            throws AutoProxRuleException
    {
        if ( !checkEnabled() )
        {
            throw new AutoProxRuleException( "AutoProx is disabled" );
        }

        final AutoProxRule rule = getRuleMatching( key );
        try
        {
            if ( rule != null )
            {
                HostedRepository repo = rule.createHostedRepository( key );
                repo.setMetadata( ArtifactStore.METADATA_ORIGIN, AUTOPROX_ORIGIN );
                return repo;
            }

            return null;
        }
        catch ( final Exception e )
        {
            throw new AutoProxRuleException( "Failed to create remote repository for: %s. Reason: %s", e, key,
                                             e.getMessage() );
        }
    }

    public Group createGroup( final StoreKey key )
            throws AutoProxRuleException
    {
        if ( !checkEnabled() )
        {
            throw new AutoProxRuleException( "AutoProx is disabled" );
        }

        final AutoProxRule rule = getRuleMatching( key );
        try
        {
            if ( rule != null )
            {
                Group group = rule.createGroup( key );
                group.setMetadata( ArtifactStore.METADATA_ORIGIN, AUTOPROX_ORIGIN );
                return group;
            }

            return null;
        }
        catch ( final Exception e )
        {
            throw new AutoProxRuleException( "Failed to create remote repository for: %s. Reason: %s", e, key,
                                             e.getMessage() );
        }
    }

    public String getRemoteValidationPath( final StoreKey key )
            throws AutoProxRuleException
    {
        if ( !checkEnabled() )
        {
            throw new AutoProxRuleException( "AutoProx is disabled" );
        }

        final AutoProxRule rule = getRuleMatching( key );
        try
        {
            return rule == null ? null : rule.getRemoteValidationPath();
        }
        catch ( final Exception e )
        {
            throw new AutoProxRuleException( "Failed to create remote repository for: %s. Reason: %s", e, key,
                                             e.getMessage() );
        }
    }

    public RemoteRepository createValidationRemote( final StoreKey key )
            throws AutoProxRuleException
    {
        if ( !checkEnabled() )
        {
            throw new AutoProxRuleException( "AutoProx is disabled" );
        }

        final AutoProxRule rule = getRuleMatching( key );
        try
        {
            return rule == null || !rule.isValidationEnabled() ? null : rule.createValidationRemote( key );
        }
        catch ( final MalformedURLException e )
        {
            throw new AutoProxRuleException( "Invalid URL genenerated for: '%s'. Reason: %s", e, key, e.getMessage() );
        }
        catch ( final Exception e )
        {
            throw new AutoProxRuleException( "Failed to create remote repository for: %s. Reason: %s", e, key,
                                             e.getMessage() );
        }
    }

    public boolean isValidationEnabled( final StoreKey key )
    {
        if ( !checkEnabled() )
        {
            return false;
        }

        final AutoProxRule rule = getRuleMatching( key );
        return rule != null && rule.isValidationEnabled();
    }

    public synchronized RuleMapping removeRuleNamed( final String name, final ChangeSummary changelog )
            throws AutoProxRuleException
    {
        if ( !checkEnabled() )
        {
            throw new AutoProxRuleException( "AutoProx is disabled" );
        }

        RuleMapping mapping = null;
        for ( final Iterator<RuleMapping> mappingIt = ruleMappings.iterator(); mappingIt.hasNext(); )
        {
            final RuleMapping m = mappingIt.next();
            if ( m.getScriptName().equals( name ) )
            {
                logger.info( "Found rule {} in rule Mappings, delete it now.", name );
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
                logger.info("Found rule file {} in flat file storage, begin to delete", scriptFile);
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
        if ( !checkEnabled() )
        {
            throw new AutoProxRuleException( "AutoProx is disabled" );
        }

        final RuleMapping mapping = ruleParser.parseRule( spec, name );
        if ( mapping == null )
        {
            throw new AutoProxRuleException( "Cannot construct RuleMapping for: {} with spec:\n\n{}\n\n", name, spec );
        }

        final int idx = ruleMappings.indexOf( mapping );
        if ( idx > -1 )
        {
            final RuleMapping existing = ruleMappings.get( idx );
            if ( mapping.getSpecification().equals( existing.getSpecification() ) )
            {
                return existing;
            }

            logger.info( "Replacing rule: {} at index: {}. Spec was:\n\n{}\n\n", mapping, idx, spec );

            ruleMappings.set( idx, mapping );
        }
        else
        {
            logger.info( "Appending rule: {}. Spec was:\n\n{}\n\n", mapping, spec );

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
        if ( !checkEnabled() )
        {
            return null;
        }

        for ( final RuleMapping mapping : ruleMappings )
        {
            if ( mapping.getScriptName().equals( name ) )
            {
                return mapping;
            }
        }

        return null;
    }

}
