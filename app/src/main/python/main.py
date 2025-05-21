import sys
import types
import builtins
import warnings

# Disable input to avoid terminal crashes
builtins.input = lambda *args, **kwargs: None

# Create a dummy 'curses' module
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

# Define common attributes used in curses applications
curses.A_NORMAL = 0
curses.A_BOLD = 1
curses.A_UNDERLINE = 2
curses.A_REVERSE = 4
curses.A_BLINK = 8
curses.A_DIM = 16
curses.A_STANDOUT = 32

# Define common key constants
curses.KEY_UP = 259
curses.KEY_DOWN = 258
curses.KEY_LEFT = 260
curses.KEY_RIGHT = 261
curses.KEY_ENTER = 10
curses.KEY_BACKSPACE = 127
curses.KEY_DC = 330
curses.KEY_HOME = 262
curses.KEY_END = 360
curses.KEY_NPAGE = 338
curses.KEY_PPAGE = 339
curses.KEY_IC = 331
curses.KEY_EIC = 332

# Create a fake submodule 'curses.has_key' with expected attributes
curses_has_key = types.ModuleType("curses.has_key")
curses_has_key._capability_names = {}

# Register both modules
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