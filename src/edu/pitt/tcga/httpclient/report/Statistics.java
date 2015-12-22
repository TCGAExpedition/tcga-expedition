package edu.pitt.tcga.httpclient.report;

public class Statistics {
	
	private static String COUNT_ALIQUOTS = "PREFIX pgrr:<http://purl.org/pgrr/core#> "+
"PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "+
"SELECT ?disAbbr ?dataType ?center ?platform ?level (count(?file) as ?Num_Files) "+
"(bif:round(sum(xsd:double(?file_size_in_bytes)/1000000)) AS ?SIZE_IN_MB) from pgrr:pgrr-meta "+
"WHERE { ?s pgrr:diseaseAbbr ?disAbbr; pgrr:analysisType ?dataType;"+
"pgrr:analysisPlatform ?platform; pgrr:fileSizeInBytes ?file_size_in_bytes; pgrr:centerName ?center; "+
"pgrr:level ?lev; pgrr:pgrrFileName ?file . "+
"BIND( xsd:decimal(?lev) AS ?level) "+
"OPTIONAL {?s pgrr:dateArchived ?dateArchived .} "+
"FILTER (!BOUND(?dateArchived))} order by ?disAbbr ?dataType ?center ?platform ?level";
	
	private static String POSTGRES_SUMMARY = "SELECT DISTINCT diseaseAbbr AS \"Disease\", analysisType AS \"DataType\",  COUNT(pgrrfilename) AS \"Num_Files\", "+ 
												"ROUND(SUM(filesizeinbytes::numeric/(1000000000)),3) AS \"Size (GB)\" FROM pgrr_meta "+
												"WHERE datearchived IS NULL "+
												"GROUP BY diseaseAbbr, analysisType ORDER by diseaseAbbr, analysisType";

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
