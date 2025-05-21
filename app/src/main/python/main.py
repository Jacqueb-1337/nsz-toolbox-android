# app/src/main/python/main.py

import builtins
builtins.input = lambda *args, **kwargs: None  # Disable any call to input()

import nsz

def convert_nsz_to_nsp(input_file, output_dir):
    args = [
        input_file,
        "-D",  # Decompress to NSP
        "--out", output_dir
    ]
    nsz.main(args)