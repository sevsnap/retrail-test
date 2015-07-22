package it.eng.msp.core.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Angelo Marguglio <br>
 *         Company: Engineering Ingegneria Informatica S.p.A. <br>
 *         E-mail: <a href="mailto:angelo.marguglio@eng.it">angelo.marguglio@eng.it</a>
 *
 */
public class ParamUtils {
	
	public final static String WEBPATH_SEPARATOR = "/"; 
	
	public static Map<String, String> parse(String paramString) {
		Map<String, String> params = new HashMap<String, String>();
		String[] paramPairs = paramString.split("&");
		for (String param : paramPairs) {
			String[] key_value = param.split("=");
			params.put(key_value[0], key_value[1]);
		}
		return params;
	}
	
	public static String addPathSegment(String path, String segment) {
		if(path==null)
			path = "";
		
		if(segment==null)
			segment = "";
		
		if(path.endsWith(WEBPATH_SEPARATOR))
			return path + segment;
		else
			return path + WEBPATH_SEPARATOR + segment;
	}

}
