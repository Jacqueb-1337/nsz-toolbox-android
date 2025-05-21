import types
import sys
import builtins
import warnings

# Disable input
builtins.input = lambda *args, **kwargs: None

# Create a real dummy module for curses
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

sys.modules["curses"] = curses

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