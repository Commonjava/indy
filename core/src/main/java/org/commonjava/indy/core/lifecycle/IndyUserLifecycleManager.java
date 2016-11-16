package org.commonjava.indy.core.lifecycle;

import org.commonjava.indy.action.IndyLifecycleAction;
import org.commonjava.indy.action.UserLifecycleManager;
import org.commonjava.indy.subsys.template.ScriptEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Collection;
import java.util.HashSet;

import org.commonjava.indy.subsys.datafile.DataFileManager;
import org.commonjava.indy.subsys.datafile.DataFile;

/**
 * Created by ruhan on 11/16/16.
 */
@ApplicationScoped
public class IndyUserLifecycleManager
        implements UserLifecycleManager
{
    private static final String LIFECYCLE_DIR = "lifecycle";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private ScriptEngine scriptEngine;

    @Inject
    private DataFileManager dataFileManager;

    @Override
    public <T extends IndyLifecycleAction> Collection<T> getUserLifecycleActions(String lifecycleName, Class<T> type)
    {
        DataFile lifecycleDir = dataFileManager.getDataFile(LIFECYCLE_DIR, lifecycleName);

        Collection<T> set = new HashSet<T>();
        if ( lifecycleDir.exists() ) {
            final DataFile[] scripts = lifecycleDir.listFiles((pathname) ->
            {
                logger.info("Checking for user lifecycle action script in: {}", pathname);
                return pathname.getName().endsWith(".groovy");
            });

            for (final DataFile script : scripts)
            {
                logger.info("Reading user lifecycle action script from: {}", script);
                try
                {
                    String s = script.readString();
                    Object obj = scriptEngine.parseScriptInstance(s, type, true);
                    T action = type.cast(obj);
                    set.add(action);
                    logger.debug("Parsed: {}", obj.getClass().getName());
                }
                catch (final Exception e)
                {
                    logger.error(String.format("Cannot load user lifecycle action script from: %s. Reason: %s", script,
                            e.getMessage()), e);
                }
            }
        }
        return set;
    }
}
