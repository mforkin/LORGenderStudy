from ..lib import text_utils as tu
from ..lib import data_utils as du
from ..lib import topic_model_utils as tmu
import pandas as pd

text_data_path = "/home/mforkin/LOR/processed-data/singleDocPath"

data = tu.get_text(text_data_path)

df = pd.DataFrame.from_dict(data, orient='index')

topics = tmu.get_topics(data['txt'])

genders = du.group_by(data, du.get_gender)
is_white = du.group_by(data, du.is_white)



