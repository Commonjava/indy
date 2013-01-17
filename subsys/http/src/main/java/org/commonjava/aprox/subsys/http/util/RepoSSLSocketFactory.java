package org.commonjava.aprox.subsys.http.util;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.Enumeration;

import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.params.HttpParams;
import org.commonjava.aprox.filer.FileManager;
import org.commonjava.aprox.model.Repository;
import org.commonjava.util.logging.Logger;

public class RepoSSLSocketFactory
    extends SSLSocketFactory
{

    // private final Map<Repository, SSLSocketFactory> repoFactories = new WeakHashMap<Repository, SSLSocketFactory>();

    private final Logger logger = new Logger( getClass() );

    private final TLRepositoryCredentialsProvider credProvider;

    public RepoSSLSocketFactory( final TLRepositoryCredentialsProvider credProvider )
        throws KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException
    {
        super( (TrustStrategy) null, BROWSER_COMPATIBLE_HOSTNAME_VERIFIER );
        this.credProvider = credProvider;
    }

    @Override
    public Socket createSocket( final HttpParams params )
        throws IOException
    {
        logger.info( "Creating socket...looking for repository definition in parameters..." );
        final Repository repo = (Repository) params.getParameter( FileManager.HTTP_PARAM_REPO );

        if ( repo != null )
        {
            logger.info( "Creating socket...using repository: %s", repo );
            final SSLSocketFactory fac = getRepoSSLFactory( repo );
            if ( fac != null )
            {
                logger.info( "Creating socket using repo-specific factory" );
                return fac.createSocket( params );
            }
            else
            {
                logger.info( "No repo-specific factory; Creating socket using default factory (this)" );
                return super.createSocket( params );
            }
        }
        else
        {
            logger.info( "No repo; Creating socket using default factory (this)" );
            return super.createSocket( params );
        }
    }

    private synchronized SSLSocketFactory getRepoSSLFactory( final Repository repo )
        throws IOException
    {
        logger.info( "Finding SSLSocketFactory for repo: %s", repo.getName() );

        SSLSocketFactory factory = null; // repoFactories.get( repo );
        if ( factory == null )
        {
            KeyStore ks = null;
            KeyStore ts = null;

            final String kcPem = repo.getKeyCertPem();
            final String kcPass = repo.getKeyPassword();
            if ( kcPem != null )
            {
                if ( kcPass == null || kcPass.length() < 1 )
                {
                    logger.error( "Invalid configuration. Repository: %s cannot have an empty key password!",
                                  repo.getName() );
                    throw new IOException( "Repository: " + repo.getName() + " is misconfigured!" );
                }

                try
                {
                    ks = SSLUtils.readKeyAndCert( kcPem, kcPass );

                    final StringBuilder sb = new StringBuilder();
                    sb.append( "Keystore contains the following certificates:" );

                    for ( final Enumeration<String> aliases = ks.aliases(); aliases.hasMoreElements(); )
                    {
                        final String alias = aliases.nextElement();
                        final X509Certificate cert = (X509Certificate) ks.getCertificate( alias );

                        if ( cert != null )
                        {
                            sb.append( "\n" )
                              .append( cert.getSubjectDN() );
                        }
                    }
                    sb.append( "\n" );
                    logger.info( sb.toString() );
                }
                catch ( final CertificateException e )
                {
                    logger.error( "Invalid configuration. Repository: %s has an invalid client certificate! Error: %s",
                                  e, repo.getName(), e.getMessage() );
                    throw new IOException( "Failed to initialize SSL connection for repository: " + repo.getName() );
                }
                catch ( final KeyStoreException e )
                {
                    logger.error( "Invalid configuration. Cannot initialize keystore for repository: %s. Error: %s", e,
                                  repo.getName(), e.getMessage() );
                    throw new IOException( "Failed to initialize SSL connection for repository: " + repo.getName() );
                }
                catch ( final NoSuchAlgorithmException e )
                {
                    logger.error( "Invalid configuration. Cannot initialize keystore for repository: %s. Error: %s", e,
                                  repo.getName(), e.getMessage() );
                    throw new IOException( "Failed to initialize SSL connection for repository: " + repo.getName() );
                }
                catch ( final InvalidKeySpecException e )
                {
                    logger.error( "Invalid configuration. Invalid client key for repository: %s. Error: %s", e,
                                  repo.getName(), e.getMessage() );
                    throw new IOException( "Failed to initialize SSL connection for repository: " + repo.getName() );
                }
            }

            final String sPem = repo.getServerCertPem();
            logger.info( "Server certificate PEM:\n%s", sPem );
            if ( sPem != null )
            {
                try
                {
                    ts = SSLUtils.readCerts( sPem, repo.getHost() );

                    final StringBuilder sb = new StringBuilder();
                    sb.append( "Trust store contains the following certificates:" );

                    for ( final Enumeration<String> aliases = ts.aliases(); aliases.hasMoreElements(); )
                    {
                        final String alias = aliases.nextElement();
                        final X509Certificate cert = (X509Certificate) ts.getCertificate( alias );
                        if ( cert != null )
                        {
                            sb.append( "\n" )
                              .append( cert.getSubjectDN() );
                        }
                    }
                    sb.append( "\n" );
                    logger.info( sb.toString() );
                }
                catch ( final CertificateException e )
                {
                    logger.error( "Invalid configuration. Repository: %s has an invalid server certificate! Error: %s",
                                  e, repo.getName(), e.getMessage() );
                    throw new IOException( "Failed to initialize SSL connection for repository: " + repo.getName() );
                }
                catch ( final KeyStoreException e )
                {
                    logger.error( "Invalid configuration. Cannot initialize keystore for repository: %s. Error: %s", e,
                                  repo.getName(), e.getMessage() );
                    throw new IOException( "Failed to initialize SSL connection for repository: " + repo.getName() );
                }
                catch ( final NoSuchAlgorithmException e )
                {
                    logger.error( "Invalid configuration. Cannot initialize keystore for repository: %s. Error: %s", e,
                                  repo.getName(), e.getMessage() );
                    throw new IOException( "Failed to initialize SSL connection for repository: " + repo.getName() );
                }
            }

            if ( ks != null || ts != null )
            {
                try
                {
                    factory =
                        new SSLSocketFactory( SSLSocketFactory.TLS, ks, kcPass, ts, null, null,
                                              SSLSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER );

                    // repoFactories.put( repo, factory );
                }
                catch ( final KeyManagementException e )
                {
                    logger.error( "Invalid configuration. Cannot initialize SSL socket factory for repository: %s. Error: %s",
                                  e, repo.getName(), e.getMessage() );
                    throw new IOException( "Failed to initialize SSL connection for repository: " + repo.getName() );
                }
                catch ( final UnrecoverableKeyException e )
                {
                    logger.error( "Invalid configuration. Cannot initialize SSL socket factory for repository: %s. Error: %s",
                                  e, repo.getName(), e.getMessage() );
                    throw new IOException( "Failed to initialize SSL connection for repository: " + repo.getName() );
                }
                catch ( final NoSuchAlgorithmException e )
                {
                    logger.error( "Invalid configuration. Cannot initialize SSL socket factory for repository: %s. Error: %s",
                                  e, repo.getName(), e.getMessage() );
                    throw new IOException( "Failed to initialize SSL connection for repository: " + repo.getName() );
                }
                catch ( final KeyStoreException e )
                {
                    logger.error( "Invalid configuration. Cannot initialize SSL socket factory for repository: %s. Error: %s",
                                  e, repo.getName(), e.getMessage() );
                    throw new IOException( "Failed to initialize SSL connection for repository: " + repo.getName() );
                }
            }
        }

        return factory;
    }

    @Override
    public Socket createLayeredSocket( final Socket socket, final String host, final int port, final boolean autoClose )
        throws IOException, UnknownHostException
    {
        logger.info( "Creating LAYERED socket to: %s:%d...looking for repository definition in parameters...", host,
                     port );

        // FIXME: This is prone to confusion if multiple repos using the same host/port have different configs!!!
        final Repository repo = credProvider.getRepository( host, port < 0 ? 443 : port );

        if ( repo != null )
        {
            logger.info( "Creating socket...using repository: %s", repo );
            final SSLSocketFactory fac = getRepoSSLFactory( repo );
            if ( fac != null )
            {
                logger.info( "Creating socket using repo-specific factory" );
                return fac.createLayeredSocket( socket, host, port, autoClose );
            }
            else
            {
                logger.info( "No repo-specific factory; Creating socket using default factory (this)" );
                return super.createLayeredSocket( socket, host, port, autoClose );
            }
        }
        else
        {
            logger.info( "No repo; Creating socket using default factory (this)" );
            return super.createLayeredSocket( socket, host, port, autoClose );
        }
    }

}
