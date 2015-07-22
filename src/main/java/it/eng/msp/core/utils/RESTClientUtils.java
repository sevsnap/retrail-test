package it.eng.msp.core.utils;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.representation.Form;
import com.sun.jersey.multipart.FormDataMultiPart;

/**
 * @author Angelo Marguglio <br>
 *         Company: Engineering Ingegneria Informatica S.p.A. <br>
 *         E-mail: <a href="mailto:angelo.marguglio@eng.it">angelo.marguglio@eng.it</a>
 *
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class RESTClientUtils {
	
	private WebResource r = null;
	
	private RESTClientUtils(String endpoint) {
		r = Client.create().resource(endpoint);
	}
	
	private RESTClientUtils(String endpoint, String path) {
		r = Client.create().resource(endpoint).path(path);
	}

	public static RESTClientUtils getInstance(String endpoint) {
		return new RESTClientUtils(endpoint);
	}
	
	public static RESTClientUtils getInstance(String endpoint, String path) {
		return new RESTClientUtils(endpoint, path);
	}

	public String getQueryParams(MultivaluedMap queryParams) {
		return (String) getQueryParams(queryParams, String.class);
	}
	
	public Object getQueryParams(MultivaluedMap queryParams, Class type) {
		return r.queryParams(queryParams).get(type);
	}
	
	public ClientResponse putElement(Object elem) {
		return r.accept(MediaType.APPLICATION_XML).put(
				ClientResponse.class, elem);
	}
	
	public ClientResponse get() {
		return r.accept(MediaType.APPLICATION_XML).get(
				ClientResponse.class);
	}
	
	public ClientResponse post() {
		return r.accept(MediaType.APPLICATION_XML).post(
				ClientResponse.class);
	}
	
	public ClientResponse postElement(Object elem) {
		return r.accept(MediaType.APPLICATION_XML).post(
				ClientResponse.class, elem);
	}

	public String putQueryParams(MultivaluedMap queryParams) {
		ClientResponse response = r.queryParams(queryParams).put(ClientResponse.class);
		String entity = response.getEntity(String.class);
		return entity;
	}
	
	public ClientResponse postFormData(MultivaluedMap formData) {
		ClientResponse response = r.type(MediaType.APPLICATION_FORM_URLENCODED).post(
				ClientResponse.class, formData);

		return response;
	}

	public String postMultipartFormData(FormDataMultiPart formData) {
		ClientResponse response = r.type(MediaType.MULTIPART_FORM_DATA).post(
				ClientResponse.class, formData);
		String entity = response.getEntity(String.class);
		return entity;
	}
	
	public String postFormData(Form form) {
		r.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE);  
		ClientResponse response = r.post(ClientResponse.class, form);  
		String entity = response.getEntity(String.class);
		return entity;
	}

}
