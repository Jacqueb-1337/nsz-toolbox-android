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
curses.KEY_DOWN = 1000
curses.KEY_UP = 1001
curses.KEY_LEFT = 1002
curses.KEY_RIGHT = 1003
curses.KEY_HOME = 1004
curses.KEY_BACKSPACE = 1005
curses.KEY_F0 = 1006
curses.KEY_DL = 1007
curses.KEY_IL = 1008
curses.KEY_DC = 1009
curses.KEY_IC = 1010
curses.KEY_EIC = 1011
curses.KEY_CLEAR = 1012
curses.KEY_EOS = 1013
curses.KEY_EOL = 1014
curses.KEY_SF = 1015
curses.KEY_SR = 1016
curses.KEY_NPAGE = 1017
curses.KEY_PPAGE = 1018
curses.KEY_STAB = 1019
curses.KEY_CTAB = 1020
curses.KEY_CATAB = 1021
curses.KEY_ENTER = 1022
curses.KEY_SRESET = 1023
curses.KEY_RESET = 1024
curses.KEY_PRINT = 1025
curses.KEY_LL = 1026
curses.KEY_A1 = 1027
curses.KEY_A3 = 1028
curses.KEY_B2 = 1029
curses.KEY_C1 = 1030
curses.KEY_C3 = 1031
curses.KEY_BTAB = 1032
curses.KEY_BEG = 1033
curses.KEY_CANCEL = 1034
curses.KEY_CLOSE = 1035
curses.KEY_COMMAND = 1036
curses.KEY_COPY = 1037
curses.KEY_CREATE = 1038
curses.KEY_END = 1039
curses.KEY_EXIT = 1040
curses.KEY_FIND = 1041
curses.KEY_HELP = 1042
curses.KEY_MARK = 1043
curses.KEY_MESSAGE = 1044
curses.KEY_MOVE = 1045
curses.KEY_NEXT = 1046
curses.KEY_OPEN = 1047
curses.KEY_OPTIONS = 1048
curses.KEY_PREVIOUS = 1049
curses.KEY_REDO = 1050
curses.KEY_REFERENCE = 1051
curses.KEY_REFRESH = 1052
curses.KEY_REPLACE = 1053
curses.KEY_RESTART = 1054
curses.KEY_RESUME = 1055
curses.KEY_SAVE = 1056
curses.KEY_SBEG = 1057
curses.KEY_SCANCEL = 1058
curses.KEY_SCOMMAND = 1059
curses.KEY_SCOPY = 1060
curses.KEY_SCREATE = 1061
curses.KEY_SDC = 1062
curses.KEY_SDL = 1063
curses.KEY_SELECT = 1064
curses.KEY_SEND = 1065
curses.KEY_SEOL = 1066
curses.KEY_SEXIT = 1067
curses.KEY_SFIND = 1068
curses.KEY_SHELP = 1069
curses.KEY_SHOME = 1070
curses.KEY_SIC = 1071
curses.KEY_SLEFT = 1072
curses.KEY_SMESSAGE = 1073
curses.KEY_SMOVE = 1074
curses.KEY_SNEXT = 1075
curses.KEY_SOPTIONS = 1076
curses.KEY_SPREVIOUS = 1077
curses.KEY_SPRINT = 1078
curses.KEY_SREDO = 1079
curses.KEY_SREPLACE = 1080
curses.KEY_SRESET = 1081
curses.KEY_SRIGHT = 1082
curses.KEY_SRSUME = 1083
curses.KEY_SSAVE = 1084
curses.KEY_SSUSPEND = 1085
curses.KEY_SUNDO = 1086
curses.KEY_SUSPEND = 1087
curses.KEY_UNDO = 1088
curses.KEY_MOUSE = 1089
curses.KEY_RESIZE = 1090
curses.KEY_EVENT = 1091
curses.KEY_MAX = 1092
curses.KEY_MIN = 1093

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