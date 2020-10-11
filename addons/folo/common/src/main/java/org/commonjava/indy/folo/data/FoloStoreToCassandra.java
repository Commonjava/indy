package org.commonjava.indy.folo.data;


import javax.inject.Qualifier;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Qualifier
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD,ElementType.TYPE})
@Retention( RetentionPolicy.RUNTIME)
public @interface FoloStoreToCassandra {
}
