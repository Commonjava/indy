package org.commonjava.indy.pkg.npm.inject;

import javax.inject.Qualifier;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;

@Qualifier
@Target( { TYPE, FIELD, PARAMETER, METHOD } )
@Retention( RetentionPolicy.RUNTIME )
public @interface NPMContentHandler
{
}
