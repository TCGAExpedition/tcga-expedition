package edu.pitt.tcga.httpclient.module.images;

public class DiagnosticImage extends ImageModule{
	@Override
	public String getDataType() {
		// TODO Auto-generated method stub
		return "Diagnostic_Images";
	}
	
	@Override
	public String getPlatformIdent() {
		return "/diagnostic_images/";
	}
}
