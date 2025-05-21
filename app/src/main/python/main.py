# app/src/main/python/main.py

import nsz

__builtins__.input = lambda *args, **kwargs: None
def convert_nsz_to_nsp(input_file, output_dir):
    args = [
        input_file,
        "-D",  # Decompress to NSP
        "--out", output_dir
    ]
    nsz.main(args)