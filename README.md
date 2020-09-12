# LOR Gender Study

The purpose of the study is to examine the LORs for biases based on gender or race.

This study will use topic modeling techniques to identify themes in the various groups
of applicants. 

Additionally, we will embed the various applicants based on their LORs and exploit
that space to predict which applicants are male / female based solely on their LORs.

Lastly we will train a classification model to predict the gender of an applicant.

### Note 
For each section below, a document could be considered either:
* A single LOR
* LORs concated together for a single candidate


## Topic Modeling

#### Method 1
* Divide the LORs by group (group: gender, race, etc)
* Extract topics for each group
* Compare the topics extracted

#### Method 2
* Extract topics for entire corpus
* For each document (LOR) find the n topic with highest affinity
* For each group, aggregate topic affinities by:
    * affinity score
    * count

## Embedding

* Create fast text embeddings for each document
    * Done by averaging the word vectors in the document

* Create an average group vector from some number of known samples in a group
* Predict by taking document vector distance from average group vector.
    * Could just take closest one
    * Could weight by confidence by how close you are to the average
    * Experiment could be setup by:
        * using each LOR as a separate document, but
          knowing that they are the same candidate
        * each doc is a LOR, know nothing else
        * each set of LORs is a document

## Classification Model

Pick a couple and see how they do!

* Logistic Regression
* Naive Bayes
* SGD
* KNN
* Decision Tree
* Random Forest
* SVM
* NN

