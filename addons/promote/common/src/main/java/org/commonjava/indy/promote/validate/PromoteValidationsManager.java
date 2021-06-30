/**
 * Copyright (C) 2011-2020 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.promote.validate;

import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.promote.conf.PromoteConfig;
import org.commonjava.indy.promote.conf.PromoteDataFileManager;
import org.commonjava.indy.promote.model.ValidationCatalogDTO;
import org.commonjava.indy.promote.model.ValidationRuleDTO;
import org.commonjava.indy.promote.model.ValidationRuleSet;
import org.commonjava.indy.promote.validate.model.ValidationRule;
import org.commonjava.indy.promote.validate.model.ValidationRuleMapping;
import org.commonjava.indy.subsys.datafile.DataFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@ApplicationScoped
public class PromoteValidationsManager
{

    public static final String RULES_DIR = "rules";

    private static final String RULES_SETS_DIR = "rule-sets";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private PromoteDataFileManager dataFileManager;

    @Inject
    private PromoteConfig config;

    @Inject
    private ValidationRuleParser ruleParser;

    private Map<String, ValidationRuleMapping> ruleMappings;

    private boolean enabled;

    private Map<String, ValidationRuleSet> ruleSets;

    protected PromoteValidationsManager()
    {
    }

    public PromoteValidationsManager( final PromoteDataFileManager dataFileManager, final PromoteConfig config,
                                      final ValidationRuleParser ruleParser )
        throws PromotionValidationException
    {
        this.dataFileManager = dataFileManager;
        this.config = config;
        this.ruleParser = ruleParser;
        parseRuleBundles();
    }

    @PostConstruct
    public void cdiInit()
    {
        try
        {
            parseRuleBundles();
        }
        catch ( final PromotionValidationException e )
        {
            logger.error( "Failed to parse validation rule: " + e.getMessage(), e );
        }
    }

    public synchronized void parseRuleBundles()
            throws PromotionValidationException
    {
        if ( !config.isEnabled() )
        {
            this.enabled = false;
            this.ruleMappings = Collections.emptyMap();

            logger.info( "Promotion is disabled." );
            return;
        }
        parseRules();
        parseRuleSets();

        this.enabled = true;
    }

    public synchronized void parseRules()
            throws PromotionValidationException
    {
        final Map<String, ValidationRuleMapping> ruleMappings = new HashMap<>();

        DataFile dataDir = dataFileManager.getDataFile( RULES_DIR );
        logger.info( "Scanning {} for promotion validation rules...", dataDir );
        if ( dataDir.exists() )
        {
            final DataFile[] scripts = dataDir.listFiles( ( pathname ) -> {
                logger.debug( "Checking for promote validation rule script in: {}", pathname );
                return pathname.getName().endsWith( ".groovy" );
            } );

            if ( scripts.length > 0 )
            {
                for ( final DataFile script : scripts )
                {
                    logger.debug( "Reading promotion validation rule from: {}", script );
                    final ValidationRuleMapping rule = ruleParser.parseRule( script );
                    if ( rule != null )
                    {
                        ruleMappings.put( rule.getName(), rule );
                    }
                }
            }
            else
            {
                logger.warn( "No rule script file was defined for promotion: no rule script found in {} directory",
                             RULES_DIR );
            }
        }
        else
        {
            logger.warn( "No rule script file was defined for promotion: {} directory not exists", RULES_DIR );
        }

        checkRuleMappings( ruleMappings );

        this.ruleMappings = ruleMappings;
    }

    private void checkRuleMappings( final Map<String, ValidationRuleMapping> ruleMappings )
    {
        checkMappings( this.ruleMappings, ruleMappings, false, ( n, r, c ) -> {
            if ( !n.isEmpty() )
            {
                logger.info( "New added rules: {}", n );
            }
            if ( !r.isEmpty() )
            {
                logger.info( "Removed rules: {}", r );
            }
            //TODO: need to think about how to track changed rules for logging
        } );
    }

    private <T> void checkMappings( final Map<String, T> original, Map<String, T> loaded, final boolean checkChanges,
                                    final LoggingConsumer loggingConsumer )
    {
        if ( original != null && !original.isEmpty() )
        {
            Set<String> existedElems = original.keySet();
            Set<String> loadedElems = loaded.keySet();
            Set<String> newElems = new HashSet<>( loadedElems.size() );
            Set<String> removedElems = new HashSet<>( existedElems.size() );
            Set<String> changedElems = new HashSet<>( existedElems.size() );
            for ( String key : loaded.keySet() )
            {
                if ( !original.containsKey( key ) )
                {
                    newElems.add( key );
                }
                else if ( checkChanges && original.get( key ).equals( loaded.get( key ) ) )
                {
                    // Here directly used equals method to check if the value changed. It works for RuleSet
                    // but not works for Rule, so need to think about how to check Rule's changing
                    changedElems.add( key );
                }
            }
            for ( String key : original.keySet() )
            {
                if ( !loaded.containsKey( key ) )
                {
                    removedElems.add( key );
                }
            }
            loggingConsumer.logging( newElems, removedElems, changedElems );
        }
    }

    @FunctionalInterface
    private interface LoggingConsumer
    {
        void logging( Set<String> added, Set<String> removed, Set<String> changed );
    }

    public synchronized void parseRuleSets()
            throws PromotionValidationException
    {
        Map<String, ValidationRuleSet> ruleSets = new HashMap<>();

        DataFile dataDir = dataFileManager.getDataFile( RULES_SETS_DIR );
        logger.info( "Scanning {} for promotion validation rule-set mappings...", dataDir );
        if ( dataDir.exists() )
        {
            final DataFile[] scripts = dataDir.listFiles( ( pathname ) -> {
                logger.debug( "Checking for promotion rule-set in: {}", pathname );
                return pathname.getName().endsWith( ".json" );
            } );

            if ( scripts.length > 0 )
            {

                for ( final DataFile script : scripts )
                {
                    logger.debug( "Reading promotion validation rule-set from: {}", script );
                    final ValidationRuleSet set = ruleParser.parseRuleSet( script );
                    if ( set != null )
                    {
                        ruleSets.put( script.getName(), set );
                    }
                }
            }
            else
            {
                logger.warn( "No rule-set json file was defined for promotion: no json file found in {} directory",
                             RULES_SETS_DIR );
            }
        }
        else
        {
            logger.warn( "No rule-set json file was defined for promotion: {} directory not exists", RULES_SETS_DIR );
        }

        checkRuleSetMappings( ruleSets );

        this.ruleSets = ruleSets;
    }

    private void checkRuleSetMappings( Map<String, ValidationRuleSet> ruleSetMappings )
    {
        checkMappings( this.ruleSets, ruleSetMappings, true, ( n, r, c ) -> {
            if ( !n.isEmpty() )
            {
                logger.info( "New added rule-sets: {}", n );
            }
            if ( !r.isEmpty() )
            {
                logger.info( "Removed rule-sets: {}", r );
            }
            if ( !c.isEmpty() )
            {
                logger.info( "Changed rule-sets: {}", r );
            }
        } );
    }

    public ValidationCatalogDTO toDTO()
    {
        final Map<String, ValidationRuleDTO> rules = new HashMap<>();
        for ( final ValidationRuleMapping mapping : ruleMappings.values() )
        {
            rules.put( mapping.getName(), mapping.toDTO() );
        }

        return new ValidationCatalogDTO( enabled, rules, ruleSets );
    }

    public Set<ValidationRuleMapping> getRuleMappings()
    {
        return new HashSet<>( ruleMappings.values() );
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled( final boolean enabled )
    {
        this.enabled = enabled;
    }

    public ValidationRuleSet getRuleSetMatching( StoreKey storeKey )
    {
        if ( ruleSets == null )
        {
            logger.debug( "No rule sets to match against. No validations will be executed for: {}", storeKey );
            return null;
        }

        // Add deprecated form of StoreKey to the check to handle older rules.
        List<String> keyStrings = Arrays.asList( storeKey.toString(),
                                                 String.format( "%s:%s", storeKey.getType().singularEndpointName(),
                                                                storeKey.getName() ) );

        for ( Map.Entry<String, ValidationRuleSet> entry : ruleSets.entrySet() )
        {
            for ( String keyStr : keyStrings )
            {
                logger.debug( "Checking for rule-set match. Key='{}', rule-set: '{}'", keyStr, entry.getKey() );
                if ( entry.getValue().matchesKey( keyStr ) )
                {
                    logger.debug( "Rule set '{}' matches key: '{}'", entry.getKey(), keyStr );
                    return entry.getValue();
                }
            }
        }

        return null;
    }

    public ValidationRule getRuleNamed( final String name )
    {
        final ValidationRuleMapping mapping = getRuleMappingNamed( name );
        return mapping == null ? null : mapping.getRule();
    }

    public Optional<ValidationRuleDTO> getNamedRuleAsDTO( final String name )
    {
        final Map<String, ValidationRuleDTO> rules = toDTO().getRules();
        ValidationRuleDTO dto = rules.get( name );
        if ( dto == null )
        {
            dto = rules.get( name + ".groovy" );
        }
        return dto == null ? Optional.empty() : Optional.of( dto );
    }

    public Optional<ValidationRuleSet> getNamedRuleSet( final String name )
    {
        final Map<String, ValidationRuleSet> ruleSets = toDTO().getRuleSets();
        ValidationRuleSet ruleSet = ruleSets.get( name );
        if ( ruleSet == null )
        {
            ruleSet = ruleSets.get( name + ".json" );
        }
        return ruleSet == null ? Optional.empty() : Optional.of( ruleSet );
    }

    public synchronized ValidationRuleMapping removeRuleNamed( final String name, final ChangeSummary changelog )
        throws PromotionValidationException
    {
        ValidationRuleMapping mapping = ruleMappings.remove( name );
        if ( mapping == null )
        {
            return null;
        }

        final DataFile dataDir = dataFileManager.getDataFile( RULES_DIR );
        if ( !dataDir.exists() )
        {
            // this would be a very strange error...implying addition of a rule without writing it to disk.
            return null;
        }

        final DataFile scriptFile = dataDir.getChild( name );
        if ( scriptFile.exists() )
        {
            try
            {
                scriptFile.delete( changelog );

                return mapping;
            }
            catch ( final IOException e )
            {
                throw new PromotionValidationException( "Failed to delete rule: %s to: %s. Reason: %s", e, name, scriptFile,
                                                 e.getMessage() );
            }
        }

        return null;
    }

    public synchronized ValidationRuleMapping storeRule( final String name, final String spec, final ChangeSummary changelog )
        throws PromotionValidationException
    {
        final ValidationRuleMapping mapping = ruleParser.parseRule( spec, name );
        ruleMappings.put( mapping.getName(), mapping );

        final DataFile dataDir = dataFileManager.getDataFile( RULES_DIR );
        if ( !dataDir.exists() )
        {
            dataDir.mkdirs();
        }

        final DataFile scriptFile = dataDir.getChild( name );
        try
        {
            scriptFile.writeString( spec, changelog );
        }
        catch ( final IOException e )
        {
            throw new PromotionValidationException( "Failed to write rule: %s to: %s. Reason: %s", e, name, scriptFile,
                                             e.getMessage() );
        }

        return mapping;
    }

    public synchronized ValidationRuleMapping getRuleMappingNamed( final String name )
    {
        return ruleMappings == null ? null : ruleMappings.get( name );
    }

}
