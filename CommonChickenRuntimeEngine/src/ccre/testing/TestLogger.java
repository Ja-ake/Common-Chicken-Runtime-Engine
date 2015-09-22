package ccre.testing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestLogger {
    public static void main(String[] args) {
    	final Logger l = LoggerFactory.getLogger(TestLogger.class);
    	final String f = "I said {}";
    	final String m = "I should go to the store!";
    	final Exception e = new Exception("I haven't gone to the store yet!");
        l.debug(m);
        l.debug(f, m);
        l.debug(m, e);
        
        l.error(m);
        l.error(f, m);
        l.error(m, e);
        
        l.trace(m);
        l.trace(f, m);
        l.trace(m, e);
        
        l.info(m);
        l.info(f, m);
        l.info(m, e);
        
        l.warn(m);
        l.warn(f, m);
        l.warn(m, e);
    }
}
