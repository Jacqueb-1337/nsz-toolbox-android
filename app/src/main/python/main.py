import sys
import os
import traceback
import logging

# Set up debug-level logging
logging.basicConfig(level=logging.DEBUG)

# Adjust the system path to include the directory containing squirrel.py
squirrel_path = os.path.join(os.path.dirname(__file__), 'NSC_Builder', 'py', 'ztools')
sys.path.insert(0, squirrel_path)

try:
    import squirrel
except ImportError as e:
    print(f"[IMPORT ERROR] Failed to import squirrel: {e}")
    traceback.print_exc()
    raise

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
        squirrel.main()
    except Exception as e:
        print(f"[NSC_Builder ERROR] {type(e).__name__}: {e}")
        traceback.print_exc()