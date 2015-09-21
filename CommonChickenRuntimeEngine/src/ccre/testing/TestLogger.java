package ccre.testing;

import org.slf4j.LoggerFactory;

public class TestLogger {
    public static void main(String[] args) {
        LoggerFactory.getLogger(TestLogger.class).debug("Hi there debug", new NullPointerException("It works!"));
    }
}
