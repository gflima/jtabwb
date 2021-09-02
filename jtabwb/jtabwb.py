from pathlib import Path

import jpype
import jpype.imports

JTABWB_JAR = str(Path(__file__).resolve().parent
                 / 'jtabwb-1.0-jar-with-dependencies.jar')
jpype.startJVM(classpath=[JTABWB_JAR])
if True:
    from cpl.g3c.interactive import Prover, ProverException


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
    def formula(self):
        return None

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
    def formula(self):
        return self.mainFormula()

    def isAxiom(self):
        return False

    def isLeft(self):
        return str(self).startswith('LEFT')

    def isRight(self):
        return str(self).startswith('RIGHT')


class JTabWb:

    __slots__ = (
        '_prover',
    )

    def __init__(self):
        self._prover = Prover()

    def reset(self):
        self._prover.reset()

    def load(self, text):
        try:
            return self._prover.load(text)
        except ProverException as e:
            raise SyntaxError(e)

    def getGoals(self):
        return dict(self._prover.getGoals())

    def getGoal(self, id):
        return self._prover.getGoal(id)

    def getApplicableRules(self, id):
        return list(self._prover.getApplicableRules(id) or [])

    def refine(self, id, rule):
        self._prover.refine(id, rule)


if __name__ == '__main__':
    sc = JTabWb()
    sc.load('=>A|~A')
    print(sc.getGoals())
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
