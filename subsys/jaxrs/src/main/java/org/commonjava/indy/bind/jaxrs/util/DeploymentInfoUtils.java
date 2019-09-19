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
package org.commonjava.indy.bind.jaxrs.util;

import io.undertow.security.api.AuthenticationMechanismFactory;
import io.undertow.security.api.NotificationReceiver;
import io.undertow.server.HandlerWrapper;
import io.undertow.server.session.SessionListener;
import io.undertow.servlet.ServletExtension;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.ErrorPage;
import io.undertow.servlet.api.FilterInfo;
import io.undertow.servlet.api.FilterMappingInfo;
import io.undertow.servlet.api.LifecycleInterceptor;
import io.undertow.servlet.api.ListenerInfo;
import io.undertow.servlet.api.LoginConfig;
import io.undertow.servlet.api.MimeMapping;
import io.undertow.servlet.api.SecurityConstraint;
import io.undertow.servlet.api.ServletContainerInitializerInfo;
import io.undertow.servlet.api.ServletInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.commonjava.indy.bind.jaxrs.IndyDeployment;
import org.commonjava.indy.bind.jaxrs.IndyDeploymentProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Application;

public final class DeploymentInfoUtils
{
    private static final Logger logger = LoggerFactory.getLogger( DeploymentInfoUtils.class );

    private DeploymentInfoUtils()
    {
    }

    public static void mergeFromProviders( final DeploymentInfo into, final Set<IndyDeploymentProvider> fromProviders,
                                           final String contextRoot, final Application application )
    {
        for ( final IndyDeploymentProvider fromProvider : fromProviders )
        {
            final DeploymentInfo from = fromProvider.getDeploymentInfo( contextRoot, application );
            logger.debug( "Merging info from deployment provider: {}, info: {}", fromProvider, from );
            if ( from != null )
            {
                merge( into, from );
            }
        }
    }

    public static void merge( final DeploymentInfo into, final Set<DeploymentInfo> froms )
    {
        for ( final DeploymentInfo from : froms )
        {
            merge( into, from );
        }
    }

    public static void merge( final DeploymentInfo into, final DeploymentInfo from )
    {
        final Map<String, AuthenticationMechanismFactory> authMechs = from.getAuthenticationMechanisms();
        if ( authMechs != null )
        {
            for ( final Map.Entry<String, AuthenticationMechanismFactory> entry : authMechs.entrySet() )
            {
                logger.debug( "Found authentication mechanism: {}", entry.getKey() );
                into.addAuthenticationMechanism( entry.getKey(), entry.getValue() );
            }
        }

        if ( from.getAuthorizationManager() != null )
        {
            logger.debug( "Found authorization manager: {}", from.getAuthorizationManager() );
            into.setAuthorizationManager( from.getAuthorizationManager() );
        }

        if ( from.getConfidentialPortManager() != null )
        {
            logger.debug( "Found confidential port manager: {}", from.getConfidentialPortManager() );
            into.setConfidentialPortManager( from.getConfidentialPortManager() );
        }

        final List<ErrorPage> errorPages = from.getErrorPages();
        if ( errorPages != null )
        {
            logger.debug( "Found error pages: {}", errorPages );
            into.addErrorPages( errorPages );
        }

        if ( from.getExceptionHandler() != null )
        {
            logger.debug( "Found exception handler: {}", from.getExceptionHandler() );
            into.setExceptionHandler( from.getExceptionHandler() );
        }

        final List<FilterMappingInfo> filterMappings = from.getFilterMappings();
        if ( filterMappings != null )
        {
            for ( final FilterMappingInfo fmi : filterMappings )
            {
                switch ( fmi.getMappingType() )
                {
                    case SERVLET:
                    {
                        logger.debug( "Found servlet-name filter mapping: {} -> {}({})", fmi.getFilterName(),
                                      fmi.getMapping(), fmi.getDispatcher() );
                        into.addFilterServletNameMapping( fmi.getFilterName(), fmi.getMapping(), fmi.getDispatcher() );
                        break;
                    }
                    default:
                    {
                        logger.debug( "Found URL filter mapping: {} -> {}({})", fmi.getFilterName(),
                                      fmi.getMapping(), fmi.getDispatcher() );
                        into.addFilterUrlMapping( fmi.getFilterName(), fmi.getMapping(), fmi.getDispatcher() );
                    }
                }
            }
        }

        final Map<String, FilterInfo> filterInfos = from.getFilters();
        if ( filterInfos != null )
        {
            logger.debug( "Found filters: {}", filterInfos.keySet() );
            into.addFilters( filterInfos.values() );
        }

        if ( from.getIdentityManager() != null )
        {
            logger.debug( "Found identity manager: {}", from.getIdentityManager() );
            into.setIdentityManager( from.getIdentityManager() );
        }

        final Map<String, String> initParameters = from.getInitParameters();
        if ( initParameters != null )
        {
            for ( final Map.Entry<String, String> entry : initParameters.entrySet() )
            {
                logger.debug( "Init-Param: {} = {} from: {}", entry.getKey(), entry.getValue(), from );
                into.addInitParameter( entry.getKey(), entry.getValue() );
            }
        }

        final List<LifecycleInterceptor> lifecycleInterceptors = from.getLifecycleInterceptors();
        if ( lifecycleInterceptors != null )
        {
            for ( final LifecycleInterceptor lifecycleInterceptor : lifecycleInterceptors )
            {
                logger.debug( "Found lifecycle interceptor: {}", lifecycleInterceptor );
                into.addLifecycleInterceptor( lifecycleInterceptor );
            }
        }

        final List<ListenerInfo> listeners = from.getListeners();
        if ( listeners != null )
        {
            logger.debug( "Found listeners: {}", listeners.stream()
                                                          .map( li -> li.getListenerClass().getName() )
                                                          .collect( Collectors.toList() ) );
            into.addListeners( listeners );
        }

        if ( from.getMetricsCollector() != null )
        {
            logger.debug( "Found metrics collector: {}", from.getMetricsCollector() );
            into.setMetricsCollector( from.getMetricsCollector() );
        }

        final List<MimeMapping> mimeMappings = from.getMimeMappings();
        if ( mimeMappings != null )
        {
            logger.debug( "Found mime mappings: {}", mimeMappings.stream()
                                                                 .map( mm -> mm.getMimeType() + " -> "
                                                                         + mm.getExtension() )
                                                                 .collect( Collectors.toList() ) );
            into.addMimeMappings( mimeMappings );
        }

        final List<NotificationReceiver> notificationReceivers = from.getNotificationReceivers();
        if ( notificationReceivers != null )
        {
            logger.debug( "Found notification receivers: {}", notificationReceivers );
            into.addNotificationReceivers( notificationReceivers );
        }

        final Map<String, Set<String>> principalVersusRolesMap = from.getPrincipalVersusRolesMap();
        if ( principalVersusRolesMap != null )
        {
            for ( final Map.Entry<String, Set<String>> entry : principalVersusRolesMap.entrySet() )
            {
                logger.debug( "Found principle-roles mapping: {} -> {}", entry.getKey(), entry.getValue() );
                into.addPrincipalVsRoleMappings( entry.getKey(), entry.getValue() );
            }
        }

        final List<SecurityConstraint> securityConstraints = from.getSecurityConstraints();
        if ( securityConstraints != null )
        {
            if ( logger.isDebugEnabled() )
            {
                for ( final SecurityConstraint sc : securityConstraints )
                {
                    logger.debug( "Security Constraint: {} from: {}", sc, from );
                }
            }
            into.addSecurityConstraints( securityConstraints );
        }

        final LoginConfig loginConfig = from.getLoginConfig();
        if ( loginConfig != null )
        {
            logger.debug( "Login Config with realm: {} and mechanism: {} from: {}", loginConfig.getRealmName(),
                          loginConfig.getAuthMethods(), from );
            if ( into.getLoginConfig() != null )
            {
                throw new IllegalStateException(
                                                 "Two or more deployment providers are attempting to provide login configurations! Enable debug logging to see more." );
            }
            into.setLoginConfig( loginConfig );
        }

        if ( from.getSecurityContextFactory() != null )
        {
            logger.debug( "Found security context factory: {}", from.getSecurityContextFactory() );
            into.setSecurityContextFactory( from.getSecurityContextFactory() );
        }

        final Set<String> securityRoles = from.getSecurityRoles();
        if ( securityRoles != null )
        {
            logger.debug( "Found security roles: {}", securityRoles );
            into.addSecurityRoles( securityRoles );
        }

        final List<ServletContainerInitializerInfo> servletContainerInitializers =
            from.getServletContainerInitializers();
        if ( servletContainerInitializers != null )
        {
            logger.debug( "Found servlet container initializers: {}", servletContainerInitializers.stream()
                                                                                                  .map( sci -> sci.getServletContainerInitializerClass()
                                                                                                                  .getName() )
                                                                                                  .collect(
                                                                                                          Collectors.toList() ) );
            into.addServletContainerInitalizers( servletContainerInitializers );
        }

        final Map<String, Object> servletContextAttributes = from.getServletContextAttributes();
        if ( servletContextAttributes != null )
        {
            for ( final Map.Entry<String, Object> entry : servletContextAttributes.entrySet() )
            {
                logger.debug( "Found servlet context attribute: {} -> {}", entry.getKey(), entry.getValue() );

                into.addServletContextAttribute( entry.getKey(), entry.getValue() );
            }
        }

        final List<ServletExtension> servletExtensions = from.getServletExtensions();
        if ( servletExtensions != null )
        {
            for ( final ServletExtension servletExtension : servletExtensions )
            {
                logger.debug( "Found servlet extension: {}", servletExtension );
                into.addServletExtension( servletExtension );
            }
        }

        final Map<String, ServletInfo> servletInfos = from.getServlets();
        if ( servletInfos != null )
        {
            logger.debug( "Found servlets: {}", servletInfos.values()
                                                            .stream()
                                                            .map( si -> si.getName() + " => " + si.getMappings() )
                                                            .collect( Collectors.toList() ) );
            into.addServlets( servletInfos.values() );
        }

        final List<SessionListener> sessionListeners = from.getSessionListeners();
        if ( sessionListeners != null )
        {
            for ( final SessionListener sessionListener : sessionListeners )
            {
                logger.debug( "Found session listener: {}", sessionListener );
                into.addSessionListener( sessionListener );
            }
        }

        if ( from.getSessionManagerFactory() != null )
        {
            logger.debug( "Found session manager factory: {}", from.getSessionManagerFactory() );
            into.setSessionManagerFactory( from.getSessionManagerFactory() );
        }

        if ( from.getSessionPersistenceManager() != null )
        {
            logger.debug( "Found session persistence manager: {}", from.getSessionPersistenceManager() );
            into.setSessionPersistenceManager( from.getSessionPersistenceManager() );
        }

        if ( from.getTempDir() != null )
        {
            logger.debug( "Found temp dir: {}", from.getTempDir() );
            into.setTempDir( from.getTempDir() );
        }

        final List<String> welcomePages = from.getWelcomePages();
        if ( welcomePages != null )
        {
            logger.debug( "Found welcome pages: {}", welcomePages );
            into.addWelcomePages( welcomePages );
        }

        final List<HandlerWrapper> initWrappers = from.getInitialHandlerChainWrappers();
        if ( initWrappers != null )
        {
            for ( final HandlerWrapper wrapper : initWrappers )
            {
                logger.debug( "Found initial handler chain wrapper: {}", wrapper );
                into.addInitialHandlerChainWrapper( wrapper );
            }
        }

        final List<HandlerWrapper> outerWrappers = from.getOuterHandlerChainWrappers();
        if ( outerWrappers != null )
        {
            for ( final HandlerWrapper wrapper : outerWrappers )
            {
                logger.debug( "Found outer handler chain wrapper: {}", wrapper );
                into.addOuterHandlerChainWrapper( wrapper );
            }
        }

        final List<HandlerWrapper> innerWrappers = from.getInnerHandlerChainWrappers();
        if ( innerWrappers != null )
        {
            for ( final HandlerWrapper wrapper : innerWrappers )
            {
                logger.debug( "Found inner handler chain wrapper: {}", wrapper );
                into.addInnerHandlerChainWrapper( wrapper );
            }
        }
    }

}
