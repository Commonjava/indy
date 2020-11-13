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
package org.commonjava.indy.promote.client;

import org.apache.http.HttpStatus;
import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.client.core.IndyClientModule;
import org.commonjava.indy.client.core.util.UrlUtils;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.promote.model.GroupPromoteRequest;
import org.commonjava.indy.promote.model.GroupPromoteResult;
import org.commonjava.indy.promote.model.PathsPromoteRequest;
import org.commonjava.indy.promote.model.PathsPromoteResult;
import org.commonjava.indy.promote.model.ValidationRuleDTO;
import org.commonjava.indy.promote.model.ValidationRuleSet;

import javax.xml.ws.Response;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.commonjava.indy.client.core.util.UrlUtils.buildUrl;

public class IndyPromoteAdminClientModule
        extends IndyClientModule
{

    public static final String PROMOTE_ADMIN_BASEPATH = "admin/promotion";

    public static final String VALIDATION_BASEPATH = PROMOTE_ADMIN_BASEPATH + "/validation";

    public static final String VALIDATION_RELOAD_PATH = VALIDATION_BASEPATH + "/reload";

    public static final String VALIDATION_RELOAD_RULES_PATH = VALIDATION_RELOAD_PATH + "/rules";

    public static final String VALIDATION_RELOAD_RULESETS_PATH = VALIDATION_RELOAD_PATH + "/rulesets";

    public static final String VALIDATION_RELOAD_ALL_PATH = VALIDATION_RELOAD_PATH + "/all";

    public static final String VALIDATION_RULES_BASEPATH = VALIDATION_BASEPATH + "/rules";

    public static final String VALIDATION_RULES_GET_ALL_PATH = VALIDATION_RULES_BASEPATH + "/all";

    public static final String VALIDATION_RULES_GET_BY_NAME_PATH = VALIDATION_RULES_BASEPATH + "/named";

    public static final String VALIDATION_RULESET_BASEPATH = VALIDATION_BASEPATH + "/rulesets";

    public static final String VALIDATION_RULESET_GET_ALL_PATH = VALIDATION_RULESET_BASEPATH + "/all";

    public static final String VALIDATION_RULESET_GET_BY_STOREKEY_PATH = VALIDATION_RULESET_BASEPATH + "/storekey";

    public static final String VALIDATION_RULESET_GET_BY_NAME_PATH = VALIDATION_RULESET_BASEPATH + "/named";

    public boolean reloadRules()
            throws IndyClientException
    {
        return http.put( VALIDATION_RELOAD_RULES_PATH, "", HttpStatus.SC_OK );

    }

    public boolean reloadRuleSets()
            throws IndyClientException
    {
        return http.put( VALIDATION_RELOAD_RULESETS_PATH, "", HttpStatus.SC_OK );

    }

    public boolean reloadRuleBundles()
            throws IndyClientException
    {
        return http.put( VALIDATION_RELOAD_ALL_PATH, "", HttpStatus.SC_OK );

    }

    public List<String> getAllRules()
            throws IndyClientException
    {
        List<?> rules = http.get( VALIDATION_RULES_GET_ALL_PATH, List.class );
        return rules.stream().map( Object::toString ).collect( Collectors.toList() );
    }

    public ValidationRuleDTO getRuleByName( final String name )
            throws IndyClientException
    {
        return http.get( buildUrl( VALIDATION_RULES_GET_BY_NAME_PATH, name ), ValidationRuleDTO.class );
    }

    public List<String> getAllRuleSets()
            throws IndyClientException
    {
        List<?> rules = http.get( VALIDATION_RULESET_GET_ALL_PATH, List.class );
        return rules.stream().map( Object::toString ).collect( Collectors.toList() );
    }

    public ValidationRuleSet getRuleSetByName( final String name )
            throws IndyClientException
    {
        return http.get( buildUrl( VALIDATION_RULESET_GET_BY_NAME_PATH, name ), ValidationRuleSet.class );
    }

    public ValidationRuleSet getRuleSetByStoreKey( final StoreKey key )
            throws IndyClientException
    {
        return http.get( buildUrl( VALIDATION_RULESET_GET_BY_STOREKEY_PATH, key.toString() ), ValidationRuleSet.class );
    }
}
