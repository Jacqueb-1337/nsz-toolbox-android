# app/src/main/python/main.py

import sys
import types
import warnings

# Disable input() to avoid EOFError on Android
__builtins__.input = lambda *args, **kwargs: None

# Dummy curses module to prevent crash on Android
sys.modules["curses"] = types.SimpleNamespace(
    initscr=lambda: None,
    endwin=lambda: None,
    wrapper=lambda func, *args, **kwargs: func(*args, **kwargs),
    newwin=lambda *a, **kw: None,
    noecho=lambda: None,
    cbreak=lambda: None,
    echo=lambda: None,
    nocbreak=lambda: None,
    curs_set=lambda x: None,
    start_color=lambda: None,
    init_pair=lambda a, b, c: None,
    color_pair=lambda x: 0,
    has_colors=lambda: False,
)

# Suppress DeprecationWarnings
warnings.filterwarnings("ignore", category=DeprecationWarning)

# NSZ main
import nsz

def convert_nsz_to_nsp(input_file, output_dir):
    args = [
        input_file,
        "-D",  # Decompress to NSP
        "--out", output_dir
    ]
    nsz.main(args)