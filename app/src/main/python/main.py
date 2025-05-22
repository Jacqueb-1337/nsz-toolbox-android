import sys
import os
import traceback

def convert_nsz_to_nsp(input_file, output_dir):
    sys.argv = [
        "squirrel.py",
        "-i", input_file,
        "-o", output_dir,
        "--verify", input_file,
        "--extract", input_file
    ]
    print(f"[DEBUG] sys.argv: {sys.argv}")
    print(f"[DEBUG] input exists: {os.path.exists(input_file)}")
    print(f"[DEBUG] output writable: {os.access(output_dir, os.W_OK)}")

    try:
        script_dir = os.path.dirname(__file__)
        squirrel_path = os.path.join(script_dir, "squirrel.py")
        with open(squirrel_path, 'r') as f:
            code = compile(f.read(), squirrel_path, 'exec')
            exec(code, {'__name__': '__main__'})
    except Exception as e:
        print(f"[NSC_Builder ERROR] {type(e).__name__}: {e}")
        traceback.print_exc()