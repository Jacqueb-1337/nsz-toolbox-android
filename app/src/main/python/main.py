import sys
import os
import traceback
import logging
import runpy
import shutil

# Set up debug-level logging
logging.basicConfig(level=logging.DEBUG)

# Adjust the system path to include the directory containing squirrel.py
ztools_path = os.path.join(os.path.dirname(__file__), 'NSC_Builder', 'py', 'ztools')
sys.path.insert(0, ztools_path)

# Accept prod.keys or title.keys as shared input
def maybe_import_keys(shared_path):
    if shared_path.endswith(("prod.keys", "title.keys")):
        keys_dest = os.path.join(ztools_path, "title.keys")
        try:
            shutil.copyfile(shared_path, keys_dest)
            print(f"[KEYS] Imported key file to {keys_dest}")
            return True
        except Exception as e:
            print(f"[KEYS ERROR] Failed to copy key file: {e}")
    return False

def convert_nsz_to_nsp(input_file, output_dir):
    keys_file = os.path.join(ztools_path, "title.keys")
    if not os.path.exists(keys_file):
        print("[KEYS] Missing title.keys or prod.keys, skipping conversion.")
        return

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
        runpy.run_path(os.path.join(ztools_path, "squirrel.py"), run_name="__main__")
    except Exception as e:
        print(f"[NSC_Builder ERROR] {type(e).__name__}: {e}")
        traceback.print_exc()