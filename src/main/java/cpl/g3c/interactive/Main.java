package cpl.g3c.interactive;

import java.io.*;
import java.lang.*;
import java.util.*;
import org.apache.commons.cli.*;

import jtabwb.engine.RuleType;
import jtabwb.engine._AbstractRule;
import jtabwb.engine._ClashDetectionRule;
import jtabwb.engine._RegularRule;
import jtabwbx.prop.formula._Sequent;

public class Main {

    // The name of this program.
    private static String ME = "jtabwb-g3c-top";

    // Defaults:
    private static final boolean defExitOnError = false;
    private static final boolean defMute = false;
    private static final PrintStream defOutput = System.out;
    private static final String defOutputFile = "(stdout)";
    private static final String defPrompt = "> ";
    private static final boolean defVerbose = false;

    // Options:
    private static boolean optExitOnError = defExitOnError;
    private static boolean optMute = defMute;
    private static PrintStream optOutput = defOutput;
    private static String optOutputFile = defOutputFile;
    private static String optPrompt = defPrompt;
    private static boolean optVerbose = defVerbose;

    // Prover.
    private static Prover prover = new Prover ();

    private static void outputln () {
        optOutput.println ();
        optOutput.flush ();
    }

    private static void outputln (Object o) {
        optOutput.println (o);
        optOutput.flush ();
    }

    private static void output (Object o) {
        optOutput.print (o);
        optOutput.flush ();
    }

    private static void output (String fmt, Object... args) {
        optOutput.format (fmt, args);
        optOutput.flush ();
    }

    private static void errorln () {
        System.err.print ("error: ");
        System.err.println ();
    }

    private static void errorln (Object o) {
        System.err.print ("error: ");
        System.err.println (o);
    }

    private static void error (Object o) {
        System.err.print ("error: ");
        System.err.print (o);
    }

    private static void error (String fmt, Object... args) {
        System.err.print ("error: ");
        System.err.format (fmt, args);
    }

    private static void infoln () {
        if (!optMute) {
            System.out.println ();
        }
    }

    private static void infoln (Object o) {
        if (!optMute) {
            System.out.println (o);
        }
    }

    private static void info (Object o) {
        if (!optMute) {
            System.out.print (o);
        }
    }

    private static void info (String fmt, Object... args) {
        if (!optMute) {
            System.out.format (fmt, args);
        }
    }

    private static void usage (String fmt, Object... args) {
        error (fmt, args);
        infoln ("use :? for help");
    }

    private static void verboseln () {
        if (optVerbose) {
            infoln ();
        }
    }

    private static void verboseln (Object o) {
        if (optVerbose) {
            infoln (o);
        }
    }

    private static void verbose (Object o) {
        if (optVerbose) {
            info (o);
        }
    }

    private static void verbose (String fmt, Object... args) {
        if (optVerbose) {
            info (fmt, args);
        }
    }

    private static long _verboseStartTime = 0;
    private static void verboseStart (String fmt, Object... args) {
        verbose (fmt, args);
        _verboseStartTime = System.currentTimeMillis ();
    }

    private static void verboseEnd () {
        long dt = System.currentTimeMillis () - _verboseStartTime;
        verbose ("done [" + ms2Str (dt) + "]\n");
    }

    public static String version () {
        String ver = null;
        try {
            Class cls = Class.forName ("cpl.g3c.interactive.Main");
            ver = cls.getPackage ().getImplementationVersion ();
        } catch (ClassNotFoundException e) {
                                // no-op
        }
        return ME + " v" + (ver == null ? "(unknown)" : ver);
    }

    // Converts a number of milliseconds into a hh:mm:ss.mmm time measure.
    private static String ms2Str (long ms) {
        if (ms < 0) {
            return "99:99:99.999";
        } else {
            return String.format ("%d:%02d:%02d.%03d",
                                  ms / (1000 * 60 * 60),
                                  (ms / 1000 * 60) % 60,
                                  (ms / 1000) % 60,
                                  ms % 1000);
        }
    }

    // Splits string into head and tail.
    private static String[] unconsStr (String str, char sep) {
        int i = str.indexOf (sep);
        if (i <= 0) {
            return new String[] {str, ""};
        } else {
            return new String[] {
                str.substring (0, i),
                str.substring (i).trim ()
            };
        }
    }

    // Reads file contents as string.
    private static String getContentsAsString (String path)
        throws IOException {
        BufferedReader reader = new BufferedReader (new FileReader (path));
        String line = null;
        StringBuilder sb = new StringBuilder ();
        String ls = System.getProperty ("line.separator");
        try {
            while ((line = reader.readLine ()) != null) {
                sb.append (line);
                sb.append (ls);
            }
            return sb.toString ();
        } finally {
            reader.close ();
        }
    }

    // -- COMMANDS -- //

    // :clear
    private static boolean cmdClear () {
        verboseStart ("resetting prover...");
        prover.reset ();
        verboseEnd ();
        return true;
    }

    // :echo <string>
    private static boolean cmdEcho (String str) {
        outputln (str);
        return true;
    }

    // :help
    private static boolean cmdHelp (String arg) {
        if (cmdHelpOption (arg)) {
            return true;
        }
        output
            ("Basic commands:\n" +
             "  # ...                         comment (no-op)\n" +
             "  ! <command>                   run command and negate its status\n" +
             "  :                             repeat last command\n" +
             "  :{\\n ...lines... \\n:}\\n       multi-line command\n" +
             "  :clear                        reset prover state\n" +
             "  :echo <string>                echo <string> to output\n" +
             "  :help, :?                     show this list of commands\n" +
             "  :include <file>                run " + ME + " commands from <file>\n" +
             "  :load <string>                load sequent from <string> into prover\n" +
             "  :quit [<status>]              exit " + ME + "with <status>\n" +
             "  :shell <string>               run shell <string>\n" +
             "  :status                       show the status of last command\n" +
             "  :time [s|ms]                  show the time it took to run last command\n" +
             "  :version                      show " + ME + "version\n" +
             "\n" +
             "Proof commands:\n" +
             "  :goals [<goal>]               show <goal>\n" +
             "  :rules [<goal>]               show rules applicable to <goal>\n" +
             "  :refine <goal.rule>           apply <rule> to <goal>\n" +
             "\n" +
             "Options:\n" +
             "  :get [<option>]               get the current value of <option>\n" +
             "  :help <option>                show the available values for option\n" +
             "  :set exit-on-error [on|*off]  exit immediately on any error\n" +
             "  :set mute [on|*off]           be quiet\n" +
             "  :set output <file>            redirect the output of commands to <file>\n" +
             "  :set prompt \"<string>\"        set the prompt used in " + ME + "\n" +
             "  :set verbose [on|*off]        be verbose\n" +
             "  :unset <option>               reset <option> to its default value\n" +
             "  :unset-all                    reset all options to their default values\n" +
             "");
        return true;
    }

    // :include <file>
    private static BufferedReader cmdInclude (String file) {
        verboseStart ("including commands from '%s'...", file);
        try {
            String script = getContentsAsString (file);
            BufferedReader br = new BufferedReader (new StringReader (script));
            verboseEnd ();
            return br;
        } catch (IOException e) {
            errorln (e.getMessage ());
            return null;
        }
    }

    // :load <string>
    private static boolean cmdLoad (String str) {
        if (str.length () == 0) {
            infoln ("OK, no sequent to load");
            return true;
        }
        try {
            prover.load (str);
            return true;
        } catch (ProverException e) {
            errorln ("invalid syntax: " + e.getMessage ());
            return false;
        }
    }

    // :shell <string>
    private static boolean cmdShell (String str) {
        if (str.length () == 0) {
            infoln ("OK, no shell string");
            return true;
        }
        try {
            verboseStart ("running shell...\n");
            Process proc = Runtime.getRuntime ().exec (str);
            proc.waitFor ();
            BufferedReader br = new BufferedReader
                (new InputStreamReader (proc.getInputStream ()));
            String line;
            while ((line = br.readLine ()) != null) {
                outputln (line);
            }
            br.close ();
            verboseEnd ();
            return proc.exitValue () == 0;
        } catch (IOException | InterruptedException e) {
            errorln (e.getMessage ());
            return false;
        }
    }

    // :goals [<goal>]
    private static boolean cmdGoal (int id) {
        _Sequent seq = prover.getGoal (id);
        if (seq == null) {
            errorln ("no such goal");
            return false;
        }
        outputln (id + "\t" + seq);
        return true;
    }

    private static boolean cmdGoals (String goal) {
        if (goal.length () == 0) {
            Map<Integer, _Sequent> goals = prover.getGoals ();
            if (goals.size () == 0) {
                outputln ("no more goals, proof complete");
            } else {
                for (Integer id : goals.keySet ()) {
                    cmdGoal (id);
                }
            }
            return true;
        } else {
            try {
                return (cmdGoal (Integer.parseInt (goal)));
            } catch (NumberFormatException e) {
                errorln ("no such goal");
                return false;
            }
        }
    }

    // :rules [<goal>]
    private static boolean cmdRules (int id) {
        Collection<_AbstractRule> rules = prover.getApplicableRules (id);
        if (rules == null) {
            errorln ("no such goal");
            return false;
        }
        int i = 0;
        for (_AbstractRule rule : rules) {
            output ("%d.%d\t", id, i++);
            RuleType type = RuleType.getType (rule);
            switch (type) {
            case REGULAR: {
                _RegularRule regular = (_RegularRule) rule;
                outputln (rule.name () + '\t' + regular.mainFormula ());
                break;
            }
            case CLASH_DETECTION_RULE: {
                outputln (rule.name ());
                break;
            }
            default:
                throw new RuntimeException ("should not get here");
            }
        }
        return true;
    }

    private static boolean cmdRules (String goal) {
        if (goal.length () == 0) {
            for (Map.Entry<Integer, _Sequent> x
                     : prover.getGoals ().entrySet ()) {
                outputln (x.getKey ().toString () + '\t'
                          + x.getValue ().toString ());
                cmdRules (x.getKey ());
            }
            return true;
        } else {
            try {
                return cmdRules (Integer.parseInt (goal));
            } catch (NumberFormatException e) {
                errorln ("no such goal");
                return false;
            }
        }
    }

    // :refine <goal.rule>
    private static boolean cmdRefine (String arg) {
        if (arg.length () == 0) {
            infoln ("OK, no refine target");
            return true;
        }
        String[] toks = arg.split ("\\.", 2);
        if (toks.length != 2) {
            errorln ("no such goal-rule");
            return false;
        }
        int id = -1;
        try {
            id = Integer.parseInt (toks[0]);
        } catch (NumberFormatException e) {
            errorln ("no such goal");
            return false;
        }
        int i = -1;
        try {
            i = Integer.parseInt (toks[1]);
        } catch (NumberFormatException e) {
            errorln ("no such rule");
            return false;
        }
        Collection<_AbstractRule> rules = prover.getApplicableRules (id);
        if (rules == null) {
            errorln ("no such goal");
            return false;
        }
        int j = 0;
        for (_AbstractRule rule : rules) {
            if (i == j) {
                verboseStart ("applying %s to goal %d...",
                              rule.name (), id);
                prover.refine (id, rule);
                verboseEnd ();
                return true;
            }
            j++;
        }
        errorln ("no such rule");
        return false;
    }

    // :get <option>
    private static boolean cmdGet (String option, boolean prefix) {
        if (option.length () == 0) {
            cmdGet ("exit-on-error", true);
            cmdGet ("mute", true);
            cmdGet ("output", true);
            cmdGet ("prompt", true);
            cmdGet ("verbose", true);
            return true;
        }
        String str = null;
        switch (option) {
        case "exit-on-error":
            str = optExitOnError ? "on" : "off";
            break;
        case "mute":
            str = optMute ? "on" : "off";
            break;
        case "output":
            str = optOutputFile;
            break;
        case "prompt":
            str = optPrompt;
            break;
        case "verbose":
            str = optVerbose ? "on" : "off";
            break;
        default:
            usage ("unknown option '%s'\n", option);
            return false;
        }
        if (prefix) {
            outputln (option + ": " + str);
        } else {
            outputln (str);
        }
        return true;
    }

    // :help <option>
    private static String _cmdHelpOptionBoolean (boolean def) {
        return def ? "on* | off" : "on | off*";
    }

    private static boolean cmdHelpOption (String option) {
        switch (option) {
        case "exit-on-error":
            outputln (_cmdHelpOptionBoolean (defExitOnError));
            break;
        case "mute":
            outputln (_cmdHelpOptionBoolean (defMute));
            break;
        case "output":
            output ("<file> (default: %s)\n", defOutputFile);
            break;
        case "prompt":
            output ("<string> | \"<string>\" (default: \"%s\")\n",
                    defPrompt);
            break;
        case "verbose":
            outputln (_cmdHelpOptionBoolean (defVerbose));
            break;
        default:
            return false;
        }
        return true;
    }

    // :set <option> <value>
    private static boolean cmdSet (String option, String value) {
        boolean unset = value == null;
        if (!unset && value.length () == 0) {
            infoln ("OK, no value to set");
            return true;
        }
        switch (option) {
        case "exit-on-error":
            optExitOnError = unset ? defExitOnError : value.equals ("on");
            break;
        case "mute":
            optMute = unset ? defMute : value.equals ("on");
            break;
        case "output":
            if (unset) {
                optOutput = defOutput;
                optOutputFile = defOutputFile;
            } else {
                try {
                    optOutput = new PrintStream (value);
                    optOutputFile = value;
                } catch (FileNotFoundException e) {
                    errorln (e.getMessage ());
                    return false;
                }
            }
            break;
        case "prompt":
            if (unset) {
                optPrompt = defPrompt;
            } else {
                if (value.charAt (0) == '"'
                    && value.charAt (value.length () - 1) == '"') {
                    value = value.substring (1, value.length () - 1);
                }
                optPrompt = value;
            }
            break;
        case "verbose":
            optVerbose = unset ? defVerbose : value.equals ("on");
            break;
        default:
            usage ("unknown option '%s'\n", option);
            return false;
        }
        return true;
    }

    // :unset <option>
    private static boolean cmdUnset (String option) {
        return cmdSet (option, null);
    }

    // :unset-all
    private static boolean cmdUnsetAll () {
        boolean status = true;
        status = status && cmdUnset ("exit-on-error");
        status = status && cmdUnset ("mute");
        status = status && cmdUnset ("output");
        status = status && cmdUnset ("prompt");
        status = status && cmdUnset ("verbose");
        return status;
    }

    // -- INTERPRETER LOOP -- //

    // Parses command-line options.
    private static boolean _parseCmdLineOptionsNoInteractive = false;
    private static boolean _parseCmdLineOptionsOptExitOnError = defExitOnError;
    private static boolean _parseCmdLineOptionsOptMute = defMute;
    private static BufferedReader parseCmdLineOptions (String[] args) {
        Options options = new Options ();
        options
            .addOption (OptionBuilder
                        .withLongOpt ("command")
                        .withDescription ("run command <string>")
                        .withArgName ("string")
                        .hasArg ()
                        .create ("c"))
            .addOption (OptionBuilder
                        .withLongOpt ("help")
                        .withDescription ("show this help and exit")
                        .create ('h'))
            .addOption (OptionBuilder
                        .withLongOpt ("include")
                        .withDescription ("run commands from <file>")
                        .withArgName ("file")
                        .hasArg ()
                        .create ("i"))
            .addOption (OptionBuilder
                        .withLongOpt ("mute")
                        .withDescription ("suppress informational messages")
                        .create ('m'))
            .addOption (OptionBuilder
                        .withLongOpt ("no-interactive")
                        .withDescription ("do not enter interactive mode")
                        .create ('n'))
            .addOption (OptionBuilder
                        .withLongOpt ("version")
                        .withDescription ("show version information and exit")
                        .create ())
            .addOption (OptionBuilder
                        .withLongOpt ("verbose")
                        .withDescription ("be verbose")
                        .create ('v'))
            .addOption (OptionBuilder
                        .withLongOpt ("exit-on-error")
                        .withDescription ("exit immediately on any error")
                        .create ('x'));
        CommandLine cl = null;
        try {
            final CommandLineParser parser = new DefaultParser ();
            cl = parser.parse (options, args);
        } catch (ParseException e) {
            errorln (e.getMessage ());
            System.out.format ("use '%s --help' for more information\n", ME);
            System.exit (-1);
        }
        assert cl != null;
        if (cl.hasOption ("help")) {
            HelpFormatter fmt = new HelpFormatter ();
            fmt.printHelp (ME + " [<OPTION>]... [<FILE>]...", options);
            System.exit (0);
        }
        if (cl.hasOption ("version")) {
            System.out.println (version ());
            System.exit (0);
        }
        if (cl.hasOption ("no-interactive")) {
            _parseCmdLineOptionsNoInteractive = true;
        }
        if (cl.hasOption ("exit-on-error")) {
            _parseCmdLineOptionsOptExitOnError = true;
        }
        if (cl.hasOption ("mute")) {
            _parseCmdLineOptionsOptMute = true;
        }
        String script = "";
        if (cl.hasOption ("verbose")) {
            script += ":set verbose on\n";
        }
        for (String uri : cl.getArgList ()) {
            script += ":load " + uri + "\n";
        }
        if (cl.hasOption ("command")) {
            for (String cmd : cl.getOptionValues ("command")) {
                script += cmd + "\n";
            }
        }
        if (cl.hasOption ("include")) {
            for (String path : cl.getOptionValues ("include")) {
                script += ":include " + path + "\n";
            }
        }
        return new BufferedReader (new StringReader (script));
    }

    private static boolean run (Stack<BufferedReader> stack)
        throws IOException {
        boolean negate = false;
        boolean lastStatus = true;
        boolean status = true;
        String lastLine = null;
        String line = null;
        long lastTime = 0;
        long time;
        String[] toks;
        String cmd;
        String arg;
        BufferedReader br = stack.pop ();
        while (true) {
            if (stack.empty ()) {
                info (optPrompt);
            }
            line = br.readLine ();
            if (line == null) {
                if (stack.empty ()) {
                    break;
                } else {
                    br = stack.pop ();
                    continue;
                }
            }
            lastLine = line;
            line = line.trim ();
            if (line.equals (":{")) { // multi-line
                String accum = "";
                while (true) {
                    line = br.readLine ();
                    if (line == null) {
                        break;
                    }
                    line = line.trim ();
                    if (line.endsWith (":}")) {
                        line = accum;
                        break;
                    }
                    accum += line + " ";
                }
                if (line == null) {
                    break;
                }
            }
            if (line.length () == 0 || line.charAt (0) == '#') {
                continue;
            }
            if (line.charAt (0) == '!') {
                if (line.length () == 1) {
                    continue;
                }
                negate = true;
                line = line.substring (1, line.length ()).trim ();
            }
            if (line.charAt (0) == ':') {
                if (line.length () == 1) {
                    line = lastLine;
                }
                toks = unconsStr (line, ' ');
                cmd = toks[0]; arg = toks[1];
                status = lastStatus;
                time = System.currentTimeMillis ();
                if (line.equals (":?") || ":help".startsWith (cmd)) {
                    status = cmdHelp (arg);
                } else if (":include".startsWith (cmd)) {
                    if (arg.length () == 0) {
                        infoln ("OK, no file to include");
                        status = true;
                    } else {
                        BufferedReader brNext = cmdInclude (arg);
                        if (brNext != null) {
                            stack.push (br);
                            br = brNext;
                            status = true;
                        } else {
                            status = false;
                        }
                    }
                } else if (":clear".startsWith (cmd)) {
                    status = cmdClear ();
                } else if (":echo".startsWith (cmd)) {
                    status = cmdEcho (arg);
                } else if (":load".startsWith (cmd)) {
                    status = cmdLoad (arg);
                } else if (":quit".startsWith (cmd)) {
                    if (arg.length () > 0) {
                        if (arg.equals ("true")) {
                            status = true;
                        } else if (arg.equals ("negate")) {
                            status = !status;
                        } else {
                            status = false;
                        }
                    }
                    break;
                } else if (":shell".startsWith (cmd)) {
                    status = cmdShell (arg);
                } else if (":status".startsWith (cmd)) {
                    outputln (lastStatus);
                    status = lastStatus;
                    time = -1; // don't update lastTime
                } else if (":time".startsWith (cmd)) {
                    if (arg.equals ("ms")) {
                        outputln (String.format ("%d", lastTime));
                    } else if (arg.equals ("s")) {
                        outputln (String.format
                                  ("%g", ((double) lastTime)/1000.));
                    } else {
                        outputln (ms2Str (lastTime));
                    }
                    status = lastStatus;
                    time = -1;  // don't update lastTime
                } else if (":version".startsWith (cmd)) {
                    outputln (version ());
                    status = true;
                } else if (":goals".startsWith (cmd)) {
                    status = cmdGoals (arg);
                } else if (":rules".startsWith (cmd)) {
                    status = cmdRules (arg);
                } else if (":refine".startsWith (cmd)) {
                    status = cmdRefine (arg);
                } else if (":get".startsWith (cmd)) {
                    status = cmdGet (arg, false);
                } else if (":set".startsWith (cmd)) {
                    toks = unconsStr (arg, ' ');
                    status = cmdSet (toks[0], toks[1]);
                } else if (":unset".startsWith (cmd)) {
                    status = cmdUnset (arg);
                } else if (":unset-all".startsWith (cmd)) {
                    status = cmdUnsetAll ();
                } else {
                    usage ("unknown command '%s'\n", cmd);
                    status = false;
                    time = -1;  // don't update lastTime
                }
            } else {
                usage ("unknown command '%s'\n", line);
                status = false;
                time = -1;  // don't update lastTime
            }
            if (negate) {
                status = !status;
                negate = false;
            }
            lastStatus = status;
            if (time >= 0) {
                lastTime = System.currentTimeMillis () - time;
            }
            if (optExitOnError && !status) {
                return false;
            }
        }
        return status;
    }

    private static boolean run (BufferedReader br) throws IOException {
        Stack<BufferedReader> stack = new Stack<BufferedReader> ();
        stack.push (br);
        return run (stack);
    }

    public static void main(String[] args) {
        try {
            optExitOnError = true;
            optMute = true;
            if (!run (parseCmdLineOptions (args))) {
                System.exit (1);
            }
            if (_parseCmdLineOptionsNoInteractive) {
                System.exit (0);
            }
            optExitOnError = _parseCmdLineOptionsOptExitOnError;
            optMute = _parseCmdLineOptionsOptMute;
            infoln (ME + ", :? for help");
            if (!run (new BufferedReader
                      (new InputStreamReader (System.in)))) {
                System.exit (1);
            }
            infoln ("leaving " + ME + ".");
        } catch (Throwable t) {
            System.err.println ("unexpected error:");
            t.printStackTrace ();
            System.exit (1);
        }
    }
}
