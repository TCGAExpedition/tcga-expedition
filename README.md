#tcga-expedition
The Cancer Genome Atlas Project (TCGA) is a National Cancer Institute effort to profile at least 500 cases of 20 different tumor types using genomic platforms and to make these data, both raw and processed, available to all researchers. TCGA data are currently over 1.2 Petabyte in size and include whole genome sequence (WGS), whole exome sequence, methylation, RNA expression, proteomic, and clinical datasets. Publicly accessible TCGA data are released through public portals, but many challenges exist in navigating and using data obtained from these sites.

We developed TCGA Expedition to support the research community focused on computational methods for cancer research. Data obtained, versioned, and archived using TCGA Expedition supports analysis with third-party tools as well as command line access at high-performance computing facilities. TCGA Expedition software consists of a set of scripts written in Bash, Python and Java that download, extract, harmonize, version and store all TCGA data and metadata. The software generates a versioned, participant- and sample-centered, local TCGA data directory with metadata structures that directly reference the local data files as well as the original data files. The software supports flexible searches of the data via a web portal, user-centric data tracking tools, and data provenance tools.

# Access Requirements
Users accessing controlled data will need to have a [dbGAP](http://www.ncbi.nlm.nih.gov/gap) Data Use Certificate.

#System Requirements

Unix 64-bit. Java 1.7

#Installation

<b>1. Select and install one of the supported storage</b>

 - <i>PostgreSLQ</i>: http://www.postgresql.org/download/
 - <i>Virtuoso</i>: http://virtuoso.openlinksw.com/dataspace/doc/dav/wiki/Main/VOSDownload
 
<b>NOTE:</b> We recommend to use PostgreSQL - it's much faster than RDF store.

<b>2. Configure</b>

Go through resources/tcgaexpedition.conf file and add required fields.

#How to Run
```
Usage: java -jar tcgaExpedition-<v.x.x>.jar --diseaseList <list> --analysistype <string> --accesstype <string>
================================================================================
--diseaseList           Comma separated list of disease abbreviations. Use ALL for the whole data set.
--analysistype          See table below for available analysis types based on the data source.
--accesstype       	Use public or controlled. Access type depends on analysis type ans data level. See table below.
```

<b>Example</b>

Download Clinical data for acc:
```bash
java -jar tcgaExpedition-<v.x.x>.jar acc clinical public
```

#Availabe Analysis / Access Types

<HTML>

<BODY TEXT="#000000">
<TABLE CELLSPACING="0" COLS="4" BORDER="0">
	<COLGROUP WIDTH="100"></COLGROUP>
	<COLGROUP WIDTH="199"></COLGROUP>
	<COLGROUP WIDTH="113"></COLGROUP>
	<COLGROUP WIDTH="72"></COLGROUP>
	<TR>
		<TD STYLE="border-top: 1px solid #000000; border-bottom: 1px solid #000000; border-left: 1px solid #000000; border-right: 1px solid #000000" HEIGHT="16" ALIGN="LEFT"><B><FONT FACE="DejaVu Sans">DataSource</FONT></B></TD>
		<TD STYLE="border-top: 1px solid #000000; border-bottom: 1px solid #000000; border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="LEFT"><B><FONT FACE="DejaVu Sans" COLOR="#000000">AnalysisType</FONT></B></TD>
		<TD STYLE="border-top: 1px solid #000000; border-bottom: 1px solid #000000; border-right: 1px solid #000000" ALIGN="LEFT"><B><FONT FACE="DejaVu Sans">AccessType</FONT></B></TD>
		<TD STYLE="border-top: 1px solid #000000; border-bottom: 1px solid #000000; border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="CENTER" SDNUM="1033;0;@"><B><FONT FACE="DejaVu Sans">Level</FONT></B></TD>
	</TR>
	<TR>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" HEIGHT="16" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans">TCGA</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans" COLOR="#000000">clinical</FONT></TD>
		<TD STYLE="border-right: 1px solid #000000" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans">public</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="CENTER" BGCOLOR="#E6E6E6" SDVAL="2" SDNUM="1033;0;@"><FONT FACE="DejaVu Sans">2</FONT></TD>
	</TR>
	<TR>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" HEIGHT="16" ALIGN="LEFT"><FONT FACE="DejaVu Sans">TCGA</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="LEFT"><FONT FACE="DejaVu Sans" COLOR="#000000">cnv_(cn_array)</FONT></TD>
		<TD STYLE="border-right: 1px solid #000000" ALIGN="LEFT"><FONT FACE="DejaVu Sans">public</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="CENTER" SDNUM="1033;0;@"><FONT FACE="DejaVu Sans">1,2,3</FONT></TD>
	</TR>
	<TR>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" HEIGHT="16" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans">TCGA</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans" COLOR="#000000">cnv_(snp_array)</FONT></TD>
		<TD STYLE="border-right: 1px solid #000000" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans">controlled</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="CENTER" BGCOLOR="#E6E6E6" SDNUM="1033;0;@"><FONT FACE="DejaVu Sans">1,2</FONT></TD>
	</TR>
	<TR>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" HEIGHT="16" ALIGN="LEFT"><FONT FACE="DejaVu Sans">TCGA</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="LEFT"><FONT FACE="DejaVu Sans" COLOR="#000000">cnv_(snp_array)</FONT></TD>
		<TD STYLE="border-right: 1px solid #000000" ALIGN="LEFT"><FONT FACE="DejaVu Sans">public</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="CENTER" SDVAL="3" SDNUM="1033;0;@"><FONT FACE="DejaVu Sans">3</FONT></TD>
	</TR>
	<TR>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" HEIGHT="16" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans">TCGA</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans" COLOR="#000000">cnv_(low_pass_dnaseq)</FONT></TD>
		<TD STYLE="border-right: 1px solid #000000" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans">controlled</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="CENTER" BGCOLOR="#E6E6E6" SDVAL="2" SDNUM="1033;0;@"><FONT FACE="DejaVu Sans">2</FONT></TD>
	</TR>
	<TR>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" HEIGHT="16" ALIGN="LEFT"><FONT FACE="DejaVu Sans">TCGA</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="LEFT"><FONT FACE="DejaVu Sans" COLOR="#000000">cnv_(low_pass_dnaseq)</FONT></TD>
		<TD STYLE="border-right: 1px solid #000000" ALIGN="LEFT"><FONT FACE="DejaVu Sans">public</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="CENTER" SDVAL="3" SDNUM="1033;0;@"><FONT FACE="DejaVu Sans">3</FONT></TD>
	</TR>
	<TR>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" HEIGHT="16" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans">TCGA</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans" COLOR="#000000">dna_methylation</FONT></TD>
		<TD STYLE="border-right: 1px solid #000000" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans">public</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="CENTER" BGCOLOR="#E6E6E6" SDNUM="1033;0;@"><FONT FACE="DejaVu Sans">1,2,3</FONT></TD>
	</TR>
	<TR>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" HEIGHT="16" ALIGN="LEFT"><FONT FACE="DejaVu Sans">TCGA</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="LEFT"><FONT FACE="DejaVu Sans" COLOR="#000000">expression_gene</FONT></TD>
		<TD STYLE="border-right: 1px solid #000000" ALIGN="LEFT"><FONT FACE="DejaVu Sans">public</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="CENTER" SDNUM="1033;0;@"><FONT FACE="DejaVu Sans">1,2,3</FONT></TD>
	</TR>
	<TR>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" HEIGHT="16" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans">TCGA</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans" COLOR="#000000">expression_protein</FONT></TD>
		<TD STYLE="border-right: 1px solid #000000" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans">public</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="CENTER" BGCOLOR="#E6E6E6" SDNUM="1033;0;@"><FONT FACE="DejaVu Sans">0,1,2,3</FONT></TD>
	</TR>
	<TR>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" HEIGHT="16" ALIGN="LEFT"><FONT FACE="DejaVu Sans">TCGA</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="LEFT"><FONT FACE="DejaVu Sans" COLOR="#000000">fragment_analysis</FONT></TD>
		<TD STYLE="border-right: 1px solid #000000" ALIGN="LEFT"><FONT FACE="DejaVu Sans">controlled</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="CENTER" SDVAL="1" SDNUM="1033;0;@"><FONT FACE="DejaVu Sans">1</FONT></TD>
	</TR>
	<TR>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" HEIGHT="16" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans">TCGA</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans" COLOR="#000000">images</FONT></TD>
		<TD STYLE="border-right: 1px solid #000000" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans">public</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="CENTER" BGCOLOR="#E6E6E6" SDVAL="1" SDNUM="1033;0;@"><FONT FACE="DejaVu Sans">1</FONT></TD>
	</TR>
	<TR>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" HEIGHT="16" ALIGN="LEFT"><FONT FACE="DejaVu Sans">TCGA</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="LEFT"><FONT FACE="DejaVu Sans" COLOR="#000000">mirnaseq</FONT></TD>
		<TD STYLE="border-right: 1px solid #000000" ALIGN="LEFT"><FONT FACE="DejaVu Sans">public</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="CENTER" SDVAL="3" SDNUM="1033;0;@"><FONT FACE="DejaVu Sans">3</FONT></TD>
	</TR>
	<TR>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" HEIGHT="16" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans">TCGA</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans" COLOR="#000000">protected_mutations</FONT></TD>
		<TD STYLE="border-right: 1px solid #000000" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans">controlled</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="CENTER" BGCOLOR="#E6E6E6" SDVAL="2" SDNUM="1033;0;@"><FONT FACE="DejaVu Sans">2</FONT></TD>
	</TR>
	<TR>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" HEIGHT="16" ALIGN="LEFT"><FONT FACE="DejaVu Sans">TCGA</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="LEFT"><FONT FACE="DejaVu Sans" COLOR="#000000">protected_mutations_maf</FONT></TD>
		<TD STYLE="border-right: 1px solid #000000" ALIGN="LEFT"><FONT FACE="DejaVu Sans">public</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="CENTER" SDVAL="2" SDNUM="1033;0;@"><FONT FACE="DejaVu Sans">2</FONT></TD>
	</TR>
	<TR>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" HEIGHT="16" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans">TCGA</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans" COLOR="#000000">rnaseq</FONT></TD>
		<TD STYLE="border-right: 1px solid #000000" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans">controlled</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="CENTER" BGCOLOR="#E6E6E6" SDVAL="2" SDNUM="1033;0;@"><FONT FACE="DejaVu Sans">2</FONT></TD>
	</TR>
	<TR>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" HEIGHT="16" ALIGN="LEFT"><FONT FACE="DejaVu Sans">TCGA</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="LEFT"><FONT FACE="DejaVu Sans" COLOR="#000000">rnaseq</FONT></TD>
		<TD STYLE="border-right: 1px solid #000000" ALIGN="LEFT"><FONT FACE="DejaVu Sans">public</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="CENTER" SDVAL="3" SDNUM="1033;0;@"><FONT FACE="DejaVu Sans">3</FONT></TD>
	</TR>
	<TR>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" HEIGHT="16" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans">TCGA</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans" COLOR="#000000">rnaseqv2</FONT></TD>
		<TD STYLE="border-right: 1px solid #000000" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans">public</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="CENTER" BGCOLOR="#E6E6E6" SDVAL="3" SDNUM="1033;0;@"><FONT FACE="DejaVu Sans">3</FONT></TD>
	</TR>
	<TR>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" HEIGHT="16" ALIGN="LEFT"><FONT FACE="DejaVu Sans">TCGA</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="LEFT"><FONT FACE="DejaVu Sans" COLOR="#000000">somatic_mutations</FONT></TD>
		<TD STYLE="border-right: 1px solid #000000" ALIGN="LEFT"><FONT FACE="DejaVu Sans">public</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="CENTER" SDVAL="2" SDNUM="1033;0;@"><FONT FACE="DejaVu Sans">2</FONT></TD>
	</TR>
	<TR>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" HEIGHT="16" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans">Firehose</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans" COLOR="#000000">CN_Level4</FONT></TD>
		<TD STYLE="border-right: 1px solid #000000" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans">controlled</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="CENTER" BGCOLOR="#E6E6E6" SDVAL="4" SDNUM="1033;0;@"><FONT FACE="DejaVu Sans">4</FONT></TD>
	</TR>
	<TR>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" HEIGHT="16" ALIGN="LEFT"><FONT FACE="DejaVu Sans">Georgetown</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="LEFT"><FONT FACE="DejaVu Sans" COLOR="#000000">mass_spectrometry</FONT></TD>
		<TD STYLE="border-right: 1px solid #000000" ALIGN="LEFT"><FONT FACE="DejaVu Sans">public</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="CENTER" SDVAL="4" SDNUM="1033;0;@"><FONT FACE="DejaVu Sans">4</FONT></TD>
	</TR>
	<TR>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" HEIGHT="17" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans">cgHub*</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans" COLOR="#000000">bisulfite-seq_(cghub)</FONT></TD>
		<TD STYLE="border-right: 1px solid #000000" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans">controlled</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="CENTER" BGCOLOR="#E6E6E6" SDVAL="1" SDNUM="1033;0;@"><FONT FACE="DejaVu Sans">1</FONT></TD>
	</TR>
	<TR>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" HEIGHT="17" ALIGN="LEFT"><FONT FACE="DejaVu Sans">cgHub*</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="LEFT"><FONT FACE="DejaVu Sans" COLOR="#000000">mirna-seq_(cghub)</FONT></TD>
		<TD STYLE="border-right: 1px solid #000000" ALIGN="LEFT"><FONT FACE="DejaVu Sans">controlled</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="CENTER" SDVAL="1" SDNUM="1033;0;@"><FONT FACE="DejaVu Sans">1</FONT></TD>
	</TR>
	<TR>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" HEIGHT="17" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans">cgHub*</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans" COLOR="#000000">rna-seq_(cghub)</FONT></TD>
		<TD STYLE="border-right: 1px solid #000000" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans">controlled</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="CENTER" BGCOLOR="#E6E6E6" SDVAL="1" SDNUM="1033;0;@"><FONT FACE="DejaVu Sans">1</FONT></TD>
	</TR>
	<TR>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" HEIGHT="17" ALIGN="LEFT"><FONT FACE="DejaVu Sans">cgHub*</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="LEFT"><FONT FACE="DejaVu Sans" COLOR="#000000">validation_(cghub)</FONT></TD>
		<TD STYLE="border-right: 1px solid #000000" ALIGN="LEFT"><FONT FACE="DejaVu Sans">controlled</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="CENTER" SDVAL="1" SDNUM="1033;0;@"><FONT FACE="DejaVu Sans">1</FONT></TD>
	</TR>
	<TR>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" HEIGHT="17" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans">cgHub*</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans" COLOR="#000000">wgs_(cghub)</FONT></TD>
		<TD STYLE="border-right: 1px solid #000000" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans">controlled</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="CENTER" BGCOLOR="#E6E6E6" SDVAL="1" SDNUM="1033;0;@"><FONT FACE="DejaVu Sans">1</FONT></TD>
	</TR>
	<TR>
		<TD STYLE="border-bottom: 1px solid #000000; border-left: 1px solid #000000; border-right: 1px solid #000000" HEIGHT="17" ALIGN="LEFT"><FONT FACE="DejaVu Sans">cgHub*</FONT></TD>
		<TD STYLE="border-bottom: 1px solid #000000; border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="LEFT"><FONT FACE="DejaVu Sans" COLOR="#000000">wxs_(cghub)</FONT></TD>
		<TD STYLE="border-bottom: 1px solid #000000; border-right: 1px solid #000000" ALIGN="LEFT"><FONT FACE="DejaVu Sans">controlled</FONT></TD>
		<TD STYLE="border-bottom: 1px solid #000000; border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="CENTER" SDVAL="1" SDNUM="1033;0;@"><FONT FACE="DejaVu Sans">1</FONT></TD>
	</TR>
</TABLE>
<!-- ************************************************************************** -->
</BODY>

</HTML>

* - coming soon
* 

#License
[GPLv2] (http://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html)

