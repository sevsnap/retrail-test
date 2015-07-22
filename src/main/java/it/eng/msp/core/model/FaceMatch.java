package it.eng.msp.core.model;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class FaceMatch {

	private byte[] sampleIMG;
	private byte[] templateIMG;

	private boolean success;
	
	public FaceMatch() {
	}

	public FaceMatch(byte[] sampleIMG, byte[] templateIMG) {
		this.sampleIMG = sampleIMG;
		this.templateIMG = templateIMG;
	}

	public FaceMatch(byte[] sampleIMG, byte[] templateIMG, boolean success) {
		this.sampleIMG = sampleIMG;
		this.templateIMG = templateIMG;
		this.success = success;
	}

	public byte[] getSampleIMG() {
		return sampleIMG;
	}

	public void setSampleIMG(byte[] sampleIMG) {
		this.sampleIMG = sampleIMG;
	}

	public byte[] getTemplateIMG() {
		return templateIMG;
	}

	public void setTemplateIMG(byte[] templateIMG) {
		this.templateIMG = templateIMG;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public boolean isValid() {
		if(sampleIMG==null || templateIMG==null) return false;

		return true;
	}

}
