import sys
import os
import traceback
import logging
import builtins
import types
import warnings

# Disable input to avoid crashes
builtins.input = lambda *args, **kwargs: None

# Enable debug logging
logging.basicConfig(level=logging.DEBUG)

# Fake `curses` for compatibility
curses = types.ModuleType("curses")
curses.initscr = lambda: None
curses.endwin = lambda: None
curses.wrapper = lambda func, *args, **kwargs: func(*args, **kwargs)
curses.newwin = lambda *a, **kw: None
curses.noecho = lambda: None
curses.cbreak = lambda: None
curses.echo = lambda: None
curses.nocbreak = lambda: None
curses.curs_set = lambda x: None
curses.start_color = lambda: None
curses.init_pair = lambda a, b, c: None
curses.color_pair = lambda x: 0
curses.has_colors = lambda: False
curses.has_key = lambda x: False
curses.A_NORMAL = 0
curses.A_BOLD = 1
curses.A_UNDERLINE = 2
curses.A_REVERSE = 4
curses.A_BLINK = 8
curses.A_DIM = 16
curses.A_STANDOUT = 32

key_names = [
    "KEY_DOWN", "KEY_UP", "KEY_LEFT", "KEY_RIGHT", "KEY_HOME", "KEY_BACKSPACE",
    "KEY_ENTER", "KEY_DC", "KEY_IC", "KEY_NPAGE", "KEY_PPAGE", "KEY_END",
    "KEY_EXIT", "KEY_PRINT", "KEY_RESIZE", "KEY_MOUSE", "KEY_F0"
] + [f"KEY_F{i}" for i in range(1, 64)]
for i, key in enumerate(key_names):
    setattr(curses, key, 1000 + i)

curses_has_key = types.ModuleType("curses.has_key")
curses_has_key._capability_names = {}
sys.modules["curses"] = curses
sys.modules["curses.has_key"] = curses_has_key

warnings.filterwarnings("ignore", category=DeprecationWarning)

# Import NSC_Builder CLI (squirrel.py)
try:
    from NSC_Builder import squirrel
except Exception as e:
    print(f"[IMPORT ERROR] Failed to import NSC_Builder: {e}")
    traceback.print_exc()
    raise

def convert_nsz_to_nsp(input_file, output_dir):
    sys.argv = [
        "squirrel.py",
        "-i", input_file,
        "-o", output_dir,
        "--verify",
        "--extract",
        "--no-autoremove",
        "--C"
    ]

    print(f"[DEBUG] sys.argv: {sys.argv}")
    print(f"[DEBUG] input exists: {os.path.exists(input_file)}")
    print(f"[DEBUG] output writable: {os.access(output_dir, os.W_OK)}")

    try:
        squirrel.main()
    except Exception as e:
        print(f"[NSC_Builder ERROR] {type(e).__name__}: {e}")
        traceback.print_exc()