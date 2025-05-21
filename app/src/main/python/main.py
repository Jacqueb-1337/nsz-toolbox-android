import sys
import types
import builtins
import warnings

# Disable input to avoid crashes in non-interactive terminals
builtins.input = lambda *args, **kwargs: None

# Create fake curses module (as a package)
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

# Also create a submodule for 'curses.has_key' to prevent import errors
curses_has_key = types.ModuleType("curses.has_key")
sys.modules["curses"] = curses
sys.modules["curses.has_key"] = curses_has_key

# Suppress deprecation warnings
warnings.filterwarnings("ignore", category=DeprecationWarning)

# Now safely import and run NSZ
import nsz

def convert_nsz_to_nsp(input_file, output_dir):
    args = [
        input_file,
        "-D",
        "--out", output_dir
    ]
    nsz.main(args)