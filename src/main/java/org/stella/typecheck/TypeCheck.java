package org.stella.typecheck;

import org.syntax.stella.Absyn.*;

public class TypeCheck
{
    public static void typecheckProgram(Program program) throws Exception
    {
        VisitTypeCheck v = new VisitTypeCheck();
        program.accept(v.new ProgramVisitor<>(), new ContextAndExpectedType() /* initial context information*/);
    }
}
