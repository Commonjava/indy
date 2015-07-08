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
package org.commonjava.aprox.bind.jaxrs.util;

import io.undertow.security.api.AuthenticationMechanismFactory;
import io.undertow.security.api.NotificationReceiver;
import io.undertow.server.session.SessionListener;
import io.undertow.servlet.ServletExtension;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.ErrorPage;
import io.undertow.servlet.api.FilterInfo;
import io.undertow.servlet.api.FilterMappingInfo;
import io.undertow.servlet.api.LifecycleInterceptor;
import io.undertow.servlet.api.ListenerInfo;
import io.undertow.servlet.api.MimeMapping;
import io.undertow.servlet.api.SecurityConstraint;
import io.undertow.servlet.api.ServletContainerInitializerInfo;
import io.undertow.servlet.api.ServletInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.commonjava.aprox.bind.jaxrs.AproxDeploymentProvider;

public final class DeploymentInfoUtils
{

    private DeploymentInfoUtils()
    {
    }

    public static void mergeFromProviders( final DeploymentInfo into, final Set<AproxDeploymentProvider> fromProviders )
    {
        for ( final AproxDeploymentProvider fromProvider : fromProviders )
        {
            final DeploymentInfo from = fromProvider.getDeploymentInfo();
            merge( into, from );
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
                into.addAuthenticationMechanism( entry.getKey(), entry.getValue() );
            }
        }

        if ( from.getAuthorizationManager() != null )
        {
            into.setAuthorizationManager( from.getAuthorizationManager() );
        }

        if ( from.getConfidentialPortManager() != null )
        {
            into.setConfidentialPortManager( from.getConfidentialPortManager() );
        }

        final List<ErrorPage> errorPages = from.getErrorPages();
        if ( errorPages != null )
        {
            into.addErrorPages( errorPages );
        }

        if ( from.getExceptionHandler() != null )
        {
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
                        into.addFilterServletNameMapping( fmi.getFilterName(), fmi.getMapping(), fmi.getDispatcher() );
                        break;
                    }
                    default:
                    {
                        into.addFilterUrlMapping( fmi.getFilterName(), fmi.getMapping(), fmi.getDispatcher() );
                    }
                }
            }
        }

        final Map<String, FilterInfo> filterInfos = from.getFilters();
        if ( filterInfos != null )
        {
            into.addFilters( filterInfos.values() );
        }

        if ( from.getIdentityManager() != null )
        {
            into.setIdentityManager( from.getIdentityManager() );
        }

        final Map<String, String> initParameters = from.getInitParameters();
        if ( initParameters != null )
        {
            for ( final Map.Entry<String, String> entry : initParameters.entrySet() )
            {
                into.addInitParameter( entry.getKey(), entry.getValue() );
            }
        }

        final List<LifecycleInterceptor> lifecycleInterceptors = from.getLifecycleInterceptors();
        if ( lifecycleInterceptors != null )
        {
            for ( final LifecycleInterceptor lifecycleInterceptor : lifecycleInterceptors )
            {
                into.addLifecycleInterceptor( lifecycleInterceptor );
            }
        }

        final List<ListenerInfo> listeners = from.getListeners();
        if ( listeners != null )
        {
            into.addListeners( listeners );
        }

        if ( from.getLoginConfig() != null )
        {
            into.setLoginConfig( from.getLoginConfig() );
        }

        if ( from.getMetricsCollector() != null )
        {
            into.setMetricsCollector( from.getMetricsCollector() );
        }

        final List<MimeMapping> mimeMappings = from.getMimeMappings();
        if ( mimeMappings != null )
        {
            into.addMimeMappings( mimeMappings );
        }

        final List<NotificationReceiver> notificationReceivers = from.getNotificationReceivers();
        if ( notificationReceivers != null )
        {
            into.addNotificationReceivers( notificationReceivers );
        }

        final Map<String, Set<String>> principalVersusRolesMap = from.getPrincipalVersusRolesMap();
        if ( principalVersusRolesMap != null )
        {
            for ( final Map.Entry<String, Set<String>> entry : principalVersusRolesMap.entrySet() )
            {
                into.addPrincipalVsRoleMappings( entry.getKey(), entry.getValue() );
            }
        }

        final List<SecurityConstraint> securityConstraints = from.getSecurityConstraints();
        if ( securityConstraints != null )
        {
            into.addSecurityConstraints( securityConstraints );
        }

        if ( from.getSecurityContextFactory() != null )
        {
            into.setSecurityContextFactory( from.getSecurityContextFactory() );
        }

        final Set<String> securityRoles = from.getSecurityRoles();
        if ( securityRoles != null )
        {
            into.addSecurityRoles( securityRoles );
        }

        final List<ServletContainerInitializerInfo> servletContainerInitializers =
            from.getServletContainerInitializers();
        if ( servletContainerInitializers != null )
        {
            into.addServletContainerInitalizers( servletContainerInitializers );
        }

        final Map<String, Object> servletContextAttributes = from.getServletContextAttributes();
        if ( servletContextAttributes != null )
        {
            for ( final Map.Entry<String, Object> entry : servletContextAttributes.entrySet() )
            {
                into.addServletContextAttribute( entry.getKey(), entry.getValue() );
            }
        }

        final List<ServletExtension> servletExtensions = from.getServletExtensions();
        if ( servletExtensions != null )
        {
            for ( final ServletExtension servletExtension : servletExtensions )
            {
                into.addServletExtension( servletExtension );
            }
        }

        final Map<String, ServletInfo> servletInfos = from.getServlets();
        if ( servletInfos != null )
        {
            into.addServlets( servletInfos.values() );
        }

        final List<SessionListener> sessionListeners = from.getSessionListeners();
        if ( sessionListeners != null )
        {
            for ( final SessionListener sessionListener : sessionListeners )
            {
                into.addSessionListener( sessionListener );
            }
        }

        if ( from.getSessionManagerFactory() != null )
        {
            into.setSessionManagerFactory( from.getSessionManagerFactory() );
        }

        if ( from.getSessionPersistenceManager() != null )
        {
            into.setSessionPersistenceManager( from.getSessionPersistenceManager() );
        }

        if ( from.getTempDir() != null )
        {
            into.setTempDir( from.getTempDir() );
        }

        final List<String> welcomePages = from.getWelcomePages();
        if ( welcomePages != null )
        {
            into.addWelcomePages( welcomePages );
        }
    }

}
