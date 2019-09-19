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
package org.commonjava.indy.subsys.keycloak;

import java.io.File;
import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.commonjava.indy.subsys.keycloak.conf.KeycloakConfig;
import org.commonjava.indy.subsys.keycloak.conf.KeycloakSecurityBindings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

@ApplicationScoped
public class SecurityConstraintProvider
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private KeycloakConfig config;

    @Inject
    private ObjectMapper mapper;

    private KeycloakSecurityBindings constraintSet;

    protected SecurityConstraintProvider()
    {
    }

    public SecurityConstraintProvider( final ObjectMapper mapper, final KeycloakConfig config )
    {
        this.mapper = mapper;
        this.config = config;
        init();
    }

    @PostConstruct
    public void init()
    {
        if ( config.isEnabled() )
        {
            final File constraintFile = new File( config.getSecurityBindingsJson() );
            if ( !constraintFile.exists() )
            {
                throw new IllegalStateException( "Cannot load keycloak security constraints: " + constraintFile );
            }

            try
            {
                logger.debug( "Loading security bindings: {}", constraintFile );
                constraintSet = mapper.readValue( constraintFile, KeycloakSecurityBindings.class );
            }
            catch ( final IOException e )
            {
                throw new IllegalStateException( "Cannot load keycloak security constraints: " + constraintFile, e );
            }
        }
    }

    @Produces
    public KeycloakSecurityBindings getConstraints()
    {
        return constraintSet;
    }

    //    public static KeycloakSecurityBindings createConstraintsConfig()
    //    {
    //        final KeycloakSecurityBindings config = new KeycloakSecurityBindings();
    //        // 1.constraint
    //        final KeycloakSecurityConstraint securityConstraint1 =
    //            new KeycloakSecurityConstraint( "admin", "/api/info/*", new String[] { "POST", "GET" } );
    //        // 2.constraint
    //        final KeycloakSecurityConstraint securityConstraint2 =
    //            new KeycloakSecurityConstraint( "user", "/api/all/*", new String[] { "POST", "GET", "PUT", "DELETE" } );
    //        // 3.constraint
    //        final KeycloakSecurityConstraint securityConstraint3 =
    //            new KeycloakSecurityConstraint( "all", "/api/test/*", new String[] { "POST", "GET", "PUT", "TRACE" } );
    //
    //        final List<KeycloakSecurityConstraint> constraints = new ArrayList<KeycloakSecurityConstraint>();
    //        constraints.add( securityConstraint1 );
    //        constraints.add( securityConstraint2 );
    //        constraints.add( securityConstraint3 );
    //
    //        config.setConstraints( constraints );
    //
    //        return config;
    //    }

}
