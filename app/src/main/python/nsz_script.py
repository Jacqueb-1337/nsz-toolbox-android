import nsz
import os
import tempfile

def decompress(input_stream, output_folder_uri):
    temp_path = tempfile.NamedTemporaryFile(delete=False).name
    with open(temp_path, "wb") as f:
        f.write(input_stream.read())

    output_path = os.path.join("/storage/emulated/0/Download/NSZ_Converted/", os.path.basename(temp_path).replace(".nsz", ".nsp"))
    nsz.decompress(temp_path, output_path)
    return output_path