from sklearn.feature_extraction.text import CountVectorizer
from sklearn.decomposition import LatentDirichletAllocation
import pyLDAvis as viz
import pyLDAvis.sklearn as s_viz


def get_topics(data, max_freq, min_occurrence, num_components):
    cv = CountVectorizer(max_df=max_freq, min_df=min_occurrence, stop_words='english')
    dtm = cv.fit_transform(data)
    model = LatentDirichletAllocation(num_components, learning_method='online', random_state=0, n_jobs=-1)
    output = model.fit_transform(dtm)
    s_viz.prepare(model, dtm, cv, mds='tsne')


def get_doc_term_matrix (data, max_freq, min_occurrence):
    return
