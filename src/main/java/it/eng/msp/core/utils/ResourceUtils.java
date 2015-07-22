package it.eng.msp.core.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

/**
 * Utility class to manage file facilities, mainly dealing with basic CRUD
 * operations on file system (such as copy, read, write).
 * 
 * @author Angelo Marguglio <br>
 *         Company: Engineering Ingegneria Informatica S.p.A. <br>
 *         E-mail: <a href="mailto:angelo.marguglio@eng.it">angelo.marguglio@eng.it</a>
 */
public class ResourceUtils {
	
	private static final String PROPERTY_FILE = "msp.properties";

	static Properties prop;
	
	static {
		try {
			init();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void init() throws IOException {
		InputStream propResource = 
			Thread.currentThread().getContextClassLoader()
				.getResourceAsStream(PROPERTY_FILE);
		
		prop = new Properties();
		if(propResource!=null)
			prop.load(propResource);
	}
	
	/**
	 * @return il valore di una property
	 */
	public static String readProperty(String propertyName){
		return prop.getProperty(propertyName);
	}
	
	/**
	 * @return il percorso ad un file contenuto nel classpath
	 */
	public static String readResource(String relativePath) {
		URL processURL = Thread.currentThread().getContextClassLoader().getResource(relativePath);
		System.out.println(processURL.getPath());
		return processURL.getPath();
	}

	/**
	 * @return il percorso ad un file contenuto nel classpath
	 */
	public static String readResourcePath(String key) {
		URL processURL = Thread.currentThread().getContextClassLoader().getResource(readProperty(key));
		return processURL.getPath();
	}
	
}