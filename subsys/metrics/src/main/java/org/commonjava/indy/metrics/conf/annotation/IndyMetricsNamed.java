package org.commonjava.indy.metrics.conf.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Created by xiabai on 3/30/17.
 */
@Target( { ElementType.FIELD} )
@Retention( RUNTIME )
public @interface IndyMetricsNamed
{
    String value() default "";
}
