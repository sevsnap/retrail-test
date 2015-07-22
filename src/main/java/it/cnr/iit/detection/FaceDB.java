package it.cnr.iit.detection;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.imageio.ImageIO;
import javax.media.jai.JAI;
import javax.media.jai.RenderedImageAdapter;

public class FaceDB
{
  private double[][] eigenFaces;
  private double[] averageFace;
  private double[][] adjustedFaces;
  private List<String> imageFileNamesList;
  private double[] eigenValues;
  
  public FaceDB(List<String> imageFileNamesList, double[][] eigenFaces, double[][] adjustedFaces, double[] avgFace, double[] eigenValues)
  {
    this.imageFileNamesList = imageFileNamesList;
    this.adjustedFaces = adjustedFaces;
    this.averageFace = avgFace;
    this.eigenFaces = eigenFaces;
    this.eigenValues = eigenValues;
  }
  
  public double[][] getEigenFaces()
  {
    return this.eigenFaces;
  }
  
  public void setEigenFaces(double[][] eigenFaces)
  {
    this.eigenFaces = eigenFaces;
  }
  
  public double[] getAverageFace()
  {
    return this.averageFace;
  }
  
  public void setAverageFace(double[] averageFace)
  {
    this.averageFace = averageFace;
  }
  
  public double[][] getAdjustedFaces()
  {
    return this.adjustedFaces;
  }
  
  public void setAdjustedFaces(double[][] adjustedFaces)
  {
    this.adjustedFaces = adjustedFaces;
  }
  
  public List<String> getImageFileNamesList()
  {
    return this.imageFileNamesList;
  }
  
  public void setImageFileNamesList(List<String> imageFileNamesList)
  {
    this.imageFileNamesList = imageFileNamesList;
  }
  
  public double[] getEigenValues()
  {
    return this.eigenValues;
  }
  
  public void setEigenValues(double[] eigenValues)
  {
    this.eigenValues = eigenValues;
  }
  
  public static BufferedImage[] loadGrayScaleImages(List<String> filenames)
  {
    BufferedImage[] bufimgs = new BufferedImage[filenames.size()];
    Iterator<String> it = filenames.iterator();
    int i = 0;
    while (it.hasNext())
    {
      String fn = (String)it.next();
      File f = new File(Utils.getJarParentDir() + fn);
      if (f.isFile())
      {
        RenderedImage rendimg = JAI.create("fileload", Utils.getJarParentDir() + fn);
        BufferedImage b = Utils.convertToGrayscale(new RenderedImageAdapter(rendimg).getAsBufferedImage());
        bufimgs[(i++)] = b;
      }
    }
    return bufimgs;
  }
  
  public static void checkImageDimensions(List<String> filenames, BufferedImage[] images)
  {
    int width = images[0].getRaster().getWidth();
    int height = images[0].getRaster().getHeight();
    for (int i = 0; i < images.length; i++) {
      if ((width != images[i].getWidth()) || (height != images[i].getHeight())) {
        throw new RuntimeException("Sytem requires images of size " + width + "x" + height + " ," + " file " + (String)filenames.get(i) + " contains image of differnt size.");
      }
    }
  }
  
  public static BufferedImage[] getImagesFromDB(List<String> filenames)
  {
    BufferedImage[] images = loadGrayScaleImages(filenames);
    checkImageDimensions(filenames, images);
    
    return images;
  }
  
  public static void saveImage(String fileName, BufferedImage image)
    throws IOException
  {
    File file = new File(fileName);
    ImageIO.write(image, "png", file);
  }
  
  public static List<String> getFileNames(String dir, String[] children)
  {
    List<String> imageFileNames = new ArrayList();
    for (String name : children)
    {
      String fileName = dir + File.separator + name;
      imageFileNames.add(fileName);
    }
    Collections.sort(imageFileNames);
    return imageFileNames;
  }
  
  public static List<String> parseDirectory(String directoryName)
  {
    final String[] extensions = { ".jpg", ".gif", ".jpeg", ".png", ".JPG", ".GIF", ".JPEG", ".PNG" };
    String[] children = null;
    
    File directory = null;
    directory = new File(Utils.getJarParentDir() + directoryName);
    if (directory.isDirectory()) {
      children = directory.list(new FilenameFilter()
      {
        public boolean accept(File f, String name)
        {
          for (String extension : extensions) {
            if (name.endsWith(extension)) {
              return true;
            }
          }
          return false;
        }
      });
    } else {
      throw new RuntimeException(directoryName + " is not a directory");
    }
    return getFileNames(directoryName, children);
  }
}
