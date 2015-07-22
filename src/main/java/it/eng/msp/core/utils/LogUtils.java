package it.eng.msp.core.utils;

import org.apache.commons.logging.Log;


/**
 * Utility class providing capabilities to log an error in a convenient way.
 * 
 * @author Angelo Marguglio <br>
 *         Company: Engineering Ingegneria Informatica S.p.A. <br>
 *         E-mail: <a href="mailto:angelo.marguglio@eng.it">angelo.marguglio@eng.it</a>
 * 
 */
public class LogUtils {

	/**
	 * Report the given error when possible.
	 * 
	 * @param log - the Log to be used
	 * @param e - the error to trace
	 */
	public static void printStackTrace(Log log, Throwable e) {
		if (log.isErrorEnabled())
			log.error(e.getClass().getSimpleName(), e);
	}

	/**
	 * @return A string containing the simple class name and the method name of
	 *         the invoker in the format "<class_name>.<method_name> INVOKED"
	 */
	public static String whereis() {
		try {
			return Thread.currentThread().getStackTrace()[2].getClassName()
					.substring(
							Thread.currentThread().getStackTrace()[2]
									.getClassName().lastIndexOf(".") + 1)
					+ "."
					+ Thread.currentThread().getStackTrace()[2].getMethodName()
					+ " INVOKED";
		} catch (Throwable e) {
			return "";
		}
	}

}
