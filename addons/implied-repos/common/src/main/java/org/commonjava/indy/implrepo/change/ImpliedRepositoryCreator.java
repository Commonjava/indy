package org.commonjava.indy.implrepo.change;

import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.subsys.template.ScriptEngine;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.galley.maven.model.view.RepositoryView;
import org.slf4j.Logger;

/**
 * Responsible for creating new {@link RemoteRepository} instances for use with repository declarations detected in
 * Maven POM files that have been stored in the system, whether via upload (hosted repo) or via download (remote repo).
 * This interface will be implemented by a Groovy script, and accessed by way of the
 * {@link org.commonjava.indy.subsys.template.ScriptEngine#parseStandardScriptInstance(ScriptEngine.StandardScriptType, String, Class)} method.
 *
 * Created by jdcasey on 8/17/16.
 */
public interface ImpliedRepositoryCreator
{
    RemoteRepository createFrom( ProjectVersionRef implyingGAV, RepositoryView repositoryView, Logger logger );
}
