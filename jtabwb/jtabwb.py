from collections.abc import Mapping
from collections import deque
from pathlib import Path
import sys

import jpype
import jpype.imports

JTABWB_JAR = str(Path(__file__).resolve().parent
                 / 'jtabwb-1.0-jar-with-dependencies.jar')
jpype.startJVM(classpath=[JTABWB_JAR])
if True:
    from cpl.g3c.interactive import Prover, ProverException


__all__ = [
    'JTabWb',
]


@jpype.JImplementationFor('jtabwbx.prop.formula.Formula')
class _JFormula:
    def __repr__(self):
        return str(self)

    def __str__(self):
        return str(self.toString())

    @property
    def type(self):
        return str(self.getFormulaType())

    @property
    def args(self):
        return self.immediateSubformulas() or []

    def isAtom(self):
        return self.type == 'ATOMIC_WFF'


@jpype.JImplementationFor('jtabwbx.prop.formula.SequentOnArray')
class _JSequent:
    def __repr__(self):
        return str(self)

    def __str__(self):
        return str(self.toString())

    @property
    def lhs(self):
        return self.getLeftFormulas()

    @property
    def rhs(self):
        return self.getRightFormulas()


@jpype.JImplementationFor('cpl.g3c.calculus.ClashDetectionRule')
class _JRuleAxiom:
    def __repr__(self):
        return str(self)

    def __str__(self):
        return str(self.name())

    @property
    def numPremises(self):
        return 0

    @property
    def sequent(self):
        return self.getPremise()

    @property
    def formula(self):
        rhs = self.sequent.rhs
        for x in self.sequent.lhs:
            if x.isAtom() and x in rhs:
                return x
        raise RuntimeError('should not get here')

    def isAxiom(self):
        return True

    def isLeft(self):
        return False

    def isRight(self):
        return False


@jpype.JImplementationFor('cpl.g3c.calculus.Rule_Left_AND')
@jpype.JImplementationFor('cpl.g3c.calculus.Rule_Left_IMPLIES')
@jpype.JImplementationFor('cpl.g3c.calculus.Rule_Left_NOT')
@jpype.JImplementationFor('cpl.g3c.calculus.Rule_Left_OR')
@jpype.JImplementationFor('cpl.g3c.calculus.Rule_Right_AND')
@jpype.JImplementationFor('cpl.g3c.calculus.Rule_Right_IMPLIES')
@jpype.JImplementationFor('cpl.g3c.calculus.Rule_Right_NOT')
@jpype.JImplementationFor('cpl.g3c.calculus.Rule_Right_OR')
class _JRuleOther:
    def __repr__(self):
        return str(self)

    def __str__(self):
        return str(self.name())

    @property
    def numPremises(self):
        return self.numberOfSubgoals()

    @property
    def sequent(self):
        return self.getPremise()

    @property
    def formula(self):
        return self.mainFormula()

    def isAxiom(self):
        return False

    def isLeft(self):
        return str(self).startswith('LEFT')

    def isRight(self):
        return str(self).startswith('RIGHT')


class FakeDict (Mapping):
    __slots__ = ('_data', '_getKeys', '_getItem')

    def __init__(self, getKeys, getItem):
        self._getKeys = getKeys
        self._getItem = getItem

    def __repr__(self):
        return str(self)

    def __str__(self):
        return str(dict(self))

    def __getitem__(self, key):
        return self._getItem(key)

    def __iter__(self):
        return iter(self._getKeys())

    def __len__(self):
        return len(self._getKeys())


class JTabWb:
    """Prover state."""

    __slots__ = (
        '_prover',              # The underlying prover.
        '_appRulesDict',        # Alias to applicable rules.
    )

    def __init__(self, text=None):
        self._prover = Prover()
        self._appRulesDict = FakeDict(self.getGoalIds,
                                      self.getApplicableRules)
        if text is not None:
            self.load(text)

    def reset(self):
        self._prover.reset()

    def load(self, text):
        try:
            return self._prover.load(text)
        except ProverException as e:
            raise SyntaxError(e)

    @property
    def goals(self):
        return self.getGoals()

    def getGoals(self):
        return dict(self._prover.getGoals())

    @property
    def goalIds(self):
        return self.getGoalIds()

    def getGoalIds(self):
        return self._prover.getGoalIds()

    def getGoal(self, id):
        return self._prover.getGoal(id)

    @property
    def appRules(self):
        return self._appRulesDict

    def getApplicableRules(self, id):
        return list(self._prover.getApplicableRules(id) or [])

    def refine(self, id, rule):
        self._prover.refine(id, rule)

    @property
    def complete(self):
        return self.proofIsComplete()

    def proofIsComplete(self):
        return len(self.getGoalIds()) == 0

    @property
    def exhausted(self):
        return self.proofIsExhausted()

    def proofIsExhausted(self):
        for id in self.getGoalIds():
            if not self.getApplicableRules(id):
                return True
        return False

    def prove(self, limit=None, timeout=None, stats=None):
        from time import time as now
        limit = limit if limit is not None else sys.maxsize
        elapsed = 0
        refines = 0

        def done(status):
            if stats is not None:
                stats['time'] = elapsed
                stats['refinements'] = refines
            return status
        t0 = now()
        while refines < limit:
            elapsed = now() - t0
            if timeout is not None and elapsed > timeout:
                return done(None)
            goals = self.goalIds
            if len(goals) == 0:
                return done(True)
            id = next(self.goalIds.iterator())
            app = self.getApplicableRules(id)
            if not app:
                return done(False)
            refines += 1
            rule = app[0]
            # --
            # print(f'#{refines}, goal={id}, rule={rule.name()}')
            # print(f'\t{self.getGoal(id)}')
            # --
            self.refine(id, rule)
        return done(None)


if __name__ == '__main__':
    sc = JTabWb()
    sc.load('=>A|~A')
    print(sc.getGoals())
    print(next(sc.goalIds.iterator()))
    seq = sc.getGoal(0)
    print(seq, seq.lhs, seq.rhs)
    print(sc.getApplicableRules(0))
    rule = sc.getApplicableRules(0)[0]
    print(rule, rule.formula, rule.numPremises,
          rule.isAxiom(), rule.isLeft(), rule.isRight())
    sc.refine(0, rule)
    print(sc.getGoal(1))
    rule = sc.getApplicableRules(1)[0]
    print(rule, rule.formula, rule.numPremises,
          rule.isAxiom(), rule.isLeft(), rule.isRight())
    sc.refine(1, rule)
    print(sc.getGoal(2))
    rule = sc.getApplicableRules(2)[0]
    print(rule, rule.formula, rule.numPremises,
          rule.isAxiom(), rule.isLeft(), rule.isRight())
