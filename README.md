#tcga-expedition
Provides data management and computing infrastructure to support biomedical investigation using Big Data.

#System requirements
Unix 64-bit

#Installation
<b>1. Choose storage</b>
  - PostgreSLQ: http://www.postgresql.org/download/
  - Virtuoso: http://virtuoso.openlinksw.com/dataspace/doc/dav/wiki/Main/VOSDownload
  <p>
  
<b>2. Configure</b>
<p>
Go through <b><i>resources/tcgaexpedition.conf</i></b> file and add required fields.




#Example:

download Clinical data for acc:

java -jar tcgaExpedition.jar acc clinical public
