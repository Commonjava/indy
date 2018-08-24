package org.commonjava.indy.boot.jaxrs;

import org.commonjava.indy.boot.BootOptions;
import org.xnio.XnioWorker;

public interface XnioService
{
    void start( XnioWorker worker, BootOptions bootOptions );
}
