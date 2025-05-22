import sys
import os
import traceback
import logging
import importlib.util

logging.basicConfig(level=logging.DEBUG)

script_dir = os.path.dirname(__file__)
sys.path.insert(0, script_dir)

def convert_nsz_to_nsp(input_file, output_dir):
    sys.argv = [
        "squirrel.py",
        "-i", input_file,
        "-o", output_dir,
        "--verify",
        "--extract"
    ]
    print(f"[DEBUG] sys.argv: {sys.argv}")
    print(f"[DEBUG] input exists: {os.path.exists(input_file)}")
    print(f"[DEBUG] output writable: {os.access(output_dir, os.W_OK)}")

    try:
        # Load squirrel.py as a module
        spec = importlib.util.find_spec("squirrel")
        if spec is None:
            raise ImportError("squirrel module not found")

        squirrel = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(squirrel)

        # Call main() if defined
        if hasattr(squirrel, "main"):
            squirrel.main()
        else:
            print("[NSC_Builder ERROR] squirrel.py has no 'main' function")
    except Exception as e:
        print(f"[NSC_Builder ERROR] {type(e).__name__}: {e}")
        traceback.print_exc()