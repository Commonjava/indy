/**
 * Copyright (C) 2011-2022 Red Hat, Inc. (https://github.com/Commonjava/indy)
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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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

import javax.security.auth.x500.X500Principal;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by ruhan on 9/18/18.
 */
public class CertUtils
{
    public static final String DEFAULT_SIGN_ALGORITHM = "SHA256withRSA";

    public static final String KEY_TYPE_RSA = "RSA";

    public static final String CERT_TYPE_X509 = "X.509";

    public static final int DEFAULT_CERT_EXPIRATION_DAYS = 365;

    public static final long MILLIS_IN_DAY = 1000L * 60 * 60 * 24;

    public static BigInteger serialNumber = new BigInteger( 64, new SecureRandom() );

    private static Logger logger = LoggerFactory.getLogger( CertUtils.class );

    static
    {
        java.security.Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * Create a self-signed X.509 cert
     *
     * @param pair      KeyPair generated for this request
     * @param dn        the X.509 Distinguished Name, eg "CN=Test, L=London, C=GB"
     * @param days      how many days from now the cert is valid for
     * @param algorithm the signing algorithm, eg "SHA256withRSA"
     * @return X509Certificate newly generated certificate
     */
    public static X509Certificate generateX509Certificate( KeyPair pair, String dn, int days, String algorithm )
                    throws GeneralSecurityException, OperatorCreationException, IOException
    {
        JcaX509CertificateConverter converter = new JcaX509CertificateConverter();
        PrivateKey subPrivKey = pair.getPrivate();
        PublicKey subPubKey = pair.getPublic();
        ContentSigner contentSignerBuilder = new JcaContentSignerBuilder( algorithm ).setProvider( BouncyCastleProvider.PROVIDER_NAME ).build( subPrivKey );
        X500Name name = new X500Name( dn );
        Date expires = new Date( System.currentTimeMillis() + (MILLIS_IN_DAY * days) );

        X509CertificateHolder holder = new X509v3CertificateBuilder(
            name,
            allocateSerialNumber(),
            new Date(),
            expires,
            name,
            SubjectPublicKeyInfo.getInstance( subPubKey.getEncoded() )
        ).build(contentSignerBuilder);

        X509Certificate cert = converter.getCertificate( holder );

        logger.debug( "Created cert using CA private key:\n" + cert.toString() );
        return cert;
    }

    /**
     * Load a certificate from a file
     * @param file      location of file
     * @return          certificate generated from the encoded file bytes
     * @throws CertificateException
     * @throws IOException
     */
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

    /**
     * Create a keystore object
     * @return          empty keystore
     * @throws KeyStoreException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    public static KeyStore createKeyStore()
                    throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException
    {
        String keyStoreType = KeyStore.getDefaultType();
        KeyStore keyStore = KeyStore.getInstance( keyStoreType );
        keyStore.load( null, null );
        return keyStore;
    }

    /**
     * Load a keystore using the contents of a file to populate the store
     * @param file
     * @param passwd
     * @return
     * @throws IOException
     * @throws KeyStoreException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     */
    public static KeyStore loadKeyStore( File file, String passwd )
                    throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException
    {
        String keyStoreType = KeyStore.getDefaultType(); // jks
        KeyStore keyStore = KeyStore.getInstance( keyStoreType );
        keyStore.load( new FileInputStream( file ), passwd.toCharArray() );
        return keyStore;
    }

    /**
     * Load a PrivateKey using the encoded bytes of a file
     * @param filename          file containing PrivateKey bytes
     * @return                  created PrivateKey
     * @throws Exception
     */
    public static PrivateKey getPrivateKey( String filename ) throws Exception
    {
        byte[] keyBytes = Files.readAllBytes( Paths.get( filename ) );

        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec( keyBytes );
        KeyFactory kf = KeyFactory.getInstance( KEY_TYPE_RSA );
        return kf.generatePrivate( spec );
    }

    /**
     * Load a PublicKey using the contents of a file
     * @param filename      file containing PublicKey
     * @return              created PublicKey
     * @throws Exception
     */
    public static PublicKey getPublicKey( String filename ) throws Exception
    {
        byte[] keyBytes = Files.readAllBytes( Paths.get( filename ) );

        X509EncodedKeySpec spec = new X509EncodedKeySpec( keyBytes );
        KeyFactory kf = KeyFactory.getInstance( KEY_TYPE_RSA );
        return kf.generatePublic( spec );
    }

    /**
     * Generate X509Certificate using objects from existing issuer and subject certificates.
     * The generated certificate is signed by issuer PrivateKey.
     * @param certificate
     * @param issuerCertificate
     * @param issuerPrivateKey
     * @param isIntermediate
     * @return
     * @throws Exception
     */
    public static X509Certificate createSignedCertificate( X509Certificate certificate,
                                                           X509Certificate issuerCertificate,
                                                           PrivateKey issuerPrivateKey, boolean isIntermediate )
                    throws Exception
    {
        String issuerSigAlg = issuerCertificate.getSigAlgName();
        X500Principal principal = issuerCertificate.getIssuerX500Principal();
        JcaX509CertificateConverter converter = new JcaX509CertificateConverter();
        JcaContentSignerBuilder contentSignerBuilder = new JcaContentSignerBuilder(issuerSigAlg).setProvider(BouncyCastleProvider.PROVIDER_NAME);
        JcaX509v3CertificateBuilder v3CertGen = new JcaX509v3CertificateBuilder(
            principal,
            certificate.getSerialNumber(),
            certificate.getNotBefore(),
            certificate.getNotAfter(),
            certificate.getSubjectX500Principal(),
            certificate.getPublicKey()
            );

        if ( isIntermediate )
        {
            v3CertGen.addExtension(
                Extension.basicConstraints,
                true,
                new BasicConstraints(-1));
        }

        return converter.getCertificate(v3CertGen.build(contentSignerBuilder.build(issuerPrivateKey)));
    }

    public static CertificateAndKeys createSignedCertificateAndKey( String dn, X509Certificate issuerCertificate,
                                                                    PrivateKey issuerPrivateKey,
                                                                    boolean isIntermediate ) 
            throws OperatorCreationException, Exception
    {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance( KEY_TYPE_RSA );
        KeyPair pair = keyPairGenerator.generateKeyPair();

        X509Certificate cert = generateX509Certificate( pair, dn, DEFAULT_CERT_EXPIRATION_DAYS, DEFAULT_SIGN_ALGORITHM );

        X509Certificate signedCertificate =
                        createSignedCertificate( cert, issuerCertificate, issuerPrivateKey, isIntermediate );
        PublicKey publicKey = signedCertificate.getPublicKey();
        PrivateKey privateKey = pair.getPrivate();
        return new CertificateAndKeys( signedCertificate, privateKey, publicKey );
    }

    private static BigInteger allocateSerialNumber()
    {
        BigInteger sn = serialNumber;
        synchronized (serialNumber) {
            serialNumber = new BigInteger( 64, new SecureRandom() );
        }
        return sn;
    }
}
