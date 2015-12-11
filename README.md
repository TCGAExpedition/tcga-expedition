#tcga-expedition
Provides data management and computing infrastructure to support biomedical investigation using Big Data.


<b>System requirements</b>

Unix 64-bit


<b>Installation</b>

<b>1.</b> Select and install storage

  - <i>PostgreSLQ</i>: http://www.postgresql.org/download/
  
 OR

 - <i>Virtuoso</i>: http://virtuoso.openlinksw.com/dataspace/doc/dav/wiki/Main/VOSDownload
 
<b>NOTE:</b> We recommend to use PostgreSQL - it's much faster than RDF store.

<b>2.</b> Configure

Go through resources/tcgaexpedition.conf file and add required fields.


<b>Example</b>

Start with Clinical data:

-- download Clinical data for acc
java -jar tcgaExpedition-1.1.jar acc clinical public


<b>Availabe Analysis Types</b>

<HTML>

<BODY TEXT="#000000">
<TABLE CELLSPACING="0" COLS="4" BORDER="0">
	<COLGROUP WIDTH="100"></COLGROUP>
	<COLGROUP WIDTH="199"></COLGROUP>
	<COLGROUP WIDTH="72"></COLGROUP>
	<COLGROUP WIDTH="113"></COLGROUP>
	<TR>
		<TD STYLE="border-top: 1px solid #000000; border-bottom: 1px solid #000000; border-left: 1px solid #000000; border-right: 1px solid #000000" HEIGHT="16" ALIGN="LEFT"><B><FONT FACE="DejaVu Sans">DataSource</FONT></B></TD>
		<TD STYLE="border-top: 1px solid #000000; border-bottom: 1px solid #000000; border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="LEFT"><B><FONT FACE="DejaVu Sans" COLOR="#000000">AnalysisType</FONT></B></TD>
		<TD STYLE="border-top: 1px solid #000000; border-bottom: 1px solid #000000; border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="CENTER" SDNUM="1033;0;@"><B><FONT FACE="DejaVu Sans">Level</FONT></B></TD>
		<TD STYLE="border-top: 1px solid #000000; border-bottom: 1px solid #000000; border-right: 1px solid #000000" ALIGN="LEFT"><B><FONT FACE="DejaVu Sans">AccessType</FONT></B></TD>
	</TR>
	<TR>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" HEIGHT="16" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans">TCGA</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans" COLOR="#000000">clinical</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="CENTER" BGCOLOR="#E6E6E6" SDVAL="2" SDNUM="1033;0;@"><FONT FACE="DejaVu Sans">2</FONT></TD>
		<TD STYLE="border-right: 1px solid #000000" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans">public</FONT></TD>
	</TR>
	<TR>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" HEIGHT="16" ALIGN="LEFT"><FONT FACE="DejaVu Sans">TCGA</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="LEFT"><FONT FACE="DejaVu Sans" COLOR="#000000">cnv_(cn_array)</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="CENTER" SDNUM="1033;0;@"><FONT FACE="DejaVu Sans">1,2,3</FONT></TD>
		<TD STYLE="border-right: 1px solid #000000" ALIGN="LEFT"><FONT FACE="DejaVu Sans">public</FONT></TD>
	</TR>
	<TR>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" HEIGHT="16" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans">TCGA</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans" COLOR="#000000">cnv_(snp_array)</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="CENTER" BGCOLOR="#E6E6E6" SDNUM="1033;0;@"><FONT FACE="DejaVu Sans">1,2</FONT></TD>
		<TD STYLE="border-right: 1px solid #000000" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans">controlled</FONT></TD>
	</TR>
	<TR>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" HEIGHT="16" ALIGN="LEFT"><FONT FACE="DejaVu Sans">TCGA</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="LEFT"><FONT FACE="DejaVu Sans" COLOR="#000000">cnv_(snp_array)</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="CENTER" SDVAL="3" SDNUM="1033;0;@"><FONT FACE="DejaVu Sans">3</FONT></TD>
		<TD STYLE="border-right: 1px solid #000000" ALIGN="LEFT"><FONT FACE="DejaVu Sans">public</FONT></TD>
	</TR>
	<TR>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" HEIGHT="16" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans">TCGA</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans" COLOR="#000000">cnv_(low_pass_dnaseq)</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="CENTER" BGCOLOR="#E6E6E6" SDVAL="2" SDNUM="1033;0;@"><FONT FACE="DejaVu Sans">2</FONT></TD>
		<TD STYLE="border-right: 1px solid #000000" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans">controlled</FONT></TD>
	</TR>
	<TR>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" HEIGHT="16" ALIGN="LEFT"><FONT FACE="DejaVu Sans">TCGA</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="LEFT"><FONT FACE="DejaVu Sans" COLOR="#000000">cnv_(low_pass_dnaseq)</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="CENTER" SDVAL="3" SDNUM="1033;0;@"><FONT FACE="DejaVu Sans">3</FONT></TD>
		<TD STYLE="border-right: 1px solid #000000" ALIGN="LEFT"><FONT FACE="DejaVu Sans">public</FONT></TD>
	</TR>
	<TR>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" HEIGHT="16" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans">TCGA</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans" COLOR="#000000">dna_methylation</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="CENTER" BGCOLOR="#E6E6E6" SDNUM="1033;0;@"><FONT FACE="DejaVu Sans">1,2,3</FONT></TD>
		<TD STYLE="border-right: 1px solid #000000" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans">public</FONT></TD>
	</TR>
	<TR>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" HEIGHT="16" ALIGN="LEFT"><FONT FACE="DejaVu Sans">TCGA</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="LEFT"><FONT FACE="DejaVu Sans" COLOR="#000000">expression_gene</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="CENTER" SDNUM="1033;0;@"><FONT FACE="DejaVu Sans">1,2,3</FONT></TD>
		<TD STYLE="border-right: 1px solid #000000" ALIGN="LEFT"><FONT FACE="DejaVu Sans">public</FONT></TD>
	</TR>
	<TR>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" HEIGHT="16" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans">TCGA</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans" COLOR="#000000">expression_protein</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="CENTER" BGCOLOR="#E6E6E6" SDNUM="1033;0;@"><FONT FACE="DejaVu Sans">0,1,2,3</FONT></TD>
		<TD STYLE="border-right: 1px solid #000000" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans">public</FONT></TD>
	</TR>
	<TR>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" HEIGHT="16" ALIGN="LEFT"><FONT FACE="DejaVu Sans">TCGA</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="LEFT"><FONT FACE="DejaVu Sans" COLOR="#000000">fragment_analysis</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="CENTER" SDVAL="1" SDNUM="1033;0;@"><FONT FACE="DejaVu Sans">1</FONT></TD>
		<TD STYLE="border-right: 1px solid #000000" ALIGN="LEFT"><FONT FACE="DejaVu Sans">controlled</FONT></TD>
	</TR>
	<TR>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" HEIGHT="16" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans">TCGA</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans" COLOR="#000000">images</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="CENTER" BGCOLOR="#E6E6E6" SDVAL="1" SDNUM="1033;0;@"><FONT FACE="DejaVu Sans">1</FONT></TD>
		<TD STYLE="border-right: 1px solid #000000" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans">public</FONT></TD>
	</TR>
	<TR>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" HEIGHT="16" ALIGN="LEFT"><FONT FACE="DejaVu Sans">TCGA</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="LEFT"><FONT FACE="DejaVu Sans" COLOR="#000000">mirnaseq</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="CENTER" SDVAL="3" SDNUM="1033;0;@"><FONT FACE="DejaVu Sans">3</FONT></TD>
		<TD STYLE="border-right: 1px solid #000000" ALIGN="LEFT"><FONT FACE="DejaVu Sans">public</FONT></TD>
	</TR>
	<TR>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" HEIGHT="16" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans">TCGA</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans" COLOR="#000000">protected_mutations</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="CENTER" BGCOLOR="#E6E6E6" SDVAL="2" SDNUM="1033;0;@"><FONT FACE="DejaVu Sans">2</FONT></TD>
		<TD STYLE="border-right: 1px solid #000000" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans">controlled</FONT></TD>
	</TR>
	<TR>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" HEIGHT="16" ALIGN="LEFT"><FONT FACE="DejaVu Sans">TCGA</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="LEFT"><FONT FACE="DejaVu Sans" COLOR="#000000">protected_mutations_maf</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="CENTER" SDVAL="2" SDNUM="1033;0;@"><FONT FACE="DejaVu Sans">2</FONT></TD>
		<TD STYLE="border-right: 1px solid #000000" ALIGN="LEFT"><FONT FACE="DejaVu Sans">public</FONT></TD>
	</TR>
	<TR>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" HEIGHT="16" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans">TCGA</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans" COLOR="#000000">rnaseq</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="CENTER" BGCOLOR="#E6E6E6" SDVAL="2" SDNUM="1033;0;@"><FONT FACE="DejaVu Sans">2</FONT></TD>
		<TD STYLE="border-right: 1px solid #000000" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans">controlled</FONT></TD>
	</TR>
	<TR>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" HEIGHT="16" ALIGN="LEFT"><FONT FACE="DejaVu Sans">TCGA</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="LEFT"><FONT FACE="DejaVu Sans" COLOR="#000000">rnaseq</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="CENTER" SDVAL="3" SDNUM="1033;0;@"><FONT FACE="DejaVu Sans">3</FONT></TD>
		<TD STYLE="border-right: 1px solid #000000" ALIGN="LEFT"><FONT FACE="DejaVu Sans">public</FONT></TD>
	</TR>
	<TR>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" HEIGHT="16" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans">TCGA</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans" COLOR="#000000">rnaseqv2</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="CENTER" BGCOLOR="#E6E6E6" SDVAL="3" SDNUM="1033;0;@"><FONT FACE="DejaVu Sans">3</FONT></TD>
		<TD STYLE="border-right: 1px solid #000000" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans">public</FONT></TD>
	</TR>
	<TR>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" HEIGHT="16" ALIGN="LEFT"><FONT FACE="DejaVu Sans">TCGA</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="LEFT"><FONT FACE="DejaVu Sans" COLOR="#000000">somatic_mutations</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="CENTER" SDVAL="2" SDNUM="1033;0;@"><FONT FACE="DejaVu Sans">2</FONT></TD>
		<TD STYLE="border-right: 1px solid #000000" ALIGN="LEFT"><FONT FACE="DejaVu Sans">public</FONT></TD>
	</TR>
	<TR>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" HEIGHT="16" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans">Firehose</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans" COLOR="#000000">CN_Level4</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="CENTER" BGCOLOR="#E6E6E6" SDVAL="4" SDNUM="1033;0;@"><FONT FACE="DejaVu Sans">4</FONT></TD>
		<TD STYLE="border-right: 1px solid #000000" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans">controlled</FONT></TD>
	</TR>
	<TR>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" HEIGHT="16" ALIGN="LEFT"><FONT FACE="DejaVu Sans">Georgetown</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="LEFT"><FONT FACE="DejaVu Sans" COLOR="#000000">mass_spectrometry</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="CENTER" SDVAL="4" SDNUM="1033;0;@"><FONT FACE="DejaVu Sans">4</FONT></TD>
		<TD STYLE="border-right: 1px solid #000000" ALIGN="LEFT"><FONT FACE="DejaVu Sans">public</FONT></TD>
	</TR>
	<TR>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" HEIGHT="17" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans">cgHub*</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans" COLOR="#000000">bisulfite-seq_(cghub)</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="CENTER" BGCOLOR="#E6E6E6" SDVAL="1" SDNUM="1033;0;@"><FONT FACE="DejaVu Sans">1</FONT></TD>
		<TD STYLE="border-right: 1px solid #000000" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans">controlled</FONT></TD>
	</TR>
	<TR>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" HEIGHT="17" ALIGN="LEFT"><FONT FACE="DejaVu Sans">cgHub*</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="LEFT"><FONT FACE="DejaVu Sans" COLOR="#000000">mirna-seq_(cghub)</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="CENTER" SDVAL="1" SDNUM="1033;0;@"><FONT FACE="DejaVu Sans">1</FONT></TD>
		<TD STYLE="border-right: 1px solid #000000" ALIGN="LEFT"><FONT FACE="DejaVu Sans">controlled</FONT></TD>
	</TR>
	<TR>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" HEIGHT="17" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans">cgHub*</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans" COLOR="#000000">rna-seq_(cghub)</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="CENTER" BGCOLOR="#E6E6E6" SDVAL="1" SDNUM="1033;0;@"><FONT FACE="DejaVu Sans">1</FONT></TD>
		<TD STYLE="border-right: 1px solid #000000" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans">controlled</FONT></TD>
	</TR>
	<TR>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" HEIGHT="17" ALIGN="LEFT"><FONT FACE="DejaVu Sans">cgHub*</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="LEFT"><FONT FACE="DejaVu Sans" COLOR="#000000">validation_(cghub)</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="CENTER" SDVAL="1" SDNUM="1033;0;@"><FONT FACE="DejaVu Sans">1</FONT></TD>
		<TD STYLE="border-right: 1px solid #000000" ALIGN="LEFT"><FONT FACE="DejaVu Sans">controlled</FONT></TD>
	</TR>
	<TR>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" HEIGHT="17" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans">cgHub*</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans" COLOR="#000000">wgs_(cghub)</FONT></TD>
		<TD STYLE="border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="CENTER" BGCOLOR="#E6E6E6" SDVAL="1" SDNUM="1033;0;@"><FONT FACE="DejaVu Sans">1</FONT></TD>
		<TD STYLE="border-right: 1px solid #000000" ALIGN="LEFT" BGCOLOR="#E6E6E6"><FONT FACE="DejaVu Sans">controlled</FONT></TD>
	</TR>
	<TR>
		<TD STYLE="border-bottom: 1px solid #000000; border-left: 1px solid #000000; border-right: 1px solid #000000" HEIGHT="17" ALIGN="LEFT"><FONT FACE="DejaVu Sans">cgHub*</FONT></TD>
		<TD STYLE="border-bottom: 1px solid #000000; border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="LEFT"><FONT FACE="DejaVu Sans" COLOR="#000000">wxs_(cghub)</FONT></TD>
		<TD STYLE="border-bottom: 1px solid #000000; border-left: 1px solid #000000; border-right: 1px solid #000000" ALIGN="CENTER" SDVAL="1" SDNUM="1033;0;@"><FONT FACE="DejaVu Sans">1</FONT></TD>
		<TD STYLE="border-bottom: 1px solid #000000; border-right: 1px solid #000000" ALIGN="LEFT"><FONT FACE="DejaVu Sans">controlled</FONT></TD>
	</TR>
</TABLE>
<!-- ************************************************************************** -->
</BODY>

</HTML>

* - coming soon

