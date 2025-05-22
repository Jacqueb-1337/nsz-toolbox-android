import sys
import types
import builtins
import warnings
import traceback
import os
import logging

# Disable input() to prevent EOFError
builtins.input = lambda *args, **kwargs: None

# Set up debug-level logging to capture NSZ internal messages
logging.basicConfig(level=logging.DEBUG)

# Create mock curses module
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

# Add KEY_ constants
key_names = [
    "KEY_DOWN", "KEY_UP", "KEY_LEFT", "KEY_RIGHT", "KEY_HOME", "KEY_BACKSPACE",
    "KEY_DL", "KEY_IL", "KEY_DC", "KEY_IC", "KEY_EIC", "KEY_CLEAR", "KEY_EOS",
    "KEY_EOL", "KEY_SF", "KEY_SR", "KEY_NPAGE", "KEY_PPAGE", "KEY_STAB", "KEY_CTAB",
    "KEY_CATAB", "KEY_ENTER", "KEY_SRESET", "KEY_RESET", "KEY_PRINT", "KEY_LL",
    "KEY_A1", "KEY_A3", "KEY_B2", "KEY_C1", "KEY_C3", "KEY_BTAB", "KEY_BEG",
    "KEY_CANCEL", "KEY_CLOSE", "KEY_COMMAND", "KEY_COPY", "KEY_CREATE", "KEY_END",
    "KEY_EXIT", "KEY_FIND", "KEY_HELP", "KEY_MARK", "KEY_MESSAGE", "KEY_MOVE",
    "KEY_NEXT", "KEY_OPEN", "KEY_OPTIONS", "KEY_PREVIOUS", "KEY_REDO",
    "KEY_REFERENCE", "KEY_REFRESH", "KEY_REPLACE", "KEY_RESTART", "KEY_RESUME",
    "KEY_SAVE", "KEY_SBEG", "KEY_SCANCEL", "KEY_SCOMMAND", "KEY_SCOPY",
    "KEY_SCREATE", "KEY_SDC", "KEY_SDL", "KEY_SELECT", "KEY_SEND", "KEY_SEOL",
    "KEY_SEXIT", "KEY_SFIND", "KEY_SHELP", "KEY_SHOME", "KEY_SIC", "KEY_SLEFT",
    "KEY_SMESSAGE", "KEY_SMOVE", "KEY_SNEXT", "KEY_SOPTIONS", "KEY_SPREVIOUS",
    "KEY_SPRINT", "KEY_SREDO", "KEY_SREPLACE", "KEY_SRESET", "KEY_SRIGHT",
    "KEY_SRSUME", "KEY_SSAVE", "KEY_SSUSPEND", "KEY_STAB", "KEY_SUNDO",
    "KEY_SUSPEND", "KEY_UNDO", "KEY_MOUSE", "KEY_RESIZE", "KEY_EVENT",
    "KEY_MAX", "KEY_MIN"
] + [f"KEY_F{i}" for i in range(1, 64)]

for i, key in enumerate(key_names):
    setattr(curses, key, 1000 + i)

# Fake submodule 'curses.has_key'
curses_has_key = types.ModuleType("curses.has_key")
curses_has_key._capability_names = {}
sys.modules["curses"] = curses
sys.modules["curses.has_key"] = curses_has_key

warnings.filterwarnings("ignore", category=DeprecationWarning)

import nsz

def convert_nsz_to_nsp(input_file, output_dir):
    sys.argv = [
        "nsz", "-D", "--extract", "--verify", "--out", output_dir, input_file
    ]
    print(f"[DEBUG] sys.argv: {sys.argv}")
    print(f"[DEBUG] input exists: {os.path.exists(input_file)}")
    print(f"[DEBUG] output writable: {os.access(output_dir, os.W_OK)}")

    try:
        nsz.main()
    except Exception as e:
        print(f"[NSZ ERROR] {type(e).__name__}: {e}")
        traceback.print_exc()