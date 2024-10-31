// File generated by the BNF Converter (bnfc 2.9.5).

package org.syntax.stella.Absyn;

public class DeclExceptionVariant  extends Decl {
  public final String stellaident_;
  public final Type type_;
  public int line_num, col_num, offset;
  public DeclExceptionVariant(String p1, Type p2) { stellaident_ = p1; type_ = p2; }

  public <R,A> R accept(org.syntax.stella.Absyn.Decl.Visitor<R,A> v, A arg) { return v.visit(this, arg); }

  public boolean equals(java.lang.Object o) {
    if (this == o) return true;
    if (o instanceof org.syntax.stella.Absyn.DeclExceptionVariant) {
      org.syntax.stella.Absyn.DeclExceptionVariant x = (org.syntax.stella.Absyn.DeclExceptionVariant)o;
      return this.stellaident_.equals(x.stellaident_) && this.type_.equals(x.type_);
    }
    return false;
  }

  public int hashCode() {
    return 37*(this.stellaident_.hashCode())+this.type_.hashCode();
  }


}