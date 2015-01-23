package org.commonjava.aprox.promote.ftest;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import org.commonjava.aprox.client.core.AproxClientModule;
import org.commonjava.aprox.ftest.core.AbstractAproxFunctionalTest;
import org.commonjava.aprox.model.core.HostedRepository;
import org.commonjava.aprox.promote.client.AproxPromoteClientModule;
import org.commonjava.aprox.promote.model.PromoteRequest;
import org.commonjava.aprox.promote.model.PromoteResult;
import org.junit.Before;
import org.junit.Test;

public class PromotionManagerTest
    extends AbstractAproxFunctionalTest
{

    private final String first = "/first/path";

    private final String second = "/second/path";

    private HostedRepository source;

    private HostedRepository target;

    @Before
    public void setupRepos()
        throws Exception
    {
        source = new HostedRepository( "source" );
        client.stores()
              .create( source, HostedRepository.class );

        client.content()
              .store( source.getKey()
                            .getType(), source.getName(), first, new ByteArrayInputStream( "This is a test".getBytes() ) );
        client.content()
              .store( source.getKey()
                            .getType(), source.getName(), second,
                      new ByteArrayInputStream( "This is a test".getBytes() ) );

        target = new HostedRepository( "target" );
        client.stores()
              .create( target, HostedRepository.class );
    }

    @Test
    public void promoteAll_PushTwoArtifactsToHostedRepo_VerifyCopiedToOtherHostedRepo()
        throws Exception
    {
        final PromoteResult result = client.module( AproxPromoteClientModule.class )
                                           .promote( new PromoteRequest( source.getKey(), target.getKey() ) );

        assertThat( result.getRequest()
                          .getSource(), equalTo( source.getKey() ) );
        assertThat( result.getRequest()
                          .getTarget(), equalTo( target.getKey() ) );

        final Set<String> pending = result.getPendingPaths();
        assertThat( pending == null || pending.isEmpty(), equalTo( true ) );

        final Set<String> completed = result.getCompletedPaths();
        assertThat( completed, notNullValue() );
        assertThat( completed.size(), equalTo( 2 ) );

        assertThat( result.getError(), nullValue() );

        assertThat( client.content()
                          .exists( target.getKey()
                                         .getType(), target.getName(), first ), equalTo( true ) );
        assertThat( client.content()
                          .exists( target.getKey()
                                         .getType(), target.getName(), second ), equalTo( true ) );
    }

    @Test
    public void promoteAll_PushTwoArtifactsToHostedRepo_DryRun_VerifyPendingPathsPopulated()
        throws Exception
    {
        final PromoteResult result =
            client.module( AproxPromoteClientModule.class )
                  .promote( new PromoteRequest( source.getKey(), target.getKey() ).setDryRun( true ) );

        assertThat( result.getRequest()
                          .getSource(), equalTo( source.getKey() ) );
        assertThat( result.getRequest()
                          .getTarget(), equalTo( target.getKey() ) );

        final Set<String> completed = result.getCompletedPaths();
        assertThat( completed == null || completed.isEmpty(), equalTo( true ) );

        final Set<String> pending = result.getPendingPaths();
        assertThat( pending, notNullValue() );
        assertThat( pending.size(), equalTo( 2 ) );

        assertThat( result.getError(), nullValue() );

        assertThat( client.content()
                          .exists( target.getKey()
                                         .getType(), target.getName(), first ), equalTo( false ) );
        assertThat( client.content()
                          .exists( target.getKey()
                                         .getType(), target.getName(), second ), equalTo( false ) );
    }

    @Test
    public void promoteAll_PurgeSource_PushTwoArtifactsToHostedRepo_VerifyCopiedToOtherHostedRepo()
        throws Exception
    {
        final PromoteResult result =
            client.module( AproxPromoteClientModule.class )
                  .promote( new PromoteRequest( source.getKey(), target.getKey() ).setPurgeSource( true ) );

        assertThat( result.getRequest()
                          .getSource(), equalTo( source.getKey() ) );
        assertThat( result.getRequest()
                          .getTarget(), equalTo( target.getKey() ) );

        final Set<String> pending = result.getPendingPaths();
        assertThat( pending == null || pending.isEmpty(), equalTo( true ) );

        final Set<String> completed = result.getCompletedPaths();
        assertThat( completed, notNullValue() );
        assertThat( completed.size(), equalTo( 2 ) );

        assertThat( result.getError(), nullValue() );

        assertThat( client.content()
                          .exists( target.getKey()
                                         .getType(), target.getName(), first ), equalTo( true ) );
        assertThat( client.content()
                          .exists( target.getKey()
                                         .getType(), target.getName(), second ), equalTo( true ) );

        assertThat( client.content()
                          .exists( source.getKey()
                                         .getType(), source.getName(), first ), equalTo( false ) );
        assertThat( client.content()
                          .exists( source.getKey()
                                         .getType(), source.getName(), second ), equalTo( false ) );
    }

    @Test
    public void rollback_PushTwoArtifactsToHostedRepo_PromoteSuccessThenRollback()
        throws Exception
    {
        PromoteResult result = client.module( AproxPromoteClientModule.class )
                                     .promote( new PromoteRequest( source.getKey(), target.getKey() ) );

        assertThat( result.getRequest()
                          .getSource(), equalTo( source.getKey() ) );
        assertThat( result.getRequest()
                          .getTarget(), equalTo( target.getKey() ) );

        Set<String> pending = result.getPendingPaths();
        assertThat( pending == null || pending.isEmpty(), equalTo( true ) );

        Set<String> completed = result.getCompletedPaths();
        assertThat( completed, notNullValue() );
        assertThat( completed.size(), equalTo( 2 ) );

        assertThat( result.getError(), nullValue() );

        result = client.module( AproxPromoteClientModule.class )
                       .rollback( result );

        assertThat( result.getRequest()
                          .getSource(), equalTo( source.getKey() ) );
        assertThat( result.getRequest()
                          .getTarget(), equalTo( target.getKey() ) );

        completed = result.getCompletedPaths();
        assertThat( completed == null || completed.isEmpty(), equalTo( true ) );

        pending = result.getPendingPaths();
        assertThat( pending, notNullValue() );
        assertThat( pending.size(), equalTo( 2 ) );

        assertThat( result.getError(), nullValue() );

        assertThat( client.content()
                          .exists( target.getKey()
                                         .getType(), target.getName(), first ), equalTo( false ) );
        assertThat( client.content()
                          .exists( target.getKey()
                                         .getType(), target.getName(), second ), equalTo( false ) );
    }

    @Test
    public void rollback_PurgeSource_PushTwoArtifactsToHostedRepo_PromoteSuccessThenRollback_VerifyContentInSource()
        throws Exception
    {
        PromoteResult result =
            client.module( AproxPromoteClientModule.class )
                  .promote( new PromoteRequest( source.getKey(), target.getKey() ).setPurgeSource( true ) );

        assertThat( result.getRequest()
                          .getSource(), equalTo( source.getKey() ) );
        assertThat( result.getRequest()
                          .getTarget(), equalTo( target.getKey() ) );

        Set<String> pending = result.getPendingPaths();
        assertThat( pending == null || pending.isEmpty(), equalTo( true ) );

        Set<String> completed = result.getCompletedPaths();
        assertThat( completed, notNullValue() );
        assertThat( completed.size(), equalTo( 2 ) );

        assertThat( result.getError(), nullValue() );

        result = client.module( AproxPromoteClientModule.class )
                       .rollback( result );

        assertThat( result.getRequest()
                          .getSource(), equalTo( source.getKey() ) );
        assertThat( result.getRequest()
                          .getTarget(), equalTo( target.getKey() ) );

        completed = result.getCompletedPaths();
        assertThat( completed == null || completed.isEmpty(), equalTo( true ) );

        pending = result.getPendingPaths();
        assertThat( pending, notNullValue() );
        assertThat( pending.size(), equalTo( 2 ) );

        assertThat( result.getError(), nullValue() );

        assertThat( client.content()
                          .exists( target.getKey()
                                         .getType(), target.getName(), first ), equalTo( false ) );
        assertThat( client.content()
                          .exists( target.getKey()
                                         .getType(), target.getName(), second ), equalTo( false ) );

        assertThat( client.content()
                          .exists( source.getKey()
                                         .getType(), source.getName(), first ), equalTo( true ) );
        assertThat( client.content()
                          .exists( source.getKey()
                                         .getType(), source.getName(), second ), equalTo( true ) );
    }

    @Override
    protected Collection<AproxClientModule> getAdditionalClientModules()
    {
        return Arrays.<AproxClientModule> asList( new AproxPromoteClientModule() );
    }
}
