package org.commonjava.aprox.bind.vertx.ui;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.inject.Qualifier;

@Qualifier
@Retention( RetentionPolicy.RUNTIME )
@Target( { METHOD, FIELD, PARAMETER, TYPE } )
public @interface UIApp
{

}
