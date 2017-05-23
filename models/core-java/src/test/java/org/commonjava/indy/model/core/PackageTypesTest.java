package org.commonjava.indy.model.core;

import org.junit.Test;

import java.util.Set;

import static org.commonjava.indy.model.core.GenericPackageTypeDescriptor.GENERIC_PKG_KEY;
import static org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor.MAVEN_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created by jdcasey on 5/12/17.
 */
public class PackageTypesTest
{
    @Test
    public void loadMavenAndGenericPackageTypes()
    {
        Set<String> typeStrings = PackageTypes.getPackageTypes();
        assertThat( typeStrings.contains( MAVEN_PKG_KEY ), equalTo( true ) );
        assertThat( typeStrings.contains( GENERIC_PKG_KEY ), equalTo( true ) );
    }
}
