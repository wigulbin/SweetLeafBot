import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class CommonTest {

    @Test
    void demoTest(){
        assertTrue(true);
    }
    @Test
    void envTest(){
        assertTrue(!System.getenv("token").isEmpty());
    }
}
