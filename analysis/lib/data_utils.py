

def is_white(metadata, file_name):
    return metadata['race'] == 'white'


def get_gender(metadata, file_name):
    return metadata['gender']


def group_by(data, fn):
    res = {}
    for d in data:
        group = fn(d.metadata)
        res[group] = d
    return res
