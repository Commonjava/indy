package org.commonjava.indy.koji.content;

import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.subsys.template.ScriptEngine;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;

/**
 * Responsible for creating new {@link RemoteRepository} and {@link HostedRepository} instances used to download and
 * house artifacts for a given Koji build.
 *
 * This interface will be implemented by a Groovy script, and accessed by way of the
 * {@link org.commonjava.indy.subsys.template.ScriptEngine#parseStandardScriptInstance(ScriptEngine.StandardScriptType, String, Class)} method.
 *
 * Created by jdcasey on 8/17/16.
 */
public interface KojiRepositoryCreator
{
    RemoteRepository createRemoteRepository( String name, String url, Integer downloadTimeoutSeconds );

    HostedRepository createHostedRepository( String name, ArtifactRef artifactRef, String nvr );
}
