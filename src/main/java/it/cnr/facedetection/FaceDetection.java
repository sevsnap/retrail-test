package it.cnr.facedetection;

import static org.bytedeco.javacpp.helper.opencv_objdetect.cvHaarDetectObjects;
import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_highgui.*;
import static org.bytedeco.javacpp.opencv_objdetect.*;

import java.util.ArrayList;
import java.util.List;

import org.bytedeco.javacpp.opencv_core.CvMemStorage;
import org.bytedeco.javacpp.opencv_core.CvRect;
import org.bytedeco.javacpp.opencv_core.CvScalar;
import org.bytedeco.javacpp.opencv_core.CvSeq;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_objdetect.CvHaarClassifierCascade;

public class FaceDetection{
	
	
	public static final String XML_FILE = 
			"resources/haarcascade_frontalface_default.xml";
	
	static CvHaarClassifierCascade cascade = new CvHaarClassifierCascade(cvLoad(XML_FILE));
	
	public static void main(String[] args){
		
		//Load image
		IplImage img = cvLoadImage("resources/prova.jpg");		
		detect(img);		
	}	
	
	//Detect for face using classifier XML file 
	public static ArrayList<IplImage> detect(IplImage src){
		
		//Define classifier 
		CvHaarClassifierCascade cascade = new CvHaarClassifierCascade(cvLoad(XML_FILE));
		ArrayList<IplImage> faces = new ArrayList<IplImage>();
		
		CvMemStorage storage = CvMemStorage.create();
		
		IplImage srcopy = src.clone();
		
		CvSeq sign = cvHaarDetectObjects(
				src,
				cascade,
				storage,
				1.5,
				3,
				CV_HAAR_DO_CANNY_PRUNING);
		
		cvClearMemStorage(storage);
		
		int total_Faces = sign.total();
		
		//Draw rectangles around detected objects
		for(int i = 0; i < total_Faces; i++){
			CvRect r = new CvRect(cvGetSeqElem(sign, i));
			
			cvSetImageROI(srcopy, r);
			IplImage cropped = cvCreateImage(cvGetSize(srcopy), srcopy.depth(), srcopy.nChannels());
			cvCopy(srcopy, cropped);

			faces.add(cropped);
//			cvSaveImage("face_"+i+".jpg", cropped);
			
			cvRectangle (
					src,
					cvPoint(r.x(), r.y()),
					cvPoint(r.width() + r.x(), r.height() + r.y()),
					CvScalar.RED,
					2,
					CV_AA,
					0);
			
		}
		
		//Display result
//		cvShowImage("Result", src);
//		cvWaitKey(0);
		
		return faces;
		
	}
	public static ArrayList<CvRect> detectFaces(IplImage grabbedImage){
		
//		CvHaarClassifierCascade cascade = new CvHaarClassifierCascade(cvLoad(XML_FILE));
		CvMemStorage storage = CvMemStorage.create();
		
		ArrayList<CvRect> result = new ArrayList<CvRect>();
		
		if(grabbedImage == null) System.out.println("grabbedimagenull");
		if(grabbedImage == null) System.out.println("cascadenull");
		if(grabbedImage == null) System.out.println("storagenull");
		
		CvSeq sign = cvHaarDetectObjects(
				grabbedImage,
				cascade,
				storage,
				1.5,
				3,
				CV_HAAR_DO_CANNY_PRUNING);
		
		for(int i = 0; i < sign.total(); i++){
			CvRect r1 = new CvRect(cvGetSeqElem(sign, i));
			boolean isInside = false;
			for(int j = i+1; j < sign.total(); j++){
				CvRect r2 = new CvRect(cvGetSeqElem(sign, j));
				if(isInside(r1, r2)){
					isInside = true;
					break;
				}
			}
			if(!isInside)result.add(r1);
		}
		
		cvClearMemStorage(storage);
		return result;
		
//		return sign;
		
	}
	
	public static ArrayList<IplImage> listDetectedFaces(IplImage grabbedImage, ArrayList<CvRect> sign){
				
		ArrayList<IplImage> faces = new ArrayList<IplImage>();
		IplImage copy = grabbedImage.clone();
		  	
		for(int i = 0; i < sign.size(); i++){
			CvRect r = new CvRect(sign.get(i));
			
			cvSetImageROI(copy, r);
			IplImage cropped = cvCreateImage(cvGetSize(copy), copy.depth(), copy.nChannels());
			cvCopy(copy, cropped);
			faces.add(cropped);
			
		}
        
        return faces;
		
	}
	public static void drawDetectedFaces(IplImage grabbedImage, ArrayList<CvRect> sign){
				
    		for(int i = 0; i < sign.size(); i++){
    			CvRect r = new CvRect(sign.get(i));
    			
    			
    			cvRectangle (
    					grabbedImage,
    					cvPoint(r.x(), r.y()),
    					cvPoint(r.width() + r.x(), r.height() + r.y()),
    					CvScalar.RED,
    					2,
    					CV_AA,
    					0);
    			
        }
		
	}
	
	static boolean isInside(CvRect r1, CvRect r2){
		
		if(r1.x() > r2.x() && r1.y() > r2.y() && r1.x() + r1.width() < r2.x() + r2.width() && r1.y() + r1.height() < r2.y() + r2.height()){
			return true;
		}
		return false;
	}
	
	
}