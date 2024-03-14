import org.example.Main;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class CommonTest {

    @Test
    void demoTest(){
        assertTrue(true);
    }

    @Test
    void testChannelId(){
        assertTrue(Main.INTRO_CHANNEL_ID.equals("1152046915731603487"));
    }
//    @Test
//    void envTest(){
//        assertTrue(!System.getenv("token").isEmpty());
//    }
}
