package org.commonjava.aprox.inject;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Stereotype;
import javax.inject.Named;
import javax.inject.Qualifier;

@Qualifier
@Stereotype
@Alternative
@Named
@Retention( RetentionPolicy.RUNTIME )
@Target( { METHOD, FIELD, PARAMETER, TYPE } )
public @interface Production
{

}
