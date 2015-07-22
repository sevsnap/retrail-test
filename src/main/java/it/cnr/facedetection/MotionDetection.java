package it.cnr.facedetection;

import it.cnr.iit.retrail.commons.impl.PepSession;
import it.cnr.iit.retrail.demo.UsageController;
import it.cnr.iit.retrail.demo.User;
import it.eng.msp.core.model.FaceMatch2;
import it.eng.msp.core.utils.FileUtils;
import it.eng.msp.web.clients.ImageMatchingClient;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bytedeco.javacpp.opencv_core.CvRect;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_objdetect.CvHaarClassifierCascade;
import org.bytedeco.javacv.*;
import org.bytedeco.javacv.FrameGrabber.Exception;

import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_highgui.cvSaveImage;

public class MotionDetection {
	
	public static final String XML_FILE = 
			"resources/haarcascade_frontalface_default.xml";
	
    static URL url = null;
    static String userpass = "admin:lab182015";
    static String basicAuth = "Basic " + new String(Base64.getEncoder().encode(userpass.getBytes()));
    static BufferedImage grabbedImage;
	
	
    public static void main(String[] args) throws InterruptedException, java.lang.Exception {
    	
    	startDetection();
    }
    
    synchronized static void setGrabbedImage(BufferedImage bi){
    	grabbedImage = bi;
    }
    
    synchronized static BufferedImage getGrabbedImage(){
    	return grabbedImage;
    }
    
    public static void startDetection() throws InterruptedException, java.lang.Exception{
    	
    	CvHaarClassifierCascade cascade = new CvHaarClassifierCascade(cvLoad(XML_FILE));
        final IPCameraFrameGrabber grabber = new IPCameraFrameGrabber("http://146.48.99.179/video/mjpg.cgi", basicAuth);
      
		grabber.start();
		
		BufferedImage grabbedImage = grabber.grabBufferedImage();
		setGrabbedImage(grabbedImage);
		IplImage frame, frame2;
		
		final CanvasFrame canvasFrame = new CanvasFrame("Video Camera");
		canvasFrame.setCanvasSize(grabbedImage.getWidth(), grabbedImage.getHeight());
		
//	        CanvasFrame canvasFrame2 = new CanvasFrame("Privacy-aware");
//	        canvasFrame.setCanvasSize(grabbedImage.width(), grabbedImage.height());
		
		IplImage image = null;
		IplImage prevImage = null;
		IplImage diff = null;
		
		//thread che effettua il grab dell'immagine da ipcam
		new Thread(new Runnable(){

			@Override
			public void run() {
				// TODO Auto-generated method stub
				while(canvasFrame.isVisible()){
					try {
						setGrabbedImage(grabber.grabBufferedImage());
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			
		}).start();
		
		Thread t = new Thread(new Runnable(){

			@Override
			public void run() {
				// TODO Auto-generated method stub
				
				BufferedImage grabbedImage = null;
				while(canvasFrame.isVisible()){
					//						grabbedImage = grabber.grabBufferedImage();
					grabbedImage = getGrabbedImage();
					IplImage iplGrabbedImage = IplImage.createFrom(grabbedImage);
			    	ArrayList<CvRect> faces = FaceDetection.detectFaces(iplGrabbedImage);
		        	ArrayList<IplImage> face_images = FaceDetection.listDetectedFaces(iplGrabbedImage, faces);
			    	
			    	FaceDetection.drawDetectedFaces(iplGrabbedImage, faces);
			    	canvasFrame.showImage(iplGrabbedImage.getBufferedImage());
			    	
		        	for(IplImage face:face_images){
		        		cvSaveImage("face.png", face);
		        		byte[] sampleIMG = FileUtils.toByteArray(new File("face.png"));
		    			FaceMatch2 current_face = new FaceMatch2();
		    			current_face.setSampleIMG(sampleIMG);
		    			ImageMatchingClient c = new ImageMatchingClient("localhost","8080");
                                        String verifyFaces = c.verifyFacesWithProcessing(current_face);
                                        System.out.println(verifyFaces);
                                                
                                        String name = "Francesco";
//                                        if(verifyFaces.split(" ").length == 3) name = verifyFaces.split(" ")[2];
                                        
                                        if(name != null){
                                            try {
                                                User user = User.getInstance(name);
                                                try {
                                                    PepSession pepSession = UsageController.getInstance().getSession(name);
                                                    if(pepSession == null){
                                                        UsageController.getInstance().getMainViewController().fireMouseEvent(name, true);
//                                                        boolean isInFrontOfTheDoor = user.goToDoor();
//                                                        if(isInFrontOfTheDoor){
//                                                            
//                                                            System.out.println("L'utente "+ name +" è alla porta");
//                                                        }
//                                                        else System.out.println("Try access fallita");
                                                    }
                                                    
                                                    pepSession = UsageController.getInstance().getSession(name);
                                                    
                                                    if(pepSession != null && pepSession.getStateName().equals("TRY")) {
                                                        UsageController.getInstance().getMainViewController().fireMouseEvent(name, true);
                                                        
//                                                        boolean entered = user.enterRoom();
//                                                        if(entered){
//                                                            
//                                                            System.out.println("L'utente "+name +" è entrato");
//                                                            pepSession.getLocalInfo().put("timestamp", new Timestamp(new java.util.Date().getTime()));
//                                                        }
//                                                        else System.out.println("Start access fallita");
                                                    }
                                                    
                                                    pepSession = UsageController.getInstance().getSession(name);
                                                    
                                                    if(pepSession != null && (pepSession.getStateName().equals("ONGOING") || pepSession.getStateName().equals("REVOKED"))){
                                                        pepSession.getLocalInfo().put("timestamp", new Timestamp(new java.util.Date().getTime()));
                                                        System.out.println("L'utente "+name +" è già dentro");
                                                    }
                                                } catch (java.lang.Exception ex) {
                                                    Logger.getLogger(MotionDetection.class.getName()).log(Level.SEVERE, null, ex);	       			  
                                                }
                                            } catch (java.lang.Exception ex) {
                                                Logger.getLogger(MotionDetection.class.getName()).log(Level.SEVERE, null, ex);
                                            }
                                        }
                                    }
				}
				try {
					grabber.stop();
					canvasFrame.dispose();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
			
		});
		
		t.start();
		
//	        CvMemStorage storage = CvMemStorage.create();
//	        
//			while(canvasFrame.isVisible() && (grabbedImage = grabber.grabBufferedImage()) != null){
//				cvClearMemStorage(storage);
//				IplImage iplGrabbedImage = IplImage.createFrom(grabbedImage);
//				frame = iplGrabbedImage.clone();
//				frame2 = iplGrabbedImage.clone();
//
//				cvSmooth(frame, frame, CV_GAUSSIAN, 9, 9, 2, 2);
//	        	
//	            if (image == null) {
//	                image = IplImage.create(frame.width(), frame.height(), IPL_DEPTH_8U, 1);
//	                cvCvtColor(frame, image, CV_RGB2GRAY);
//	            } else {
//	                prevImage = IplImage.create(frame.width(), frame.height(), IPL_DEPTH_8U, 1);
//	                prevImage = image;
//	                image = IplImage.create(frame.width(), frame.height(), IPL_DEPTH_8U, 1);
//	                cvCvtColor(frame, image, CV_RGB2GRAY);
//	            }
//	            
//	            if (diff == null) {
//	                diff = IplImage.create(frame.width(), frame.height(), IPL_DEPTH_8U, 1);
//	            }
//	            
//	            if (prevImage != null) {
//	                // perform ABS difference
//	                cvAbsDiff(image, prevImage, diff);
//	                // do some threshold for wipe away useless details
//	                cvThreshold(diff, diff, 64, 255, CV_THRESH_BINARY);
//
//	                //canvasFrame.showImage(diff);
//	                
//	                if(cvSum(new CvArr(diff)).blue() == 0){
//	                	
//	    	        	ArrayList<CvRect> faces = FaceDetection.detectFaces(iplGrabbedImage);
//	    	        	ArrayList<IplImage> face_images = FaceDetection.listDetectedFaces(iplGrabbedImage, faces);
//	    	        	
//	    	        	//System.out.println("Detected faces: "+face_images.size());
//	    	        	FaceDetection.drawDetectedFaces(iplGrabbedImage, faces);
//	    	        	//canvasFrame.showImage(grabbedImage);
//	    	        	
//	    	        	int index = 0;
//	    	        	for(IplImage face:face_images){
//	    	        		cvSaveImage("face.png", face);
//	    	        		byte[] sampleIMG = FileUtils.toByteArray(new File("face.png"));
//	    	    			FaceMatch2 current_face = new FaceMatch2();
//	    	    			current_face.setSampleIMG(sampleIMG);
//	    	    			ImageMatchingClient c = new ImageMatchingClient("localhost","8080");
//	    					String verifyFaces = c.verifyFacesWithProcessing(current_face);
////	    					System.out.println(verifyFaces);
////	    					if(verifyFaces.split(" ")[0].equals("User")){
////	    		    			cvRectangle (
////	    		    					frame2,
////	    		    					cvPoint(faces.get(index).x(), faces.get(index).y()),
////	    		    					cvPoint(faces.get(index).width() + faces.get(index).x(), faces.get(index).height() + faces.get(index).y()),
////	    		    					CvScalar.BLACK,
////	    		    					-2,
////	    		    					CV_AA,
////	    		    					0);
////	    					}
////	    	        		index++;
//	    	        	}
////	    	        	canvasFrame2.showImage(frame2);
//	    	        	
//	            }
//			}
//			
//			}
//	        grabber.stop();
//	        canvasFrame.dispose();
//		t.stop();
    }
        
	        
}
