package org.commonjava.indy.subsys.template.scripts;

import org.commonjava.indy.subsys.template.fixture.ScriptedThing;
import org.commonjava.indy.subsys.template.fixture.ScriptedThingOwner;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

public class TestScript implements ScriptedThingOwner
{
    @Inject
    private ScriptedThing thing;

    @Override
    public ScriptedThing getThing()
    {
        return thing;
    }
}