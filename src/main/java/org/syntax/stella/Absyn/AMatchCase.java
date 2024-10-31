// File generated by the BNF Converter (bnfc 2.9.5).

package org.syntax.stella.Absyn;

public class AMatchCase  extends MatchCase {
  public final Pattern pattern_;
  public final Expr expr_;
  public int line_num, col_num, offset;
  public AMatchCase(Pattern p1, Expr p2) { pattern_ = p1; expr_ = p2; }

  public <R,A> R accept(org.syntax.stella.Absyn.MatchCase.Visitor<R,A> v, A arg) { return v.visit(this, arg); }

  public boolean equals(java.lang.Object o) {
    if (this == o) return true;
    if (o instanceof org.syntax.stella.Absyn.AMatchCase) {
      org.syntax.stella.Absyn.AMatchCase x = (org.syntax.stella.Absyn.AMatchCase)o;
      return this.pattern_.equals(x.pattern_) && this.expr_.equals(x.expr_);
    }
    return false;
  }

  public int hashCode() {
    return 37*(this.pattern_.hashCode())+this.expr_.hashCode();
  }


}