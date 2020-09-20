# Pipeline

The pipeline is somewhat manual so we can inspect results
as we proceed.

1. Split the LORs into pages. We do this for two reasons:
    * OCR seems to work better on individual pages
    * Headers and Footers are on each page of a letter and
      parsing them out is easier if we split the pages apart
      
    ```sh
   java \
   -Dconfig.file=/home/mforkin/devel/src/LORGenderStudy/conf/application.conf \
   -cp target/ocr-data-pipeline-0.0.1-SNAPSHOT-SHADED.jar \
   "com.greenleaf.lor.ocr.pipeline.apps.SplitPdfApp"
   ```
1. Convert each pdf into an image. For this we use imagemagick. We've used two
   methods with decent success.
    * This was our final method as OCR performed better on these output images.
    ```sh
     ls *.pdf | xargs -I{} -P7 sh -c "convert -density 600 -trim -quality 100 -flatten -sharpen 0x1.0 -black-threshold 50% -white-threshold 50% '{}' '{}.jpg'"
    ```
    * This was an intermediate method that produced decent results. 
    ```sh
     ls *.pdf | xargs -I{} -P7 sh -c "convert -density 600 -trim -brightness-contrast 5x0 '{}' -set colorspace Gray -separate -average -depth 8 -strip '{}.png'"
    ```
1. Convert each image into a text file. We leverage the java [Tess4j](http://tess4j.sourceforge.net/) library to 
   convert the pdf images to text files. This is necessary because a majority of the pdfs are backed by an image and not
   plain text. More information the code can be found [here](https://github.com/tesseract-ocr). It was originally
   developed by HP in the 1980s but is now open source with development sponsored by google.
```sh
java \
   -Dconfig.file=/home/mforkin/devel/src/LORGenderStudy/conf/application.conf \
   -cp target/ocr-data-pipeline-0.0.1-SNAPSHOT-SHADED.jar \
   "com.greenleaf.lor.ocr.pipeline.apps.ConvertApp"
```
1. Filter out "forms". Some LORs are actually a standard form with the first two pages being a questionnaire. We filter
   out the questionnaire so it doesn't affect our results. Additionally, some letters of recommendation are just
   single sentences that say the recommender filled out the questionnaire. We also filter these out as they are not
   useful.
 ```sh
 java \
    -Dconfig.file=/home/mforkin/devel/src/LORGenderStudy/conf/application.conf \
    -cp target/ocr-data-pipeline-0.0.1-SNAPSHOT-SHADED.jar \
    "com.greenleaf.lor.ocr.pipeline.apps.FilterFormsApp"
 ```  

