package it.cnr.iit.detection;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
//import org.dom4j.Document;
//import org.dom4j.DocumentException;
//import org.dom4j.io.SAXReader;

public class SysConfig
{
//  private static final String CONFIG_FILE = "conf.xml";
  private static SysConfig instance;
  private String dBPath;
  private int eyeDistance;
  private int eigenFaceWidth;
  private int eigenFaceHeight;
  private int eigenVectorsNumber;
  private int threshold;
//  
//  private SysConfig()
//    throws URISyntaxException
//  {
//    if (instance != null) {
//      throw new Error();
//    }
//    Document doc = getConfigDoc();
//    this.dBPath = doc.selectSingleNode("//conf/db_path").getText();
//    this.eyeDistance = Integer.parseInt(doc.selectSingleNode("//conf/eye_distance").getText());
//    this.eigenFaceWidth = Integer.parseInt(doc.selectSingleNode("//conf/eigen_face_width").getText());
//    this.eigenFaceHeight = Integer.parseInt(doc.selectSingleNode("//conf/eigen_face_height").getText());
//    this.eigenVectorsNumber = Integer.parseInt(doc.selectSingleNode("//conf/num_of_eigenfaces").getText());
//    this.threshold = Integer.parseInt(doc.selectSingleNode("//conf/threshold").getText());
//  }
//  
//  private static Document getConfigDoc()
//    throws URISyntaxException
//  {
//    URL fileURL = SysConfig.class.getClassLoader().getResource("conf.xml");
//    SAXReader reader = new SAXReader();
//    Document doc = null;
//    try
//    {
//      if (fileURL == null)
//      {
//        File xml = new File(Utils.getJarParentDir() + "\\" + "conf.xml");
//        doc = reader.read(xml);
//      }
//      else
//      {
//        doc = reader.read(fileURL);
//      }
//    }
//    catch (DocumentException e)
//    {
//      e.printStackTrace();
//    }
//    return doc;
//  }
//  
  public String getDBPath()
  {
    return this.dBPath;
  }
  
  public int getEyeDistance()
  {
    return this.eyeDistance;
  }
  
  public int getEigenFaceWidth()
  {
    return this.eigenFaceWidth;
  }
  
  public int getEigenFaceHeight()
  {
    return this.eigenFaceHeight;
  }
  
  public int getEigenVectorsNumber()
  {
    return this.eigenVectorsNumber;
  }
  
  public int getThreshold()
  {
    return this.threshold;
  }
  
  public static synchronized SysConfig getInstance()
  {
    if (instance == null) {
        instance = new SysConfig();
    }
    return instance;
  }
}
