package it.eng.msp.core.model;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Angelo Marguglio <br>
 *         Company: Engineering Ingegneria Informatica S.p.A. <br>
 *         E-mail: <a href="mailto:angelo.marguglio@eng.it">angelo.marguglio@eng.it</a>
 *
 */
@XmlRootElement
public class FaceMatch2 {

	private byte[] sampleIMG;

	private boolean success;
	private String persoName;
	
	public FaceMatch2() {
	}

	public FaceMatch2(byte[] sampleIMG) {
		this.sampleIMG = sampleIMG;
	}

	public FaceMatch2(byte[] sampleIMG, boolean success, String persoName) {
		this.sampleIMG = sampleIMG;
		this.success = success;
		this.persoName = persoName;
	}

	public byte[] getSampleIMG() {
		return sampleIMG;
	}

	public void setSampleIMG(byte[] sampleIMG) {
		this.sampleIMG = sampleIMG;
	}

	public String getPersonName() {
		return persoName;
	}

	public void setPersoName(String persoName) {
		this.persoName = persoName;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public boolean isValid() {
		if(sampleIMG==null) return false;

		return true;
	}

}
