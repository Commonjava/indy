package org.commonjava.indy.pathmap.migrate;

public interface Command
{
    void run(MigrateOptions options) throws MigrateException;
}
