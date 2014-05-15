import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.junit.Test;

public class MediaTypeTest
{

    @Test
    public void toStringWithTwoParams()
    {
        final Map<String, String> map = new HashMap<String, String>();
        map.put( "p1", "one" );
        map.put( "p2", "two" );

        final MediaType type = new MediaType( "foo", "bar", map );

        final String typeStr = type.toString();
        System.out.println( typeStr );
    }

    @Test
    public void parseWithApiVersion()
    {
        final MediaType type = MediaType.valueOf( "foo/bar;api=1.0" );

        assertThat( type.getParameters()
                        .get( "api" ), equalTo( "1.0" ) );

        System.out.println( type );
    }

}
