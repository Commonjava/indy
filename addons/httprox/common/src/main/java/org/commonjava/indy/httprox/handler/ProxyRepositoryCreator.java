package org.commonjava.indy.httprox.handler;

import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.subsys.http.util.UserPass;
import org.commonjava.indy.subsys.template.ScriptEngine;
import org.commonjava.indy.util.UrlInfo;
import org.slf4j.Logger;

/**
 * Responsible for creating new {@link RemoteRepository} instances for use with the HTTProx proxy add-on.
 * This interface will be implemented by a Groovy script, and accessed by way of the
 * {@link org.commonjava.indy.subsys.template.ScriptEngine#parseStandardScriptInstance(ScriptEngine.StandardScriptType, String, Class)} method.
 *
 * Created by jdcasey on 8/17/16.
 */
public interface ProxyRepositoryCreator
{
    RemoteRepository create( String name, String baseUrl, UrlInfo info, UserPass up, Logger logger );
}
