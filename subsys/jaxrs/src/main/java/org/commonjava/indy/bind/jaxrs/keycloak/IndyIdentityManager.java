/**
 * Copyright (C) 2011-2023 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.bind.jaxrs.keycloak;

import io.undertow.security.idm.Account;
import io.undertow.security.idm.Credential;
import io.undertow.security.idm.IdentityManager;
import org.commonjava.indy.bind.jaxrs.util.JwtTokenUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.security.Principal;
import java.util.*;

@ApplicationScoped
public class IndyIdentityManager implements IdentityManager
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private JwtTokenUtils tokenUtils;

    @Inject
    private AuthConfig authConfig;

    public Account verify( String id, String token )
    {
        if ( tokenUtils.validate(id, token) )
        {
            LocalAccount account = new LocalAccount();
            logger.info("Authenticated as {}, roles [{}]", account.name, account.roles );
            return account;
        }
        return null;
    }

    @Override
    public Account verify( Account account ) {
        return account;
    }

    @Override
    public Account verify( String id, Credential credential ) {
        return null;
    }

    @Override
    public Account verify(Credential credential) {
        return null;
    }

    private class LocalAccount implements Account {

        String name;
        Set<String> roles;

        public LocalAccount()
        {
            name = UUID.randomUUID().toString();
            if ( roles == null )
            {
                roles = new HashSet<>();
                String roleStr = authConfig.getRoles();
                if ( !roleStr.isBlank() )
                {
                    for ( String role : roleStr.split(",") )
                    {
                        roles.add( role );
                    }
                }
            }
        }

        private final Principal principal = new Principal() {
            @Override
            public String getName() {
                return name;
            }
        };

        @Override
        public Principal getPrincipal() {
            return principal;
        }

        @Override
        public Set<String> getRoles() {
            return roles;
        }

    }

}
