package org.commonjava.indy.core.ctl;

import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.dto.EndpointView;
import org.commonjava.indy.subsys.template.ScriptEngine;

/**
 * Responsible for creating new {@link RemoteRepository} instances created as a result of replicating another Indy instance.
 * This interface will be implemented by a Groovy script, and accessed by way of the
 * {@link org.commonjava.indy.subsys.template.ScriptEngine#parseStandardScriptInstance(ScriptEngine.StandardScriptType, String, Class)} method.
 *
 * Created by jdcasey on 8/17/16.
 */
public interface ReplicationRepositoryCreator
{
    RemoteRepository createRemoteRepository( String name, EndpointView view );
}
