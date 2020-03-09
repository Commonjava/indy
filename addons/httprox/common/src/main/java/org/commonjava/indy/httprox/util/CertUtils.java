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
package org.commonjava.indy.httprox.util;

import static org.bouncycastle.asn1.x509.X509Extension.keyUsage;
import static org.bouncycastle.jce.X509KeyUsage.digitalSignature;

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
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x509.X509Extension;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.asn1.x509.KeyUsage;

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
     * Create signed certificate using issuer certificate and private key. This has been optimized 
     * @param dn                    Distinguished name
     * @param issuerCertificate     Issuer certificate
     * @param issuerPrivateKey      Issuer private key
     * @param isIntermediate        Is the generated certificate an Intermediate
     * @return                      Certificate and associated keys for a host
     * @throws OperatorCreationException
     * @throws Exception
     */
    public static CertificateAndKeys createSignedCertificateAndKey( String dn, X509Certificate issuerCertificate,
            PrivateKey issuerPrivateKey,
            boolean isIntermediateNeeded ) 
    throws OperatorCreationException, Exception
    {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance( KEY_TYPE_RSA );
        X509Certificate parentCertificate = null;
        PrivateKey parentPrivateKey = null;

        if ( isIntermediateNeeded )
        {
            KeyPair intermediatePair = keyPairGenerator.generateKeyPair();
            X500Principal intermediatePrincipal = new X500Principal( "CN=Test Org Intermediate CA" );
            X509Certificate intermediateCertificate = createIntermediateSignedCertificate( intermediatePair.getPublic(),
                intermediatePrincipal, DEFAULT_CERT_EXPIRATION_DAYS, DEFAULT_SIGN_ALGORITHM,
                issuerCertificate, issuerPrivateKey);
            parentCertificate = intermediateCertificate;
            parentPrivateKey = intermediatePair.getPrivate();
        }
        else
        {
            parentCertificate = issuerCertificate;
            parentPrivateKey = issuerPrivateKey;
        }

        KeyPair pair = keyPairGenerator.generateKeyPair();
        X500Principal endEntityPrincipal = new X500Principal( dn );
        X509Certificate signedCertificate =
            createEndEntitySignedCertificate( pair.getPublic(),
            endEntityPrincipal, DEFAULT_CERT_EXPIRATION_DAYS, DEFAULT_SIGN_ALGORITHM,
            parentCertificate, parentPrivateKey );

        PublicKey publicKey = signedCertificate.getPublicKey();
        PrivateKey privateKey = pair.getPrivate();
        return new CertificateAndKeys( signedCertificate, privateKey, publicKey );
    }

    /**
     * Create an intermediate certificate using an issuer certificate.
     * @param subPubKey         Subject PublicKey
     * @param subjectPrincipal  Subject Principal object
     * @param days              expiry after days
     * @param algorithm         algorithm to use to generate the certificate.
     * @param issuerCertificate Issuer certificate
     * @param issuerPrivateKey  Issuer PrivateKey
     * @return                  X509Certificate object
     * @throws Exception
     */
    public static X509Certificate createIntermediateSignedCertificate( PublicKey subPubKey, 
        X500Principal subjectPrincipal, int days, String algorithm, X509Certificate issuerCertificate,
            PrivateKey issuerPrivateKey )
        throws Exception
    {
        String issuerSigAlg = issuerCertificate.getSigAlgName();
        Date notBefore = new Date();
        Date notAfter = new Date( System.currentTimeMillis() + ( MILLIS_IN_DAY * 356 ) );
        JcaX509CertificateConverter converter = new JcaX509CertificateConverter();
        JcaContentSignerBuilder contentSignerBuilder = new JcaContentSignerBuilder( DEFAULT_SIGN_ALGORITHM ).setProvider( BouncyCastleProvider.PROVIDER_NAME );
        JcaX509v3CertificateBuilder v3CertGen = new JcaX509v3CertificateBuilder(
            issuerCertificate.getSubjectX500Principal(),
            allocateSerialNumber(),
            notBefore,
            notAfter,
            subjectPrincipal,
            subPubKey );

        JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();
        v3CertGen.addExtension(
            X509Extension.authorityKeyIdentifier,
            false,
            extUtils.createAuthorityKeyIdentifier( issuerCertificate ) );
        v3CertGen.addExtension(
            X509Extension.subjectKeyIdentifier,
            false,
            extUtils.createSubjectKeyIdentifier( subPubKey ) );
        v3CertGen.addExtension(
            X509Extension.basicConstraints,
            true,
            new BasicConstraints( 0 ) );
        v3CertGen.addExtension(
            X509Extension.keyUsage,
            true,
            new KeyUsage( KeyUsage.digitalSignature | KeyUsage.keyCertSign | KeyUsage.cRLSign ) );

    return converter.getCertificate( v3CertGen.build( contentSignerBuilder.build( issuerPrivateKey ) ) );
}

    /**
     * Generate X509Certificate using objects from existing issuer and subject certificates.
     * The generated certificate is signed by issuer PrivateKey.
     * @param subPubKey             Subject public key
     * @param dn                    Distinguished name
     * @param days                  Number of delays the new certificate is valid for
     * @param algorithm             Algorithm
     * @param issuerCertificate     Issuer certificate
     * @param issuerPrivateKey      Issue private key
     * @param isIntermediate        Is the generated certificate an Intermediate
     * @return                      X509Certificate
     * @throws Exception
     */
    public static X509Certificate createEndEntitySignedCertificate( PublicKey subPubKey, 
        X500Principal subjectPrincipal, int days, String algorithm, X509Certificate issuerCertificate,
            PrivateKey issuerPrivateKey )
        throws Exception
    {
        String issuerSigAlg = issuerCertificate.getSigAlgName();
        Date notBefore = new Date();
        Date notAfter = new Date( System.currentTimeMillis() + (MILLIS_IN_DAY * 356) );
        JcaX509CertificateConverter converter = new JcaX509CertificateConverter();
        JcaContentSignerBuilder contentSignerBuilder = new JcaContentSignerBuilder(DEFAULT_SIGN_ALGORITHM).setProvider(BouncyCastleProvider.PROVIDER_NAME);
        JcaX509v3CertificateBuilder v3CertGen = new JcaX509v3CertificateBuilder(
            issuerCertificate.getSubjectX500Principal(),
            allocateSerialNumber(),
            notBefore,
            notAfter,
            subjectPrincipal,
            subPubKey );

        JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();
        v3CertGen.addExtension(
            X509Extension.authorityKeyIdentifier,
            false,
            extUtils.createAuthorityKeyIdentifier( issuerCertificate ) );
        v3CertGen.addExtension(
            X509Extension.subjectKeyIdentifier,
            false,
            extUtils.createSubjectKeyIdentifier( subPubKey ) );
        v3CertGen.addExtension(
            X509Extension.basicConstraints,
            true,
            new BasicConstraints( 0 ) );
        v3CertGen.addExtension(
            X509Extension.keyUsage,
            true,
            new KeyUsage( KeyUsage.digitalSignature | KeyUsage.keyEncipherment ) );

        return converter.getCertificate( v3CertGen.build( contentSignerBuilder.build( issuerPrivateKey ) ) );
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
