
import org.commonjava.aprox.autoprox.conf.AutoProxFactory

import java.net.MalformedURLException

import org.commonjava.aprox.model.*

import groovy.json.JsonSlurper

import java.io.*

class LegacyFactory implements AutoProxFactory
{
    def remote = null
    def group = null
    def hosted = null
    
    LegacyFactory()
    {
        def slurper = new JsonSlurper();
        
        def jsonFile = new File( System.getProperty("aprox.config.dir"), "autoprox.json" )
        
        if ( jsonFile )
        {
            def json = slurper.parse( jsonFile )
            
            remote = json.remote
            if ( !remote )
            {
                remote = json.repo
            }
            
            hosted = json.hosted
            if ( !hosted )
            {
                hosted = json.deploy
            }
            
            group = json.group
        }
    }
    
    RemoteRepository createRemoteRepository( String named )
        throws MalformedURLException
    {
        if ( !remote )
            return null
        
        RemoteRepository r = new RemoteRepository( named, remote.url.replaceAll( /\$\{name\}/, named ) );
        
        if ( remote.timeout_seconds ) r.setTimeoutSeconds( remote.timeout_seconds )
        if ( remote.user ) r.setUser( remote.user )
        if ( remote.password ) r.setPassword( remote.password )
        if ( remote.passthrough ) r.setPassthrough( remote.passthrough )
        if ( remote.timeout_seconds ) r.setTimeoutSeconds( remote.timeout_seconds )
        if ( remote.timeout_seconds ) r.setTimeoutSeconds( remote.timeout_seconds )
        if ( remote.timeout_seconds ) r.setTimeoutSeconds( remote.timeout_seconds )
        if ( remote.cache_timeout_seconds ) r.setCacheTimeoutSeconds( remote.cache_timeout_seconds )
        if ( remote.key_password ) r.setKeyPassword( remote.key_password )
        if ( remote.key_certificate_pem ) r.setKeyCertificatePem( remote.key_certificate_pem )
        if ( remote.server_certificate_pem ) r.setServerCertificatePem( remote.server_certificate_pem )
        if ( remote.proxy_host ) r.setProxyHost( remote.proxy_host )
        if ( remote.proxy_port ) r.setProxyPort( remote.proxy_port )
        if ( remote.proxy_user ) r.setProxyUser( remote.proxy_user )
        if ( remote.proxy_password ) r.setProxyPassword( remote.proxy_password )
        
        r
    }

    HostedRepository createHostedRepository( String named )
    {
        if ( !hosted ) return null
        
        HostedRepository h = new HostedRepository( named );
        
        h.setAllowSnapshots( hosted.allow_snapshots == true )
        h.setAllowReleases( hosted.allow_releases == true )
        
        h
    }

    Group createGroup( String named, RemoteRepository remote, HostedRepository hosted )
    {
        if ( !group ) return null
        
        Group g = new Group( named );
        
        group.constituents.each {
            switch ( it )
            {
                case ["\${deploy}", "\${hosted}"]:
                  def h = createHostedRepository( named )
                  if ( h ) g.addConstituent( h.getKey() )
                  break
                  
                case ["\${repository}", "\${remote}"]:
                  def r = createRemoteRepository( named )
                  if ( r ) g.addConstituent( r.getKey() )
                  break

                default:
                  def match = (it =~ /([^:]+):(.+)/)[0]
                  StoreType type = StoreType.get( match[1] )
                  if ( !type ) type = StoreType.remote
                  
                  g.addConstituent( new StoreKey( type, match[2] ) )
            }
        }
        
        g
    }

    String getRemoteValidationPath()
    {
        null
    }
}