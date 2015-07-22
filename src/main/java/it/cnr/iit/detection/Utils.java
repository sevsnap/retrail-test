package it.cnr.iit.detection;

import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import javax.media.jai.JAI;
import javax.media.jai.RenderedImageAdapter;

public class Utils
{
  public static BufferedImage loadImage(String path)
  {
    RenderedImage rendimg = JAI.create("fileload", getJarParentDir() + path);
    return new RenderedImageAdapter(rendimg).getAsBufferedImage();
  }
  
  public static BufferedImage convertToGrayscale(BufferedImage image)
  {
    BufferedImage after = null;
    try
    {
      after = new BufferedImage(image.getWidth(), image.getHeight(), 10);
      ColorConvertOp op = new ColorConvertOp(ColorSpace.getInstance(1003), null);
      op.filter(image, after);
    }
    catch (Exception e)
    {
      throw new RuntimeException("Failed to convert image to grayscale");
    }
    return after;
  }
  
  public static BufferedImage scaleToWindow(BufferedImage image, int windowWidth, int windowHeight)
  {
    BufferedImage after = null;
    try
    {
      after = new BufferedImage(windowWidth, windowHeight, image.getType());
      AffineTransform at = new AffineTransform();
      

      at.setToScale((float)windowWidth / image.getRaster().getWidth(), (float)windowHeight / image.getRaster().getHeight());
      
      AffineTransformOp scaleOp = new AffineTransformOp(at, 2);
      
      scaleOp.filter(image, after);
    }
    catch (Exception e)
    {
      throw new RuntimeException("Failed to scale image");
    }
    return after;
  }
  
  public static DenseDoubleMatrix2D normalize(DenseDoubleMatrix2D matrix)
  {
    double[][] data = matrix.toArray();
    double[] maxValues = new double[data.length];
    for (int i = 0; i < data.length; i++) {
      maxValues[i] = max(data[i]);
    }
    for (int i = 0; i < data.length; i++) {
      for (int j = 0; j < data[0].length; j++) {
        data[i][j] /= maxValues[i];
      }
    }
    return new DenseDoubleMatrix2D(data);
  }
  
  private static double max(double[] arr)
  {
    double m = 4.9E-324D;
    for (int i = 0; i < arr.length; i++) {
      m = Math.max(m, arr[i]);
    }
    return m;
  }
  
  public static DenseDoubleMatrix2D imagesToDenseMatrix(BufferedImage[] bufImgs)
  {
    int imageWidth = bufImgs[0].getWidth();
    int imageHeight = bufImgs[0].getHeight();
    int rows = bufImgs.length;
    int cols = imageWidth * imageHeight;
    double[][] data = new double[rows][cols];
    for (int i = 0; i < rows; i++) {
      bufImgs[i].getData().getPixels(0, 0, imageWidth, imageHeight, data[i]);
    }
    return new DenseDoubleMatrix2D(data);
  }
  
  public static double[] getImageDoublePixels(BufferedImage image)
  {
    int imageWidth = image.getWidth();
    int imageHeight = image.getHeight();
    double[] inputFace = new double[imageWidth * imageHeight];
    image.getData().getPixels(0, 0, imageWidth, imageHeight, inputFace);
    return inputFace;
  }
  
  public static double[] getAverageImage(DenseDoubleMatrix2D manifold)
  {
    int cols = manifold.columns();
    int rows = manifold.rows();
    double[][] data = manifold.toArray();
    double t = 0.0D;
    double[] avgValues = new double[cols];
    for (int i = 0; i < cols; i++)
    {
      t = 0.0D;
      for (int j = 0; j < rows; j++) {
        t += data[j][i];
      }
      avgValues[i] = (t / rows);
    }
    return avgValues;
  }
  
  public static DenseDoubleMatrix2D subtractFromEachRow(DenseDoubleMatrix2D mat, double[] array)
  {
    double[][] denseArray = mat.toArray();
    for (int i = 0; i < denseArray.length; i++) {
      for (int j = 0; j < denseArray[0].length; j++) {
        denseArray[i][j] -= array[j];
      }
    }
    return new DenseDoubleMatrix2D(denseArray);
  }
  
  public static double[] getDiagonal(double[][] matrix)
  {
    double[] diagonal = new double[matrix.length];
    for (int i = 0; i < matrix.length; i++) {
      diagonal[i] = matrix[i][i];
    }
    return diagonal;
  }
  
  public static double norm(double[] oneDArray)
  {
    double val = 0.0D;
    for (int i = 0; i < oneDArray.length; i++) {
      val += oneDArray[i] * oneDArray[i];
    }
    return Math.sqrt(val);
  }
  
  public static String getJarParentDir()
  {
    File file = null;
    try
    {
      file = new File(Utils.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    }
    catch (URISyntaxException e)
    {
      e.printStackTrace();
    }
    return file.getParent() + "\\";
  }
}
