package edu.pitt.tcga.httpclient.module.clin;

/**
 * Holds parameters to ease the finding of missing records.
 * 
 * @author opm1
 * 
 */
public class ClinicalBean {

	private String barcode = null;
	private String tcgaPath = null;
	private String pgrrUUID = null;

	/**
	 * constructor
	 * 
	 * @param barcode
	 *            - barcode field value in storage
	 * @param tcgaPath
	 *            - tcgaPath field value in storage
	 * @param pgrrUUID
	 *            - pgrrUUID field value in storage
	 */
	public ClinicalBean(String barcode, String tcgaPath, String pgrrUUID) {
		setBarcode(barcode);
		setTCGAPath(tcgaPath);
		setPgrrUUID(pgrrUUID);
	}

	/**
	 * 
	 * @return barcode
	 */
	public String getBarcode() {
		return barcode;
	}

	/**
	 * Sets barcode
	 * 
	 * @param barcode
	 * @return Nothing.
	 */
	public void setBarcode(String barcode) {
		this.barcode = barcode;
	}

	/**
	 * Sets tcgaPath
	 * 
	 * @param tcgaPath
	 * @return Nothing.
	 */
	public void setTCGAPath(String tcgaPath) {
		this.tcgaPath = tcgaPath;
	}

	/**
	 * 
	 * @return tcgaPath
	 */
	public String getTCGAPath() {
		return tcgaPath;
	}

	/**
	 * sets pgrrUUID
	 * 
	 * @param pgrrUUID
	 * @return Nothing.
	 */
	public void setPgrrUUID(String pgrrUUID) {
		this.pgrrUUID = pgrrUUID;
	}

	/**
	 * 
	 * @return pgrrUUID
	 */
	public String getPgrrUUID() {
		return pgrrUUID;
	}

	/**
	 * checks for barcode mtch
	 * 
	 * @param bc
	 *            - other barcode to be compared with the local one
	 * @return true if barcode matches
	 */
	public boolean exists(String bc) {
		return barcode.equals(bc);
	}

	/**
	 * Used to create error message if this record is missed from the curent
	 * archive
	 */
	public String toString() {
		return (" WHILE exists in storage tcgaPath: " + tcgaPath + " barcode: "
				+ barcode + "  record uuid: " + pgrrUUID);
	}

}
