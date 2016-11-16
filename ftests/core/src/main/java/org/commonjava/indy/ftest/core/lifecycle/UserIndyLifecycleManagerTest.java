package org.commonjava.indy.ftest.core.lifecycle;

import org.commonjava.indy.ftest.core.AbstractIndyFunctionalTest;
import org.commonjava.indy.test.fixture.core.CoreServerFixture;
import org.junit.Test;

import java.io.IOException;

import static org.commonjava.indy.model.core.StoreType.hosted;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created by ruhan on 11/16/16.
 */
public class UserIndyLifecycleManagerTest
        extends AbstractIndyFunctionalTest
{
    @Override
    protected void initTestData( CoreServerFixture fixture )
            throws IOException
    {
        super.initTestData(fixture);
        writeDataFile("lifecycle/boot/boot01.groovy", readTestResource("lifecycle/boot01.groovy"));
        writeDataFile("lifecycle/boot/boot02.groovy", readTestResource("lifecycle/boot02.groovy")); // test priority
        writeDataFile("lifecycle/migrate/migrate01.groovy", readTestResource("lifecycle/migrate01.groovy"));
        writeDataFile("lifecycle/start/start01.groovy", readTestResource("lifecycle/start01.groovy"));
        writeDataFile("lifecycle/shutdown/shutdown01.groovy", readTestResource("lifecycle/shutdown01.groovy"));
    }

    @Test
    public void loadUserLifecycleActions()
            throws Exception
    {
        assertThat(client.content().exists( hosted, "test", "org/foo/foo.pom" ) , equalTo( true ) );
        assertThat(client.content().exists( hosted, "test", "org/bar/bar.pom" ) , equalTo( true ) );
    }

}