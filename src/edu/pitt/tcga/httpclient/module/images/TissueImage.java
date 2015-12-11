package edu.pitt.tcga.httpclient.module.images;

public class TissueImage extends ImageModule{
	
	@Override
	public String getDataType() {
		return "Tissue_Images";
	}

	@Override
	public String getPlatformIdent() {
		return "/tissue_images/";
	}
}