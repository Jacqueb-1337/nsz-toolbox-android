import nsz
import sys

def convert_nsz_to_nsp(input_path, output_dir):
    sys.argv = [
        "nsz",
        input_path,
        "--outdir", output_dir,
        "--verify",
        "--overwrite"
    ]
    nsz.main()
