package it.cnr.iit.detection;

import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.linalg.EigenvalueDecomposition;
import cern.jet.math.Functions;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import org.bytedeco.javacpp.opencv_core.CvRect;
import org.bytedeco.javacpp.opencv_core.IplImage;

public class FaceFinder
{
  private double[][] projectedFaces;
  private FaceDB faceDB;
  private String dbPath;
  private static final SysConfig sysConfig = SysConfig.getInstance();
  
  public FaceFinder()
  {

    this.dbPath = FaceFinder.sysConfig.getDBPath();
    processDB();
  }
  
  public String findMatchingImage(BufferedImage image)
  {
    DenseDoubleMatrix2D inputFace = Utils.normalize(doubleVectorToDenseMatrix(Utils.getImageDoublePixels(image)));
    DenseDoubleMatrix2D avgFace = doubleVectorToDenseMatrix(this.faceDB.getAverageFace());
    
    inputFace.assign(avgFace, Functions.minus);
    

    DenseDoubleMatrix2D projectedFace = projectToFacesSpace(inputFace);
    
    double[] distances = getDistances(projectedFace);
    
    int matchedImageIndex = getClosesedImageIndex(distances);
    System.out.println("Closest image distance: "+Math.sqrt(distances[matchedImageIndex]));
    if(Math.sqrt(distances[matchedImageIndex]) <= sysConfig.getThreshold())  return (String)this.faceDB.getImageFileNamesList().get(matchedImageIndex);
    else return null;
//    return Utils.loadImage((String)this.faceDB.getImageFileNamesList().get(matchedImageIndex));
   
  }
  
  private DenseDoubleMatrix2D doubleVectorToDenseMatrix(double[] vector)
  {
    double[][] inputFaceData = new double[1][];
    inputFaceData[0] = vector;
    DenseDoubleMatrix2D inputFace = new DenseDoubleMatrix2D(inputFaceData);
    return inputFace;
  }
  
  private int getClosesedImageIndex(double[] distances)
  {
    double minimumDistance = 1.7976931348623157E+308D;
    int index = 0;
    for (int i = 0; i < distances.length; i++) {
      if (distances[i] < minimumDistance)
      {
        minimumDistance = distances[i];
        index = i;
      }
    }
    return index;
  }
  
  private double[] getDistances(DenseDoubleMatrix2D projectedFace)
  {
    DenseDoubleMatrix2D tempFaces = new DenseDoubleMatrix2D(this.projectedFaces);
    double[] inputWtData = projectedFace.toArray()[0];
    tempFaces = Utils.subtractFromEachRow(tempFaces, inputWtData);
    tempFaces.assign(tempFaces, Functions.mult);
    double[][] temp = tempFaces.toArray();
    double[] distances = new double[temp.length];
    for (int i = 0; i < temp.length; i++)
    {
      double sum = 0.0D;
//      for (int j = 0; j < temp[0].length; j++) {
      for (int j = 0; j < sysConfig.getEigenVectorsNumber(); j++) {
        sum += temp[i][j];
      }
      distances[i] = sum;
    }
    return distances;
  }
  
  public void processDB()
  {
    List<String> imglist = FaceDB.parseDirectory(this.dbPath);
    this.faceDB = buildFaceDB(imglist);
    
    this.projectedFaces = projectToFacesSpace(new DenseDoubleMatrix2D(this.faceDB.getAdjustedFaces())).toArray();
  }
  
  private DenseDoubleMatrix2D projectToFacesSpace(DenseDoubleMatrix2D inputFace)
  {
    double[][] eigenFacesArray = this.faceDB.getEigenFaces();
    DenseDoubleMatrix2D eigenFacesMatrix = new DenseDoubleMatrix2D(eigenFacesArray);
    DenseDoubleMatrix2D eigenFacesMatrixTranspose = new DenseDoubleMatrix2D(eigenFacesMatrix.viewDice().toArray());
    DenseDoubleMatrix2D projectedFace = new DenseDoubleMatrix2D(inputFace.zMult(eigenFacesMatrixTranspose, null).toArray());
    return projectedFace;
  }
  
  public FaceDB buildFaceDB(List<String> filenames)
  {
    BufferedImage[] bufimgs = FaceDB.getImagesFromDB(filenames);
    
    DenseDoubleMatrix2D imagesData = Utils.normalize(Utils.imagesToDenseMatrix(bufimgs));
    double[] averageFace = Utils.getAverageImage(imagesData);
    imagesData = Utils.subtractFromEachRow(imagesData, averageFace);
    EigenvalueDecomposition egdecomp = getEigenvalueDecomposition(imagesData);
    double[] eigenValues = Utils.getDiagonal(egdecomp.getD().toArray());
    double[][] eigenVectors = egdecomp.getV().toArray();
    
    sortEigenVectors(eigenValues, eigenVectors);
    DenseDoubleMatrix2D eigenFaces = getNormalisedEigenFaces(imagesData, new DenseDoubleMatrix2D(eigenVectors));
    
    return new FaceDB(filenames, eigenFaces.toArray(), imagesData.toArray(), averageFace, eigenValues);
  }
  
  private EigenvalueDecomposition getEigenvalueDecomposition(DenseDoubleMatrix2D imagesData)
  {
    DenseDoubleMatrix2D imagesDataTranspose = new DenseDoubleMatrix2D(imagesData.viewDice().toArray());
    DenseDoubleMatrix2D covarianceMatrix = new DenseDoubleMatrix2D(imagesData.zMult(imagesDataTranspose, null).toArray());
    return new EigenvalueDecomposition(covarianceMatrix);
  }
  
  private DenseDoubleMatrix2D getNormalisedEigenFaces(DenseDoubleMatrix2D imagesData, DenseDoubleMatrix2D eigenVectors)
  {
    DenseDoubleMatrix2D eigenVectorsTranspose = new DenseDoubleMatrix2D(eigenVectors.viewDice().toArray());
    DenseDoubleMatrix2D eigenFaces = new DenseDoubleMatrix2D(eigenVectorsTranspose.zMult(imagesData, null).toArray());
    double[][] eigenFacesData = eigenFaces.toArray();
    for (int i = 0; i < eigenFacesData.length; i++)
    {
      double norm = Utils.norm(eigenFacesData[i]);
      for (int j = 0; j < eigenFacesData[i].length; j++)
      {
        double v = eigenFacesData[i][j];
        eigenFacesData[i][j] = (v / norm);
      }
    }
    return new DenseDoubleMatrix2D(eigenFacesData);
  }
  
  public void sortEigenVectors(double[] eigenValues, double[][] eigenVectors)
  {
    Hashtable<Double, double[]> table = new Hashtable();
    Double[] evals = new Double[eigenValues.length];
    getEigenValuesAsDouble(eigenValues, evals);
    fillHashtable(eigenValues, eigenVectors, table, evals);
    ArrayList<Double> keylist = sortKeysInReverse(table);
    updateEigenVectors(eigenVectors, table, evals, keylist);
    Double[] sortedkeys = new Double[keylist.size()];
    keylist.toArray(sortedkeys);
    
    updateEigenValues(eigenValues, sortedkeys);
  }
  
  private void getEigenValuesAsDouble(double[] eigenValue, Double[] evals)
  {
    for (int i = 0; i < eigenValue.length; i++) {
      evals[i] = new Double(eigenValue[i]);
    }
  }
  
  private ArrayList<Double> sortKeysInReverse(Hashtable<Double, double[]> table)
  {
    Enumeration<Double> keys = table.keys();
    ArrayList<Double> keylist = Collections.list(keys);
    Collections.sort(keylist, Collections.reverseOrder());
    return keylist;
  }
  
  private void updateEigenValues(double[] eigenValue, Double[] sortedKeys)
  {
    for (int i = 0; i < sortedKeys.length; i++)
    {
      Double dbl = sortedKeys[i];
      double dblval = dbl.doubleValue();
      eigenValue[i] = dblval;
    }
  }
  
  private void updateEigenVectors(double[][] eigenVector, Hashtable<Double, double[]> table, Double[] evals, ArrayList<Double> keylist)
  {
    for (int i = 0; i < evals.length; i++)
    {
      double[] ret = (double[])table.get(keylist.get(i));
      setColumn(eigenVector, ret, i);
    }
  }
  
  private void fillHashtable(double[] eigenValues, double[][] eigenVectors, Hashtable<Double, double[]> table, Double[] evals)
  {
    for (int i = 0; i < eigenValues.length; i++)
    {
      Double key = evals[i];
      double[] value = getColumn(eigenVectors, i);
      table.put(key, value);
    }
  }
  
  private double[] getColumn(double[][] mat, int j)
  {
    int m = mat.length;
    double[] res = new double[m];
    for (int i = 0; i < m; i++) {
      res[i] = mat[i][j];
    }
    return res;
  }
  
  private void setColumn(double[][] mat, double[] col, int c)
  {
    int len = col.length;
    for (int row = 0; row < len; row++) {
      mat[row][c] = col[row];
    }
  }
  
  /*---MY FUNCTIONS---*/

  
  public static IplImage convertToEigenFaceFormat(IplImage image, ArrayList<CvRect> eyes){
	  
	    BufferedImage before = image.getBufferedImage();
		
		Point pLeft = new Point(eyes.get(0).x() + eyes.get(0).width()/2, eyes.get(0).y() + eyes.get(0).height()/2);
		Point pRight = new Point(eyes.get(1).x() + eyes.get(1).width()/2, eyes.get(1).y() + eyes.get(1).height()/2);
		
        if (pLeft.x > pRight.x){
        pRight = pLeft;
        pLeft = new Point(eyes.get(1).x() + eyes.get(1).width()/2, eyes.get(1).y() + eyes.get(1).height()/2);
        }
        
        int eyeY = (pLeft.y + pRight.y) / 2;
        int eyeDistance = Math.abs(pRight.x - pLeft.x);
                
//	    float scaleFactorOrigX = (float)Visualizer.this.selectedImagePanel.getWidth() / before.getRaster().getWidth();
//	    float scaleFactorOrigY = (float)Visualizer.this.selectedImagePanel.getHeight() / before.getRaster().getHeight();
        float scaleFactorFinal = (float)sysConfig.getEyeDistance() / eyeDistance;
	    Dimension leftCorner = null;
	    Dimension rightCorner = null;
	      
	    int relativeEyeDistanceFromBorder = 6 * sysConfig.getEigenFaceWidth() / 25;
	    int relativeEyeDepthFromBorder = sysConfig.getEigenFaceHeight() / 6 + 20;
	    leftCorner = new Dimension((int)((pLeft.x - relativeEyeDistanceFromBorder / scaleFactorFinal) /*/ scaleFactorOrigX*/), (int)((eyeY - relativeEyeDepthFromBorder / scaleFactorFinal) /*/ scaleFactorOrigY*/));
	      
	
	    rightCorner = new Dimension((int)((pRight.x + relativeEyeDistanceFromBorder / scaleFactorFinal) /*/ scaleFactorOrigX*/), (int)((eyeY + 5 * relativeEyeDepthFromBorder / scaleFactorFinal) /*/ scaleFactorOrigY*/));
	    if (leftCorner.width < 0) {
	      leftCorner.width = 0;
	    }
	    if (leftCorner.height < 0) {
	      leftCorner.height = 0;
	    }
	    if (rightCorner.width > before.getRaster().getWidth()) {
	      rightCorner.width = before.getRaster().getWidth();
	    }
	    if (rightCorner.height > before.getRaster().getHeight()) {
	      rightCorner.height = before.getRaster().getHeight();
	    }
	      
	    BufferedImage after = before.getSubimage(leftCorner.width, leftCorner.height, rightCorner.width - leftCorner.width, rightCorner.height - leftCorner.height);
	
	    after = Utils.scaleToWindow(after, sysConfig.getEigenFaceWidth(), sysConfig.getEigenFaceHeight());
	      
	    BufferedImage grayscale = Utils.convertToGrayscale(after);
	
		return IplImage.createFrom(grayscale);
  }
  
  public String findMatchingImage(IplImage image){
	  return findMatchingImage(image.getBufferedImage());
  }
  
  public boolean authenticateWithEigenfaces(IplImage face1, IplImage face2){
	  
	    DenseDoubleMatrix2D inputface1 = Utils.normalize(doubleVectorToDenseMatrix(Utils.getImageDoublePixels(face1.getBufferedImage())));
	    DenseDoubleMatrix2D inputface2 = Utils.normalize(doubleVectorToDenseMatrix(Utils.getImageDoublePixels(face2.getBufferedImage())));
	    
	    inputface1.assign(doubleVectorToDenseMatrix(faceDB.getAverageFace()), Functions.minus);
	    inputface2.assign(doubleVectorToDenseMatrix(faceDB.getAverageFace()), Functions.minus);
	    
	    DenseDoubleMatrix2D projectedFace1 = projectToFacesSpace(inputface1);
	    DenseDoubleMatrix2D projectedFace2 = projectToFacesSpace(inputface2);
	    
	    double distance = 0;
	    for(int i = 0; i < sysConfig.getEigenVectorsNumber(); i++){
	    	double tmp = projectedFace1.toArray()[0][i] - projectedFace2.toArray()[0][i];
	    	distance += tmp*tmp;
	    }
	    
	    distance = Math.sqrt(distance);
	    
	    System.out.println("Distance: "+distance);
	    if(distance <= sysConfig.getThreshold()) return true;
	    else return false;
  }
  
  
}





