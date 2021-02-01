import csv

key_path = '/home/mforkin/LOR/data/key.csv'


def parse_key():
    with open(key_path, 'r') as f:
        reader = csv.reader(f, delimiter=',')
        lines = [line for line in reader]
        headers_raw = enumerate([header.lower() for header in lines[0]])
        headers_map = {}
        for i, h in headers_raw:
            headers_map[h] = i
        metadata = {}
        [handle_line(line, metadata, headers_map) for line in lines[1:]]
        return metadata


def handle_line(line, metadata, header_map):
    id = line[0]
    user_data = {}
    if id in metadata:
        user_data = metadata[id]
    else:
        user_data = extract_metadata(line, header_map)
    metadata[id] = user_data
    lor_data = extract_lor_data(line, header_map)
    metadata[id]['lor_metadata'].append(lor_data)


def extract_metadata(line, header_map):
    return {
        'participant_number': line[header_map['participant number']],
        'medical_school': line[header_map['medical school']],
        'gender': line[header_map['gender']],
        'race': line[header_map['race (self identification)']],
        'aoa': line[header_map['alpha omega alpha (aoa) status']],
        'step1': line[header_map['usmle score (step 1)']],
        'step2': line[header_map['usmle score (step 2)']],
        'birth_date': line[header_map['birth date']],
        'interview_data': line[header_map['interview date']],
        'lor_metadata': []
    }


def extract_lor_data(line, header_map):
    return {
        'file_name': line[header_map['letter of recommendation file']],
        'writer_metadata': {
            'name': line[header_map["letter writer's name"]],
            'rank': line[header_map["letter writer's academic rank"]],
            'is_program_director': line[header_map['anesthesiology residency program director']] == 'yes',
        }
    }


key = parse_key()
