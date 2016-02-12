/*******************************************************************************
 * Copyright (C) 2013, 2016 Mauro Ferrari This program is free software; you can
 * redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. You should have received
 * a copy of the GNU General Public License along with this program; if not, see
 * <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package jtabwbx.prop.formula;

import jtabwb.util.CaseNotImplementedImplementationError;
import jtabwb.util.ContractViolationImplementationError;
import jtabwb.util.ImplementationError;
import jtabwbx.prop.basic.FormulaType;
import jtabwbx.prop.basic.PropositionalConnective;
import jtabwbx.prop.btformula.BTFormula;
import jtabwbx.prop.btformula.BTFormulaProposition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.antlr.v4.runtime.tree.ParseTree;

/**
 * The factory building formulas.
 * 
 * @author Lorenzo Bossi
 */
public class FormulaFactory {

  private final Map<String, FormulaProposition> propositions;
  private final Map<AbstractCompoundFormula, AbstractCompoundFormula> formulaCompounds;
  private final ArrayList<Formula> formulasByIndex;
  private BitSetOfFormulas[] formulasByType;
  private BitSetOfFormulas generatedFormulas;
  private int formulaCounter;
  boolean translateNot = false; /*
                                 * if true (~ A) is built as (A -> FALSE)
                                 */
  boolean translateIff = false; /*
                                 * if true (A <-> B) is built as (A -> B) & (B
                                 * -> A)
                                 */
  boolean translateImpliesFalse = false; /*
                                          * if true (A -> fasle) is built as ~A
                                          */

  /**
   * FALSE is represented by a propositional variable with this name.
   */
  private String FALSE_NAME = "false";

  /**
   * TRUE is represented by a propositional variable with name "true".
   */
  private String TRUE_NAME = "true";

  /**
   * The formula representing the propositional variable FALSE.
   */
  public final FormulaProposition FALSE;

  /**
   * The formula representing the propositional variable TRUE.
   */
  public final FormulaProposition TRUE;

  BitSetOfFormulas intuitionisticNonLocalFormulas; // the set of intuitionistic local formulas in this factory.

  /**
   * Constructs an instance of the factory using the specified names for true
   * and false representation. Note that the names for true and false must be
   * different, otherwise a {@link ContractViolationImplementationError} is
   * thrown.
   * 
   * @param falseName the name of the false constant.
   * @param trueName the name of the true constant.
   */
  public FormulaFactory(String falseName, String trueName) {
    if (trueName.equals(falseName))
      throw new ContractViolationImplementationError(
          "Names for false and true constants must be different.");
    this.formulaCounter = 0;
    this.propositions = new HashMap<String, FormulaProposition>(100, .5f);
    this.formulaCompounds = new HashMap<AbstractCompoundFormula, AbstractCompoundFormula>(100, .5f);
    this.formulasByIndex = new ArrayList<Formula>(50);
    this.generatedFormulas = new BitSetOfFormulas(this);
    this.formulasByType = new BitSetOfFormulas[FormulaType.values().length];
    for (int i = 0; i < this.formulasByType.length; i++)
      this.formulasByType[i] = new BitSetOfFormulas(this);
    this.intuitionisticNonLocalFormulas = new BitSetOfFormulas(this);
    this.FALSE_NAME = falseName;
    this.FALSE = this.buildAtomic(FALSE_NAME);
    this.TRUE_NAME = trueName;
    this.TRUE = this.buildAtomic(TRUE_NAME);
  }

  /**
   * Constructs an instance of the factory using "false" and "true" as names for
   * true and false constants.
   */
  public FormulaFactory() {
    this("false", "true");
  }

  /**
   * The number of distinct formulas generated by this factory.
   * 
   * @return number of distinct formulas generated by this factory
   */
  public int numberOfGeneratedFormulas() {
    return formulaCounter;
  }

  /**
   * Returns a copy of the list of formulas generated by this factory.
   * 
   * @return a copy of the list of formulas generated by this factory.
   */
  @SuppressWarnings("unchecked")
  public ArrayList<Formula> generatedFormulas() {
    return (ArrayList<Formula>) formulasByIndex.clone();
  }

  /**
   * Returns the formula modelling the propositional constant TRUE.
   * 
   * @return the formula representing TRUE.
   */
  public FormulaProposition getTrue() {
    return TRUE;
  }

  /**
   * Returns the formula modelling the propositional constant FALSE.
   * 
   * @return the formula representing FALSE.
   */
  public FormulaProposition getFalse() {
    return FALSE;
  }

  /**
   * Returns the {@link BitSetOfFormulas} containing all the generated formulas.
   * 
   * @return the bitset of all generated formulas.
   */
  public BitSetOfFormulas getGeneratedFormula() {
    return generatedFormulas;
  }

  /**
   * Returns the {@link BitSetOfFormulas} containing all the generated formulas
   * of the specified type.
   * 
   * @param type the type of the formulas to return.
   * @return the bitset of all generated formulas.
   */
  public BitSetOfFormulas getGeneratedFormulasOfType(FormulaType type) {
    return formulasByType[type.ordinal()];
  }

  public FormulaProposition buildAtomic(String name) {
    FormulaProposition newProp = propositions.get(name);
    if (newProp == null) {
      newProp = new FormulaProposition(this, name, name.equals(TRUE_NAME), name.equals(FALSE_NAME));
      newProp.size = 1;
      propositions.put(name, newProp);
      newProp.setIndex(formulaCounter++);
      formulasByIndex.add(newProp);
      this.generatedFormulas.add(newProp);
      this.formulasByType[newProp.getFormulaType().ordinal()].add(newProp);
      if (!newProp.isIntuitionisticLocalFormula())
        intuitionisticNonLocalFormulas.add(newProp);
    }
    return newProp;
  }

  /**
   * Builds the formula having the specified logical constant as main connective
   * and the specified subformulas as direct subformulas.
   * 
   * @param mainConnective the main connective (a logical constant).
   * @param subFormulas the direct subformulas.
   * @return the corresponding compound formula.
   */
  public Formula buildCompound(PropositionalConnective mainConnective, Formula... subFormulas) {

    PropositionalConnective connective = (PropositionalConnective) mainConnective;
    AbstractCompoundFormula newFormula;

    try {
      if (connective == PropositionalConnective.NOT) {
        if (translateNot)
          newFormula = new FormulaImplies(this, subFormulas[0], FALSE);
        else
          newFormula = new FormulaNot(this, subFormulas[0]);
      } else {
        Formula left = subFormulas[0];
        Formula right = subFormulas[1];
        switch (connective) {
        case IMPLIES:
          if (translateImpliesFalse && right.equals(FALSE))
            newFormula = new FormulaNot(this, left);
          else
            newFormula = new FormulaImplies(this, left, right);
          break;
        case EQ:
          if (translateIff) {
            Formula leftToRight = buildCompound(PropositionalConnective.IMPLIES, left, right);
            Formula rightToLeft = buildCompound(PropositionalConnective.IMPLIES, right, left);
            newFormula = new FormulaAnd(this, leftToRight, rightToLeft);
          } else
            newFormula = new FormulaIff(this, left, right);
          break;
        case AND:
          newFormula = new FormulaAnd(this, left, right);
          break;
        case OR:
          newFormula = new FormulaOr(this, left, right);
          break;
        default:
          throw new CaseNotImplementedImplementationError(connective.getName());
        }
      }
      return getCanonicalFormula(newFormula);
    } catch (ClassCastException e) {
      throw new ImplementationError("Wrong subformula type: " + e.getMessage());
    }
  }

  private AbstractCompoundFormula getCanonicalFormula(AbstractCompoundFormula newFormula) {
    AbstractCompoundFormula canonicalFormula = formulaCompounds.get(newFormula);
    if (canonicalFormula == null) {
      formulaCompounds.put(newFormula, newFormula);
      canonicalFormula = newFormula;
      canonicalFormula.setIndex(formulaCounter++);
      formulasByIndex.add(canonicalFormula);
      this.generatedFormulas.add(canonicalFormula);
      this.formulasByType[canonicalFormula.getFormulaType().ordinal()].add(canonicalFormula);
      if (!canonicalFormula.isIntuitionisticLocalFormula())
        intuitionisticNonLocalFormulas.add(canonicalFormula);
    }
    return canonicalFormula;
  }

  public Formula getByIndex(int index) {
    return formulasByIndex.get(index);
  }

  /**
   * Build an instance of the specified formula in this factory.
   * 
   * @param wff the formula to build.
   * @return the instance of the formula in this factory.
   */
  public Formula buildFrom(Formula wff) {
    if (wff.isAtomic())
      return buildAtomic(((FormulaProposition) wff).getName());
    else {
      PropositionalConnective mainConnective = wff.mainConnective();
      switch (mainConnective) {
      case AND:
      case EQ:
      case IMPLIES:
      case OR: {
        Formula left = buildFrom(wff.immediateSubformulas()[0]);
        Formula right = buildFrom(wff.immediateSubformulas()[1]);
        return buildCompound(mainConnective, left, right);
      }
      case NOT:
        return buildCompound(mainConnective, buildFrom(wff.immediateSubformulas()[0]));
      default:
        throw new ImplementationError(ImplementationError.CASE_NOT_IMPLEMENTED);
      }
    }
  }

  /**
   * Build an instance of the specified {@link BTFormula}.
   * 
   * @param wff the formula to build.
   * @return the instance of the formula in this factory.
   */
  public Formula buildFrom(BTFormula wff) {
    if (wff.isAtomic())
      return buildAtomic(((BTFormulaProposition) wff).getName());
    else {
      PropositionalConnective mainConnective = wff.mainConnective();
      Formula translation = null;
      switch (mainConnective) {
      case AND:
      case EQ:
      case IMPLIES:
      case OR: {
        Formula left = buildFrom(wff.immediateSubformulas()[0]);
        Formula right = buildFrom(wff.immediateSubformulas()[1]);
        translation = buildCompound(mainConnective, left, right);
        break;
      }
      case NOT:
        translation = buildCompound(mainConnective, buildFrom(wff.immediateSubformulas()[0]));
        break;
      default:
        throw new ImplementationError(ImplementationError.CASE_NOT_IMPLEMENTED);
      }
      return translation;
    }
  }

  /**
   * Build a formula from the specified parse-tree.
   * 
   * @param parseTree the parse tree.
   * @return the instance of the formula in this factory.
   */
  public Formula buildFrom(ParseTree parseTree) {
    FromParseTreeFormulaBuilder builder = new FromParseTreeFormulaBuilder(this);
    return builder.buildFrom(parseTree);
  }

  /**
   * Returns the bitset containing the intuitionistic NON local formulas in this
   * factory. Do not modify.
   * 
   * @return the set of intuitionistic local formulas in this factory.
   */
  public BitSetOfFormulas intuitionisticNonLocalFormulas() {
    return intuitionisticNonLocalFormulas;
  }

  public String getDescription() {
    return "Factory for propositional formulas.";
  }

  /**
   * If <code>b</code> is <code>true</code> negated formulas (~ A) are built as
   * <code>(A -&gt; FALSE)</code>.
   * 
   * @param b if <code>true</code> negations are translated
   */
  public void setTranslateNegations(boolean b) {
    translateNot = b;
  }

  /**
   * If <code>b</code> is <code>true</code> equivalences
   * <code>(A &lt;-&gt; B)</code> are built as
   * <code>(A -&gt; B) &amp; (B -&gt; A)</code>.
   * 
   * @param b if <code>true</code> equivalences are translated
   */
  public void setTranslateEquivalences(boolean b) {
    translateIff = b;
  }

  /**
   * If <code>b</code> is <code>true</code> implications of the kind
   * <code>A -&gt; false</code> are built as <code>~ A</code>.
   * 
   * @param b if true the translation is applied.
   */
  public void setTranslateImplisesFalse(boolean b) {
    translateImpliesFalse = b;
  }

  @Override
  public String toString() {
    return String.format("GFormulaFactory:\n propositions: %d\n formulaCompounds: %d",
        propositions.size(), formulaCompounds.size());
  }

  GDebugInfo getDebugInfo() {
    GDebugInfo info = new GDebugInfo(propositions.size(), formulaCompounds.size());
    return info;
  }
}

class GDebugInfo {

  private final int propositions;
  private final int compoundFormula;

  public GDebugInfo(int propositions, int compoundFormula) {
    this.propositions = propositions;
    this.compoundFormula = compoundFormula;
  }

  public int getPropositions() {
    return propositions;
  }

  public int getCompoundFormula() {
    return compoundFormula;
  }
}