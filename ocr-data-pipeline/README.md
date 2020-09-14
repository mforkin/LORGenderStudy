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
1. Convert each image into a text file.
