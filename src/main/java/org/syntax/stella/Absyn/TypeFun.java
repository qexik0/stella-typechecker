// File generated by the BNF Converter (bnfc 2.9.5).

package org.syntax.stella.Absyn;

public class TypeFun  extends Type {
  public final ListType listtype_;
  public final Type type_;
  public int line_num, col_num, offset;
  public TypeFun(ListType p1, Type p2) { listtype_ = p1; type_ = p2; }

  public <R,A> R accept(org.syntax.stella.Absyn.Type.Visitor<R,A> v, A arg) { return v.visit(this, arg); }

  public boolean equals(java.lang.Object o) {
    if (this == o) return true;
    if (o instanceof org.syntax.stella.Absyn.TypeFun) {
      org.syntax.stella.Absyn.TypeFun x = (org.syntax.stella.Absyn.TypeFun)o;
      return this.listtype_.equals(x.listtype_) && this.type_.equals(x.type_);
    }
    return false;
  }

  public int hashCode() {
    return 37*(this.listtype_.hashCode())+this.type_.hashCode();
  }


}
