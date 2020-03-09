/**
 * Copyright (C) 2011-2020 Red Hat, Inc. (https://github.com/Commonjava/indy)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.indy.httprox.util;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v1CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;

public class GeneratePKIXArtifacts {

   static
   {
      java.security.Security.addProvider(new BouncyCastleProvider());
   }

   public static void main(String[] args)
      throws Exception
   {
      KeyStore store = createKeyStore();
      KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance( "RSA" );
      keyPairGenerator.initialize(2048, new SecureRandom());
      KeyPair pair = keyPairGenerator.generateKeyPair();
      Date notAfter = new Date(System.currentTimeMillis() + MILLIS_IN_YEAR * 10);
      JcaX509v1CertificateBuilder certGen = new JcaX509v1CertificateBuilder( 
            new X500Name("CN=Test CA, O=Test Org"),
            BigInteger.valueOf(3),
            new Date(),
            notAfter,
            new X500Name("CN=Test CA, O=Test Org"),
            pair.getPublic() );

      JcaContentSignerBuilder contentSignerBuilder = new JcaContentSignerBuilder("SHA256WithRSA");
      contentSignerBuilder.setProvider(BouncyCastleProvider.PROVIDER_NAME);

      X509Certificate cert = new JcaX509CertificateConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME)
            .getCertificate( certGen.build(contentSignerBuilder.build(pair.getPrivate())));
   
      store.setCertificateEntry("ca", cert);
      File ks = new File( TEMP, "ca.jks" );
      try (OutputStream fos = new BufferedOutputStream(new FileOutputStream(ks)))
      {
         store.store(fos, "passwd".toCharArray());
         System.out.println("Saved key store file");
      }

      writeCertificateAsPEM("ca.crt", cert, "CERTIFICATE");
      System.out.println("Saved CA certificate file");

      writePrivateKeyAsPEM("ca.key", pair.getPrivate(), "Test Org root CA private key" );
      System.out.println("Saved CA private key file");

      writePrivateKeyAsBINARY("ca.der", pair.getPrivate() );
      System.out.println("Saved CA private key file as binary file ");
   }

   private static KeyStore createKeyStore()
         throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException
   {
      KeyStore keyStore = KeyStore.getInstance( "JKS" );
      keyStore.load( null, null );
      return keyStore;
   }

   private static void writeCertificateAsPEM(String name, X509Certificate cert, String description )
      throws IOException, CertificateEncodingException
   {
      PemObject object = new PemObject(description, cert.getEncoded());
      try (PemWriter writer = new PemWriter(new BufferedWriter(new FileWriter(new File ( TEMP, name )))))
      {
         writer.writeObject(object);
      }
   }
   
   private static void writePrivateKeyAsPEM(String name, PrivateKey privKey, String description) 
      throws Exception
   {
      PemObject object = new PemObject(description, privKey.getEncoded());
      try (PemWriter writer = new PemWriter(  new BufferedWriter(new FileWriter(new File ( TEMP, name )))))
      {
         writer.writeObject( object );
      }
   }

   private static void writePrivateKeyAsBINARY(String name, PrivateKey privKey ) 
         throws Exception
   {
      try (OutputStream fos = new BufferedOutputStream(new FileOutputStream(new File ( TEMP, name ))))
      {
         fos.write( privKey.getEncoded() );
      }
   }

   public static final long MILLIS_IN_YEAR = 1000L * 60 * 60 * 24 * 365;
   private static File TEMP = new File( System.getProperty( "java.io.tmpdir" ) );

}
