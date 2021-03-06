import spacy
import nltk
import os
import key_parser as kp

nltk.download('stopwords')
en_stop = set([word.lower() for word in nltk.corpus.stopwords.words('english')])
en = spacy.load('en')


def clean_text(text, min_length=1):
    # lowercase
    text = text.lower()

    # filter out stop words and words that are too small
    text = " ".join([word for word in text.split(" ") if len(word) > min_length and word not in en_stop])

    words = en(text)
    return " ".join([word.lemma_ for word in words if word.lemma_ != '-PRON-'])


def get_text(path):
    res = {}
    for subdir, dirs, files in os.walk(path):
        for f in files:
            f_name = subdir + os.sep + f
            with open(f_name, 'r') as fi:
                txt = clean_text(" ".join(fi.readlines()))
                key_metadata = kp.key[f[:-5]]
                res[f] = {
                    'txt': txt,
                    'metadata': key_metadata
                }
    return res
