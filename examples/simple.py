from jtabwb import JTabWb

sc = JTabWb('A->B, B->C => A->C')
n = 0
while len(sc.goals) > 0:
    n += 1
    print(f'- Step {n} -')
    print('goals:', sc.goals)
    print('appRules:', sc.appRules)
    goal = min(sc.goals.keys())
    for rule in sc.appRules[goal]:
        print(f'{rule.name()}, isLeft={rule.isLeft()}, \
isRight={rule.isRight()}:\t {rule.formula}')
    print()
    print(f'applying the left-most rule of goal {goal}...')
    sc.refine(goal, sc.appRules[goal][0])
    print()
