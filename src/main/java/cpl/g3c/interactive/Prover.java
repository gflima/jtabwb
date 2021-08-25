package cpl.g3c.interactive;

import java.util.*;

import cpl.g3c.calculus.ClashDetectionRule;
import cpl.g3c.calculus.Rule_Left_AND;
import cpl.g3c.calculus.Rule_Left_IMPLIES;
import cpl.g3c.calculus.Rule_Left_NOT;
import cpl.g3c.calculus.Rule_Left_OR;
import cpl.g3c.calculus.Rule_Right_AND;
import cpl.g3c.calculus.Rule_Right_IMPLIES;
import cpl.g3c.calculus.Rule_Right_NOT;
import cpl.g3c.calculus.Rule_Right_OR;
import jtabwb.engine.RuleType;
import jtabwb.engine._AbstractGoal;
import jtabwb.engine._AbstractRule;
import jtabwb.engine._ClashDetectionRule;
import jtabwb.engine._RegularRule;
import jtabwbx.prop.basic.FormulaType;
import jtabwbx.prop.basic.PropositionalConnective;
import jtabwbx.prop.formula.Formula;
import jtabwbx.prop.formula.FormulaFactory;
import jtabwbx.prop.formula.SequentOnArray;
import jtabwbx.prop.formula._Sequent;
import jtabwbx.prop.parser.FormulaParseException;
import jtabwbx.prop.parser.PropositionalFormulaParser;

// Interactive prover.
public class Prover {
    private FormulaFactory _formulaFactory;
    PropositionalFormulaParser _formulaParser;
    private int _nextGoalId;    // Incremented at each new goal.
    private Map<Integer, _Sequent> _goals;
    private Map<Integer, Collection<_AbstractRule>> _appRules;

    // Types of compound formulas.
    private final EnumSet<FormulaType> COMPOUND_TYPES
        = EnumSet.of (FormulaType.AND_WFF,
                      FormulaType.OR_WFF,
                      FormulaType.IMPLIES_WFF,
                      FormulaType.NOT_WFF);

    // Constructor.
    public Prover() {
        this._formulaFactory = new FormulaFactory ("@FALSE", "@TRUE");
        this._formulaParser = new PropositionalFormulaParser();
        this._goals = new HashMap<>();
        this._appRules = new HashMap<>();
        this.load (new SequentOnArray (this._formulaFactory));
    }

    // Resets prover state.
    public void reset () {
        this._nextGoalId = 0;
        this._goals.clear ();
        this._appRules.clear ();
    }

    // Loads a new initial sequent.
    public void load (_Sequent sequent) {
        this.reset ();
        this._goals.put (this._nextGoalId++, sequent);
    }

    private Formula _parse (String text) throws ProverException {
        try {
            return this._formulaFactory.buildFrom
                (this._formulaParser.parse (text));
        } catch (FormulaParseException e) {
            throw new ProverException(e);
        }
    }

    // Loads a new initial sequent from problem text.
    public void load (String problem) throws ProverException {
        String[] sides = problem.split ("=>", 2);
        if (sides.length < 2) {
            sides = new String[] {"", sides[0]};
        }
        LinkedList<Formula> lhs = new LinkedList<Formula> ();
        LinkedList<Formula> rhs = new LinkedList<Formula> ();
        for (int i = 0; i < sides.length; i++) {
            if (sides[i].length () == 0) {
                continue;       // nothing to do
            }
            for (String text : sides[i].split (",")) {
                Formula wff = this._parse (text);
                if (i == 0) {
                    lhs.add (wff);
                } else {
                    rhs.add (wff);
                }
            }
        }
        _Sequent seq = new SequentOnArray(this._formulaFactory);
        for (Formula wff : lhs) {
            seq.addLeft (wff);
        }
        for (Formula wff : rhs) {
            seq.addRight (wff);
        }
        this.load (seq);
    }

    // Gets goal.
    public _Sequent getGoal(int id) {
        return this._goals.get (id);
    }

    // Gets all goals.
    public Map<Integer, _Sequent> getGoals () {
        return this._goals;
    }

    // Gets the rules applicable to goal.
    public Collection<_AbstractRule> getApplicableRules (int id) {
        _Sequent goal = this.getGoal (id);
        if (goal == null) {
            return null;        // no such goal
        }
        if (this._appRules.containsKey (id)) {
            return this._appRules.get (id); // already computed
        }
        Collection<_AbstractRule> appRules;
        appRules = new LinkedHashSet<_AbstractRule>();
        for (FormulaType type : this.COMPOUND_TYPES) {
            Collection<Formula> lhs = goal.getLeftFormulas (type);
            if (lhs != null) {
                for (Formula wff : lhs) { // lhs
                    PropositionalConnective conn = wff.mainConnective ();
                    switch (conn) {
                    case AND:
                        appRules.add (new Rule_Left_AND (goal, wff));
                        break;
                    case OR:
                        appRules.add (new Rule_Left_OR (goal, wff));
                        break;
                    case IMPLIES:
                        appRules.add (new Rule_Left_IMPLIES (goal, wff));
                        break;
                    case NOT:
                        appRules.add (new Rule_Left_NOT (goal, wff));
                        break;
                    default:
                        throw new RuntimeException ("should not get here");
                    }
                }
            }
            Collection<Formula> rhs = goal.getRightFormulas (type);
            if (rhs != null) {
                for (Formula wff : rhs) { // rhs
                    PropositionalConnective conn = wff.mainConnective();
                    switch (conn) {
                    case AND:
                        appRules.add (new Rule_Right_AND (goal, wff));
                        break;
                    case OR:
                        appRules.add (new Rule_Right_OR (goal, wff));
                        break;
                    case IMPLIES:
                        appRules.add (new Rule_Right_IMPLIES (goal, wff));
                        break;
                    case NOT:
                        appRules.add (new Rule_Right_NOT (goal, wff));
                        break;
                    default:
                        throw new RuntimeException ("should not get here");
                    }
                }
            }
        }
        if (goal.isIdentityAxiom()) { // axiom
            appRules.add (new ClashDetectionRule(goal));
        }
        this._appRules.put (id, appRules);
        return appRules;
    }

    // Applies rule to goal.
    public void refine (int id, _AbstractRule rule) {
        Collection<_AbstractRule> appRules = this.getApplicableRules (id);
        if (appRules == null || !appRules.contains (rule)) {
            return;             // nothing to do
        }
        RuleType type = RuleType.getType (rule);
        switch (type) {
        case REGULAR: {
            _RegularRule app = ((_RegularRule) rule);
            while (app.hasNextSubgoal ()) {
                _AbstractGoal next = app.nextSubgoal ();
                this._goals.put (this._nextGoalId++, (_Sequent) next);
            }
            break;
        }
        case CLASH_DETECTION_RULE: {
            break;              // nothing to do
        }
        default:
            throw new RuntimeException ("should not get here");
        }
        this._goals.remove (id);
        this._appRules.remove (id);
    }
}
