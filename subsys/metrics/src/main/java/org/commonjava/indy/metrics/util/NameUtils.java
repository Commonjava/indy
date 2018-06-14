package org.commonjava.indy.metrics.util;

import org.apache.commons.lang3.ClassUtils;

public class NameUtils
{
    private static final int DEFAULT_LEN = 40;

    public static String getAbbreviatedName( Class cls )
    {
        return ClassUtils.getAbbreviatedName( cls, DEFAULT_LEN );
    }
}
