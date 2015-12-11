package edu.pitt.tcga.httpclient.storage;

import edu.pitt.tcga.httpclient.util.MySettings;

public class StorageFactory {
	
	private static Storage[] availStorages = null;
	private  static Storage storage = null; // connectTo
	
	public static Storage getStorage(){
		if(storage == null){
			storage = getStorage(MySettings.getStrProperty("storage.name"));	
		}
		return storage;
	}
	
	private static Storage getStorage(String storageName){
		Storage locStorage = null;
		switch (storageName){
		case "virtuoso":
			locStorage = VirtuosoStorage.getInstace();
			break;
		case "postgres":
			locStorage = PostgreStorage.getInstace();
			break;
		
		default: 
			System.out.println("No storage named: "+storageName);
			System.exit(0);
		}
		return locStorage;
	}

}
