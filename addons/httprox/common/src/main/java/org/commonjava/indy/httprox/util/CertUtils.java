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
package org.commonjava.indy.httprox.util;

//import sun.security.tools.keytool.CertAndKeyGen;

import sun.security.x509.AlgorithmId;
import sun.security.x509.BasicConstraintsExtension;
import sun.security.x509.CertificateAlgorithmId;
import sun.security.x509.CertificateExtensions;
import sun.security.x509.CertificateSerialNumber;
import sun.security.x509.CertificateValidity;
import sun.security.x509.CertificateVersion;
import sun.security.x509.CertificateX509Key;
import sun.security.x509.X500Name;
import sun.security.x509.X509CertImpl;
import sun.security.x509.X509CertInfo;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Date;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Created by ruhan on 9/18/18.
 */
public class CertUtils
{
    public static final String DEFAULT_SIGN_ALGORITHM = "SHA256withRSA";

    public static final String KEY_TYPE_RSA = "RSA";

    public static final String CERT_TYPE_X509 = "X.509";

    public static final int DEFAULT_CERT_EXPIRATION_DAYS = 365;

    /**
     * Create a self-signed X.509 cert
     *
     * @param dn        the X.509 Distinguished Name, eg "CN=Test, L=London, C=GB"
     * @param days      how many days from now the cert is valid for
     * @param algorithm the signing algorithm, eg "SHA256withRSA"
     */
    public static X509Certificate generateX509Certificate( KeyPair pair, String dn, int days, String algorithm )
                    throws GeneralSecurityException, IOException
    {
        PrivateKey privateKey = pair.getPrivate();
        X509CertInfo info = new X509CertInfo();
        Date from = new Date();
        Date to = new Date( from.getTime() + TimeUnit.DAYS.toMillis( days ) );
        CertificateValidity interval = new CertificateValidity( from, to );
        BigInteger sn = new BigInteger( 64, new SecureRandom() );
        X500Name owner = new X500Name( dn );

        info.set( X509CertInfo.VALIDITY, interval );
        info.set( X509CertInfo.SERIAL_NUMBER, new CertificateSerialNumber( sn ) );
        info.set( X509CertInfo.SUBJECT, owner );
        info.set( X509CertInfo.ISSUER, owner );
        info.set( X509CertInfo.KEY, new CertificateX509Key( pair.getPublic() ) );
        info.set( X509CertInfo.VERSION, new CertificateVersion( CertificateVersion.V3 ) );

        AlgorithmId algo = new AlgorithmId( AlgorithmId.sha256WithRSAEncryption_oid );
        info.set( X509CertInfo.ALGORITHM_ID, new CertificateAlgorithmId( algo ) );

        // Sign the cert to identify the algorithm that's used.
        X509CertImpl cert = new X509CertImpl( info );
        cert.sign( privateKey, algorithm );

        // Update the algorithm, and resign.
        algo = (AlgorithmId) cert.get( X509CertImpl.SIG_ALG );
        info.set( CertificateAlgorithmId.NAME + "." + CertificateAlgorithmId.ALGORITHM, algo );
        cert = new X509CertImpl( info );
        cert.sign( privateKey, algorithm );
        return cert;
    }

    public static X509Certificate loadX509Certificate( File file ) throws CertificateException, IOException
    {
        CertificateFactory cf = CertificateFactory.getInstance( CERT_TYPE_X509 );
        Certificate ca;
        try (InputStream caInput = new BufferedInputStream( new FileInputStream( file ) ))
        {
            ca = cf.generateCertificate( caInput );
        }
        return (X509Certificate) ca;
    }

    public static KeyStore createKeyStore()
                    throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException
    {
        String keyStoreType = KeyStore.getDefaultType();
        KeyStore keyStore = KeyStore.getInstance( keyStoreType );
        keyStore.load( null, null );
        return keyStore;
    }

    public static KeyStore loadKeyStore( File file, String passwd )
                    throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException
    {
        String keyStoreType = KeyStore.getDefaultType(); // jks
        KeyStore keyStore = KeyStore.getInstance( keyStoreType );
        keyStore.load( new FileInputStream( file ), passwd.toCharArray() );
        return keyStore;
    }

    public static PrivateKey getPrivateKey( String filename ) throws Exception
    {
        byte[] keyBytes = Files.readAllBytes( Paths.get( filename ) );

        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec( keyBytes );
        KeyFactory kf = KeyFactory.getInstance( KEY_TYPE_RSA );
        return kf.generatePrivate( spec );
    }

    public static PublicKey getPublicKey( String filename ) throws Exception
    {
        byte[] keyBytes = Files.readAllBytes( Paths.get( filename ) );

        X509EncodedKeySpec spec = new X509EncodedKeySpec( keyBytes );
        KeyFactory kf = KeyFactory.getInstance( KEY_TYPE_RSA );
        return kf.generatePublic( spec );
    }

    public static X509Certificate createSignedCertificate( X509Certificate certificate,
                                                           X509Certificate issuerCertificate,
                                                           PrivateKey issuerPrivateKey, boolean isIntermediate )
                    throws Exception
    {
        Principal issuer = issuerCertificate.getSubjectDN();
        String issuerSigAlg = issuerCertificate.getSigAlgName();

        byte[] inCertBytes = certificate.getTBSCertificate();
        X509CertInfo info = new X509CertInfo( inCertBytes );
        info.set( X509CertInfo.ISSUER, issuer );

        if ( isIntermediate )
        {
            CertificateExtensions exts = new CertificateExtensions();
            BasicConstraintsExtension bce = new BasicConstraintsExtension( true, -1 );
            exts.set( BasicConstraintsExtension.NAME, new BasicConstraintsExtension( false, bce.getExtensionValue() ) );
            info.set( X509CertInfo.EXTENSIONS, exts );
        }

        X509CertImpl outCert = new X509CertImpl( info );
        outCert.sign( issuerPrivateKey, issuerSigAlg );

        return outCert;
    }

    public static CertificateAndKeys createSignedCertificateAndKey( String dn, X509Certificate issuerCertificate,
                                                                    PrivateKey issuerPrivateKey,
                                                                    boolean isIntermediate ) throws Exception
    {
        /*
         * CertAndKeyGen is jre class. Maven compile will fail unless use some additional plugin settings.
         * Although it is neat and nice, ATM we just use old fashioned code to create cert.
         *
        CertAndKeyGen gen = new CertAndKeyGen( KEY_TYPE_RSA, DEFAULT_SIGN_ALGORITHM, null );
        gen.generate( KEY_BITS );
        X509Certificate cert = gen.getSelfCertificate(new X500Name(dn), TimeUnit.DAYS.toMillis( DEFAULT_CERT_EXPIRATION_DAYS ));
        */

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance( KEY_TYPE_RSA );
        KeyPair pair = keyPairGenerator.generateKeyPair();

        X509Certificate cert = generateX509Certificate( pair, dn, DEFAULT_CERT_EXPIRATION_DAYS, DEFAULT_SIGN_ALGORITHM );

        X509Certificate signedCertificate =
                        createSignedCertificate( cert, issuerCertificate, issuerPrivateKey, isIntermediate );
        PublicKey publicKey = pair.getPublic();
        PrivateKey privateKey = pair.getPrivate();
        return new CertificateAndKeys( signedCertificate, privateKey, publicKey );
    }

}
