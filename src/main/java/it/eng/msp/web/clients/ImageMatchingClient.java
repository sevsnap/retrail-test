package it.eng.msp.web.clients;

import static org.bytedeco.javacpp.opencv_highgui.cvSaveImage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import it.cnr.iit.detection.DetectionUtils;
import it.cnr.iit.detection.DetectionUtils.Haarcascade;
import it.cnr.iit.detection.FaceFinder;
import it.eng.msp.core.model.FaceMatch;
import it.eng.msp.core.model.FaceMatch2;
import it.eng.msp.core.utils.FileUtils;
import it.eng.msp.core.utils.ParamUtils;
import it.eng.msp.core.utils.RESTClientUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bytedeco.javacpp.opencv_core.CvRect;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacv.CanvasFrame;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.sun.jersey.api.client.ClientResponse;

public class ImageMatchingClient {
	
	private static Log log = LogFactory.getLog(ImageMatchingClient.class);
	
	private String endpoint = "http://localhost:8080/ImageMatching/rest/processing/authentication";
	
	public ImageMatchingClient() {
	}

	public ImageMatchingClient(String endpoint) {
		this.endpoint = endpoint;
	}
	
	public ImageMatchingClient(String host, String port) {
		endpoint = endpoint.replace("localhost", host);
		endpoint = endpoint.replace("8080", port);
	}
	
	public String verifyFaces(FaceMatch2 face) {
		try {
			ClientResponse response = RESTClientUtils.getInstance(ParamUtils.addPathSegment(
					endpoint, "verifyFaces")).postElement(face);
			
			JSONObject obj = new JSONObject(response.getEntity(String.class));
			log.debug("completed: " + obj.getBoolean("completed"));
			log.debug("success: " + obj.getBoolean("success"));
			log.debug("user: " + obj.getString("user"));
			
			boolean result = obj.getBoolean("completed") && obj.getBoolean("success");
			
			if(result) return "User identified: "+ obj.getString("user");
			else return "Unidentified user";
			
		} catch (JSONException e) {
			log.error(e);
		}
		
		return null;
	}
	
	public String verifyFacesWithProcessing(FaceMatch2 face) {
		try {
			
			ClientResponse response = RESTClientUtils.getInstance(ParamUtils.addPathSegment(
					endpoint, "verify1FaceWithProcessing")).postElement(face);
			
			JSONObject obj = new JSONObject(response.getEntity(String.class));

			log.debug("completed: " + obj.getBoolean("completed"));
			log.debug("success: " + obj.getBoolean("success"));
			log.debug("user: " + obj.getString("user"));
						
			boolean result = obj.getBoolean("completed") && obj.getBoolean("success");
			
			if(result) return "User identified: "+ obj.getString("user");
			else return "Unidentified user";
			
		} catch (JSONException e) {
			log.error(e);
		}
		
		return null;
	}
	
	public String verifyFacesWithProcessing(FaceMatch face) {
		try {
			
			ClientResponse response = RESTClientUtils.getInstance(ParamUtils.addPathSegment(
					endpoint, "verifyFacesWithProcessing")).postElement(face);
			
			JSONObject obj = new JSONObject(response.getEntity(String.class));

			log.debug("completed: " + obj.getBoolean("completed"));
			log.debug("success: " + obj.getBoolean("success"));
			log.debug("user: " + obj.getString("user"));
						
			boolean result = obj.getBoolean("completed") && obj.getBoolean("success");
			
			if(result) return "User identified: "+ obj.getString("user");
			else return "Unidentified user";
			
		} catch (JSONException e) {
			log.error(e);
		}
		
		return null;
	}
	
	public String verifyFacesWithEigenFaces(FaceMatch2 face) throws IOException {
		try {
			IplImage grabbedImage = IplImage.createFrom(ImageIO.read(new ByteArrayInputStream(face.getSampleIMG())));
			
        	ArrayList <CvRect>faces = DetectionUtils.detect(grabbedImage, Haarcascade.FACES);
        	ArrayList <CvRect>eyes = DetectionUtils.detect(grabbedImage, Haarcascade.EYES);
        	
        	DetectionUtils.removeInvalidValues(faces, eyes);
        	
        	if(eyes.size() != 2) return "No faces detected";
        	
        	IplImage faceImage = FaceFinder.convertToEigenFaceFormat(grabbedImage, eyes);
        	cvSaveImage(FileUtils.getSystemTmpDir()+"tmp.png", faceImage);
        	face.setSampleIMG(FileUtils.toByteArray(FileUtils.getSystemTmpDir()+"tmp.png"));
			
			ClientResponse response = RESTClientUtils.getInstance(ParamUtils.addPathSegment(
					endpoint, "verify1FaceWithEigenFaces")).postElement(face);
			
			
			JSONObject obj = new JSONObject(response.getEntity(String.class));

			log.debug("completed: " + obj.getBoolean("completed"));
			log.debug("success: " + obj.getBoolean("success"));
			log.debug("user: " + obj.getString("user"));
						
			boolean result = obj.getBoolean("completed") && obj.getBoolean("success");
			
			if(result) return "User identified: "+ obj.getString("user");
			else return "Unidentified user";
			
		} catch (JSONException e) {
			log.error(e);
		}
		
		return null;
	}
	
	public String verifyFacesWithEigenFaces(FaceMatch face) throws IOException {
		try {
			
			ClientResponse response = RESTClientUtils.getInstance(ParamUtils.addPathSegment(
					endpoint, "verifyFacesWithEigenFaces")).postElement(face);
			
			
			JSONObject obj = new JSONObject(response.getEntity(String.class));

			log.debug("completed: " + obj.getBoolean("completed"));
			log.debug("success: " + obj.getBoolean("success"));
			log.debug("user: " + obj.getString("user"));
						
			boolean result = obj.getBoolean("completed") && obj.getBoolean("success");
			
			if(result) return "User identified: "+ obj.getString("user");
			else return "Unidentified user";
			
		} catch (JSONException e) {
			log.error(e);
		}
		
		return null;
	}

}