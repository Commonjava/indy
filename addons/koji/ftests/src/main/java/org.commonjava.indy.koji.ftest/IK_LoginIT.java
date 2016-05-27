package org.commonjava.indy.koji.ftest;

import org.junit.Test;

/**
 * This IT tests that Indy can boot normally with Koji support enabled. There are no assertions, because failure to
 * boot will cause a failure of this test in the setup phase.
 *
 * Created by jdcasey on 5/26/16.
 */
public class IK_LoginIT
    extends AbstractKojiIT
{
    @Test
    public void run()
    {
        // nop
    }
}
