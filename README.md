# Kelastika
Elastika writen in Kotlin

### Installation:

 Install and start Elastic Search
 ```sh
 ➜ brew install elasticsearch
 ➜ elasticsearch
 ```
Import project into IntelliJ IDEA.
If you want to run the project just do type ```CMD + ALT + R, RIGHT, EDIT``` and check that the following fields are correct:

  - Main class:  ```com.irontec.KelastikaKt```
  - Program arguments (you can change them): ```-p 9200 -i irontecdocs -t irondocs --file test.pdf```
  - Working directory:  ```your_path/kelastika/test```
  - Apply
  - Run

You can also generate a .jar file. Just go to ```Module - Artifacts - Add (+)``` and select ```JAR - From modules with dependencies - Select Kelastika + Ok```
Then, generate the JAR just by clicking in ```Build + Build artifacts```. This will generate a out folder with the jar file, move that jar file to ```your_path/kelastika/test``` and you're ready to go.

### How it works:
  - The program will extract the contents and metadata from the specified file
  - It will check if the especified ```ìndex``` and ```mapping```exist and if not it will try to create them.
  - If everything goes fine, it will post the data to the Elastic Search instance.

```
Hostname http://localhost

#Extractic metadata
Executing: java -jar tika-app.jar -j test.pdf

#Extractic content
Executing: java -jar tika-app.jar -T test.pdf

#Data object that will be posted as new documento to Elastic Search
{"pdf:PDFVersion":"1.5","X-Parsed-By":["org.apache.tika.parser.DefaultParser","org.apache.tika.parser.pdf.PDFParser"],"access_permission:modify_annotations":"true","access_permission:can_print_degraded":"true","access_permission:assemble_document":"true","access_permission:extract_for_accessibility":"true","xmpTPg:NPages":"1","resourceName":"test.pdf","dc:format":"application/pdf; version=1.5","access_permission:extract_content":"true","content":"","access_permission:can_print":"true","access_permission:fill_in_form":"true","pdf:encrypted":"false","producer":"Skia/PDF m61","Content-Length":"16134","access_permission:can_modify":"true","Content-Type":"application/pdf"}

#Index check
check --> http://localhost:9200/irontecdocs/

#Index creation
create --> http://localhost:9200/irontecdocs/
create body --> {"settings":{"index":{"number_of_shards":3,"number_of_replicas":2}}}

#Mapping creation
create --> http://localhost:9200/irontecdocs/_mapping/irondocs/
create body --> {"properties":{"content":{"type":"text"}}}

#Post document to Elastic Search
post --> http://localhost:9200/irontecdocs/irondocs/

#Result OK
{"result":"created","_shards":{"total":3,"failed":0,"successful":1},"_index":"irontecdocs","created":true,"_type":"irondocs","_id":"AVzzKj2n_xhkoT6fMb8X","_version":1}

Process finished with exit code 0
```

### License

[EUPL v1.1](https://raw.githubusercontent.com/axierjhtjz/kelastika/master/LICENSE)

```
Copyright 2015 Asier Fernandez

Licensed under the EUPL, Version 1.1 or - as soon they will be approved by the European
Commission - subsequent versions of the EUPL (the "Licence"); You may not use this work
except in compliance with the Licence.

You may obtain a copy of the Licence at:
http://ec.europa.eu/idabc/eupl.html

Unless required by applicable law or agreed to in writing, software distributed under 
the Licence is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF 
ANY KIND, either express or implied. See the Licence for the specific language 
governing permissions and limitations under the Licence.
```