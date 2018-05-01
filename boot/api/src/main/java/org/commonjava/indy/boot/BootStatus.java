/**
 * Copyright (C) 2011-2018 Red Hat, Inc. (https://github.com/Commonjava/indy)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.indy.boot;

public class BootStatus
{

    private Throwable error;

    private int exit;

    public BootStatus()
    {
        this.exit = -1;
    }

    public BootStatus( final int exit, final Throwable error )
    {
        markFailed( exit, error );
    }

    public synchronized void markFailed( final int exit, final Throwable error )
    {
        this.exit = exit;
        this.error = error;
        notifyAll();
    }

    public boolean isSet()
    {
        return exit > -1;
    }

    public boolean isFailed()
    {
        return exit > 0;
    }

    public boolean isSuccess()
    {
        return exit == 0;
    }

    public Throwable getError()
    {
        return error;
    }

    public int getExitCode()
    {
        return exit;
    }

    public synchronized void markSuccess()
    {
        exit = 0;
        notifyAll();
    }

}
