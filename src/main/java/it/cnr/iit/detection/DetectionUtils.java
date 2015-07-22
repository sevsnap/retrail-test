package it.cnr.iit.detection;

import static org.bytedeco.javacpp.helper.opencv_objdetect.cvHaarDetectObjects;
import static org.bytedeco.javacpp.opencv_core.CV_AA;
import static org.bytedeco.javacpp.opencv_core.cvGetSeqElem;
import static org.bytedeco.javacpp.opencv_core.cvLoad;
import static org.bytedeco.javacpp.opencv_core.cvPoint;
import static org.bytedeco.javacpp.opencv_core.cvRectangle;
import static org.bytedeco.javacpp.opencv_objdetect.CV_HAAR_DO_CANNY_PRUNING;
import it.eng.msp.core.utils.ResourceUtils;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;

import org.bytedeco.javacpp.opencv_core.*;
import org.bytedeco.javacpp.opencv_objdetect.CvHaarClassifierCascade;


public class DetectionUtils {
	
	private static CvMemStorage storage = CvMemStorage.create();
	private static final CvHaarClassifierCascade cascade_eyes = new CvHaarClassifierCascade(cvLoad("resources/haarcascade_eye.xml"));
	private static final CvHaarClassifierCascade cascade_faces = new CvHaarClassifierCascade(cvLoad("resources/haarcascade_frontalface_default.xml"));
	
//	private static final CvHaarClassifierCascade cascade_eyes = new CvHaarClassifierCascade(cvLoad(DetectionUtils.class.getResource("resources/haarcascade_eye.xml").getPath()));
//	private static final CvHaarClassifierCascade cascade_faces = new CvHaarClassifierCascade(cvLoad(DetectionUtils.class.getResource("resources/haarcascade_frontalface_default.xml").getPath()));
	
	public enum Haarcascade{
		EYES, FACES
	}
	
	public static ArrayList<CvRect> detect(IplImage image, Haarcascade haarcascade){
		ArrayList<CvRect> result = new ArrayList<CvRect>();
		
		CvHaarClassifierCascade cascade = null;

		if(haarcascade == Haarcascade.FACES) cascade = cascade_faces;
		else if (haarcascade == Haarcascade.EYES) cascade = cascade_eyes;
		
//		File file = new File(DetectionUtils.class.getResource("resources/haarcascade_frontalface_default.xml").getPath());
//		System.out.println("exists "+file.exists());
		
		CvSeq sign = cvHaarDetectObjects(
				image,
				cascade,
				storage,
				1.5,
				3,
				CV_HAAR_DO_CANNY_PRUNING);
		
		for(int i = 0; i < sign.total(); i++){
			boolean inside = false;
			for(int j = i; j < sign.total(); j++){
				if(isInside(new CvRect(cvGetSeqElem(sign, i)),new CvRect(cvGetSeqElem(sign, j)))){
					inside = true;
					break;
				}
			}
			if(!inside)result.add(new CvRect(cvGetSeqElem(sign, i)));
			
		}

		return result;
		
	}
	
	public static void removeInvalidValues(ArrayList<CvRect> faces, ArrayList<CvRect> eyes){
		removeDuplicate(faces);
		removeDuplicate(eyes);
		
		for(int i = 0; i < eyes.size(); i++){
			boolean inside = false;
			for(CvRect face:faces){
				if(isInside(eyes.get(i),face)){
					inside = true;
					break;
				}
			}
			if(!inside){
				eyes.remove(i);
				i--;
			}
		}
	}
	
	
	public static ArrayList<CvRect> detect(BufferedImage image, Haarcascade haarcascade){
		
		return detect(IplImage.createFrom(image), haarcascade);
		
	}
	
	private static boolean isInside(CvRect r1, CvRect r2){
		
		if(r1.x() > r2.x() && r1.y() > r2.y() && r1.x() + r1.width() < r2.x() + r2.width() && r1.y() + r1.height() < r2.y() + r2.height()){
			return true;
		}
		return false;
	}
	
	private static void removeDuplicate(ArrayList<CvRect> set){
		
		for(int i = 0; i < set.size(); i++){
			for(int j = i; j < set.size(); j++){
				if(isInside(set.get(i),set.get(j))){
					set.remove(i);
					i--;
					break;
				}
			}
		}
		
	}

}
