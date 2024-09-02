package apkeep.utils;

import java.util.logging.Logger;


public class TestLogger {

	public TestLogger() {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args)  {
        Logger logger=Logger.getLogger(TestLogger.class.getName());
	    logger.info("Test logger");
    }
}