
import org.commonjava.aprox.autoprox.data.*;

import java.net.MalformedURLException;

import org.commonjava.aprox.model.core.*;

class ComplexGroupsRule extends AbstractAutoProxRule
{
    boolean matches( String name ){
        name =~ /.+\+.+/
    }

    Group createGroup( String named )
    {
        String[] parts = named.split("\\+")
        
        Group g = null
        if ( parts.length > 1 ){
            g = new Group( named )
            parts.each{
              int idx = it.indexOf('_')
              
              String type = 'remote'
              String name = null
              if ( idx < 1 ){
                name = it
              }
              else{
                type = it.substring(0,idx)
                name = it.substring(idx+1)
              }
              
              g.addConstituent( new StoreKey( StoreType.get( type ), name ) );
            }
        }
        
        g
    }
}
