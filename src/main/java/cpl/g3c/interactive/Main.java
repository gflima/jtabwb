package cpl.g3c.interactive;

import java.io.*;
import java.lang.*;
import java.util.*;
import org.apache.commons.cli.*;

import jtabwb.engine._AbstractRule;
import jtabwb.engine.RuleType;

public class Main {

    // The name of this program.
    private static String ME = "jtabwb-g3c-top";

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

    public static void main(String[] args) {
        if (args.length != 1) {
            errorln ("Usage: %s <formula>");
            System.exit(-1);
        }
        try {
            Prover prover = new Prover();
            System.out.println(prover.getGoal (0).toString());
            prover.load (args[0]);
            System.out.println(prover.getGoal (0).toString());
            Collection<_AbstractRule> appRules = prover.getApplicableRules (0);
            for (_AbstractRule rule : appRules) {
                RuleType type = RuleType.getType (rule);
                System.out.println (rule.name());
                System.out.println (type.name());
            }
            System.out.println ("done");
        } catch (ProverException e) {
            errorln (e.getMessage());
            System.exit(1);
        }
    }
}
