import unittest
from jtabwb import JTabWb


class TestJTabWb(unittest.TestCase):

    def test_JTabWb_init(self):
        sc = JTabWb()
        self.assertIsNotNone(sc._prover)
        self.assertEqual(len(sc.getGoals()), 1)
        self.assertIsNotNone(sc.getGoal(0))
        self.assertIsNone(sc.getGoal(4))
        self.assertEqual(len(sc.getApplicableRules(0)), 0)
        self.assertEqual(len(sc.getApplicableRules(4)), 0)

    def test_JTabWb_load(self):
        sc = JTabWb('A => A')
        self.assertEqual(len(sc.goals), 1)
        self.assertEqual(len(sc.goalIds), 1)
        self.assertEqual(len(sc.appRules), 1)
        self.assertEqual(len(sc.appRules[0]), 1)
        self.assertEqual(sc.appRules[0][0].name(), 'AX')
        sc.load('B => B & B')
        self.assertEqual(len(sc.goals), 1)
        self.assertEqual(len(sc.goalIds), 1)
        self.assertEqual(len(sc.appRules), 1)
        self.assertEqual(len(sc.appRules[0]), 1)
        self.assertEqual(sc.appRules[0][0].name(), 'RIGHT_AND')
        self.assertRaises(SyntaxError, sc.load, 'syntax error')
        self.assertRaises(SyntaxError, JTabWb, 'syntax error')

    def test_JTabWb_refine(self):
        sc = JTabWb()
        sc.load('A -> A => A')
        self.assertFalse(sc.complete)
        self.assertFalse(sc.exhausted)
        rule = sc.appRules[0][0]
        self.assertIsNotNone(rule)
        self.assertEqual(rule.name(), 'LEFT_IMPLIES')
        sc.refine(4, None)      # no-op
        sc.refine(4, rule)      # no-op
        sc.refine(0, rule)
        self.assertEqual(len(sc.goals), 2)
        self.assertFalse(0 in sc.goals)
        self.assertTrue(1 in sc.goals)
        self.assertTrue(2 in sc.goals)
        self.assertEqual(len(sc.appRules[1]), 0)
        self.assertEqual(len(sc.appRules[2]), 1)
        self.assertFalse(sc.complete)
        self.assertTrue(sc.exhausted)
        rule = sc.appRules[2][0]
        self.assertEqual(rule.name(), 'AX')
        sc.refine(1, rule)
        rule = sc.appRules[2][0]
        self.assertEqual(rule.name(), 'AX')
        sc.refine(2, rule)
        self.assertEqual(len(sc.goalIds), 1)
        self.assertFalse(sc.complete)
        self.assertTrue(sc.exhausted)
        sc.load('AvA => AvA')
        sc.refine(0, sc.appRules[0][0])

    def test_JTabWb_sequent(self):
        sc = JTabWb('A, B, X, A, Y, Z => A&B, B&A, C')
        seq = sc.goals[0]
        self.assertEqual(len(seq.lhs), 5)
        self.assertEqual(len(seq.rhs), 2)

    def test_JTabWb_formula(self):
        sc = JTabWb('A, B, X, A, Y, Z => A&B, B&A, C')
        seq = sc.goals[0]
        rule = sc.appRules[0][0]
        self.assertEqual(rule.name(), 'RIGHT_AND')
        self.assertFalse(rule.isAxiom())
        self.assertFalse(rule.isLeft())
        self.assertTrue(rule.isRight())
        self.assertEqual(rule.sequent, seq)
        self.assertEqual(str(rule.formula), '(A & B)')
        self.assertEqual(rule.formula.type, 'AND_WFF')
        self.assertEqual(len(rule.formula.args), 2)
        self.assertEqual(str(rule.formula.args[0]), 'A')
        self.assertEqual(rule.formula.args[0].type, 'ATOMIC_WFF')
        self.assertEqual(str(rule.formula.args[1]), 'B')
        self.assertEqual(rule.formula.args[1].type, 'ATOMIC_WFF')

        sc = JTabWb('A => A')
        seq = sc.goals[0]
        rule = sc.appRules[0][0]
        self.assertEqual(rule.name(), 'AX')
        self.assertTrue(rule.isAxiom())
        self.assertFalse(rule.isLeft())
        self.assertFalse(rule.isRight())
        self.assertEqual(rule.sequent, seq)
        self.assertEqual(str(rule.formula), 'A')
        self.assertEqual(len(rule.formula.args), 0)
        self.assertTrue(rule.formula.isAtom())

    def test_JTabWb_prove(self):
        self.assertTrue(JTabWb('A=>A').prove())
        self.assertFalse(JTabWb('=>').prove())
        self.assertTrue(JTabWb('=>A|~A').prove())
        stats = {}
        self.assertTrue(JTabWb('A|B=>B|A').prove(stats=stats))
        self.assertEqual(stats['refinements'], 5)
        stats = {}
        self.assertIsNone(JTabWb('A|B=>B|A').prove(limit=2, stats=stats))
        self.assertEqual(stats['refinements'], 2)
        self.assertTrue(JTabWb('A&(B|C)=>(A&B)|(A&C)').prove())


if __name__ == '__main__':
    unittest.main()
