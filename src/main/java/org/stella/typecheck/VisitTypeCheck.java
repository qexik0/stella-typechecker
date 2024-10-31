

package org.stella.typecheck;

import org.syntax.stella.Absyn.*;

import javafx.util.Pair;

import java.util.*;
import java.util.List;

enum TypeKind {
    TYPE_BOOL, TYPE_NAT, TYPE_UNIT;

    public Type getStellaType() {
        switch (this) {
            case TYPE_BOOL -> {
                return new TypeBool();
            }
            case TYPE_NAT -> {
                return new TypeNat();
            }
            case TYPE_UNIT -> {
                return new TypeUnit();
            }
        }
        return null;
    }
}

class ContextAndExpectedType {
    public Deque<Map<String, Type>> scopes = new ArrayDeque<>(List.of(new HashMap<>()));
    public Type expected;
    public Type expected_pattern;

    public Object thrown_exception;

    public Type findVariableType(String name) {
        var it = scopes.descendingIterator();
        while (it.hasNext()) {
            var cur = it.next();
            var currentType = cur.getOrDefault(name, null);
            if (currentType != null) {
                return currentType;
            }
        }
        return null;
    }

    public Type getException() {
        if (thrown_exception != null) {
            return (Type) thrown_exception;
        }
        return null;
    }

    public void registerException(Type type) {
        thrown_exception = type;
    }

    public void registerException(String name, Type type) {
        if (thrown_exception == null) {
            thrown_exception = new TypeVariant(new ListVariantFieldType());
            ((TypeVariant) thrown_exception).listvariantfieldtype_.add(new AVariantFieldType(name, new SomeTyping(type)));
        } else if (thrown_exception instanceof TypeVariant exception) {
            exception.listvariantfieldtype_.add(new AVariantFieldType(name, new SomeTyping(type)));
        }
    }
}

/*** Visitor Design Pattern Skeleton. ***/

/* This implements the common visitor design pattern.
   Tests show it to be slightly less efficient than the
   instanceof method, but easier to use.
   Replace the R and A parameters with the desired return
   and context types.*/

public class VisitTypeCheck {
    public void checkExhaustiveness(ListMatchCase listMatchCase, Type expected) {
        if (expected instanceof TypeSum) {
            checkSumExhaustiveness(listMatchCase);
        } else if (expected instanceof TypeVariant typeVariant) {
            checkVariantExhaustiveness(listMatchCase, typeVariant);
        }
    }

    private void checkSumExhaustiveness(ListMatchCase listMatchCase) {
        boolean hasInl = false, hasInr = false;
        for (var listMatchCaseElement : listMatchCase) {
            var aMatchCase = (AMatchCase) listMatchCaseElement;
            if (aMatchCase.pattern_ instanceof PatternInl) {
                hasInl = true;
            } else if (aMatchCase.pattern_ instanceof PatternInr) {
                hasInr = true;
            }
        }
        if (!hasInl || !hasInr) {
            throw new RuntimeException("ERROR_NONEXHAUSTIVE_MATCH_PATTERNS");
        }
    }

    private void checkVariantExhaustiveness(ListMatchCase listMatchCase, TypeVariant typeVariant) {
        var used = new BitSet(typeVariant.listvariantfieldtype_.size());
        for (var listMatchCaseItem : listMatchCase) {
            var aMatchCase = (AMatchCase) listMatchCaseItem;
            if (aMatchCase.pattern_ instanceof PatternVariant patternVariant) {
                var hasPattern = false;
                for (var i = 0; i < typeVariant.listvariantfieldtype_.size(); i++) {
                    var actualType = typeVariant.listvariantfieldtype_.get(i).accept(new VariantFieldTypeVisitor<>(), new ContextAndExpectedType());
                    if (actualType.getKey().equals(patternVariant.stellaident_)) {
                        used.set(i, true);
                        hasPattern = true;
                        break;
                    }
                }
                if (!hasPattern) {
                    throw new RuntimeException("ERROR_UNEXPECTED_PATTERN_FOR_TYPE");
                }
            } else {
                throw new RuntimeException("ERROR_UNEXPECTED_PATTERN_FOR_TYPE");
            }
        }
        if (used.cardinality() != typeVariant.listvariantfieldtype_.size()) {
            throw new RuntimeException("ERROR_NONEXHAUSTIVE_MATCH_PATTERNS");
        }
    }

    private void checkType(Type actual, Type expected) {
        if (expected == null) {
            return;
        } else if (actual instanceof TypeSum actual_sum && expected instanceof TypeSum expected_sum) {
            if (actual_sum.type_1 == null) {
                checkType(actual_sum.type_2, expected_sum.type_2);
            }
            if (actual_sum.type_2 == null) {
                checkType(actual_sum.type_1, expected_sum.type_1);
            }
        } else if (!actual.equals(expected)) {
            throw new RuntimeException("ERROR_UNEXPECTED_TYPE_FOR_EXPRESSION");
        }
    }

    public class ProgramVisitor<R extends Type, A extends ContextAndExpectedType> implements org.syntax.stella.Absyn.Program.Visitor<R, A> {
        public R visit(org.syntax.stella.Absyn.AProgram aProgram, A arg) { /* Code for AProgram goes here */
            aProgram.languagedecl_.accept(new LanguageDeclVisitor<>(), arg);
            for (var extension : aProgram.listextension_) {
                extension.accept(new ExtensionVisitor<>(), arg);
            }
            for (var decl : aProgram.listdecl_) {
                if (decl instanceof DeclFun declFun) {
                    var listType = new ListType();
                    for (var param : declFun.listparamdecl_) {
                        listType.add(param.accept(new ParamDeclVisitor<>(), new ContextAndExpectedType()).getValue());
                    }
                    arg.scopes.getLast().put(declFun.stellaident_, new TypeFun(listType, declFun.returntype_.accept(new ReturnTypeVisitor<>(), new ContextAndExpectedType())));
                } else if (decl instanceof DeclTypeAlias declTypeAlias) {
                    arg.scopes.getLast().put(declTypeAlias.stellaident_, declTypeAlias.type_);
                }
            }
            var mainType = arg.findVariableType("main");
            if (mainType == null) {
                throw new RuntimeException("ERROR_MISSING_MAIN");
            }
            if (((TypeFun) mainType).listtype_.size() != 1) {
                throw new RuntimeException("INCORRECT_ARITY_OF_MAIN");
            }
            for (var decl : aProgram.listdecl_) {
                decl.accept(new DeclVisitor<>(), arg);
            }
            return null;
        }
    }

    public class LanguageDeclVisitor<R, A extends ContextAndExpectedType> implements org.syntax.stella.Absyn.LanguageDecl.Visitor<R, A> {
        public R visit(org.syntax.stella.Absyn.LanguageCore languageCore, A arg) { /* Code for LanguageCore goes here */
            return null;
        }
    }

    public class ExtensionVisitor<R, A extends ContextAndExpectedType> implements org.syntax.stella.Absyn.Extension.Visitor<R, A> {
        public R visit(org.syntax.stella.Absyn.AnExtension anExtension, A arg) { /* Code for AnExtension goes here */
            for (var extensionName : anExtension.listextensionname_) {
            }
            return null;
        }
    }

    public class DeclVisitor<R extends Type, A extends ContextAndExpectedType> implements org.syntax.stella.Absyn.Decl.Visitor<R, A> {
        public R visit(org.syntax.stella.Absyn.DeclFun declFun, A arg) {
            arg.scopes.add(new HashMap<>());

            for (var annotation : declFun.listannotation_) {
                annotation.accept(new AnnotationVisitor<>(), arg);
            }

            var listParameterTypes = new ListType();
            for (var paramDecl : declFun.listparamdecl_) {
                listParameterTypes.add(paramDecl.accept(new ParamDeclVisitor<>(), arg).getValue());
            }

            var expectedReturnType = declFun.returntype_.accept(new ReturnTypeVisitor<>(), arg);
            arg.expected = expectedReturnType;
            var actualReturnType = declFun.expr_.accept(new ExprVisitor<>(), arg);

            checkType(actualReturnType, expectedReturnType);

            arg.scopes.removeLast();
            return (R) new TypeFun(listParameterTypes, actualReturnType);
        }

        @Override
        public R visit(DeclFunGeneric p, A arg) {
            return null;
        }

        public R visit(org.syntax.stella.Absyn.DeclTypeAlias declTypeAlias, A arg) { /* Code for DeclTypeAlias goes here */

            declTypeAlias.type_.accept(new TypeVisitor<>(), arg);
            return null;
        }

        @Override
        public R visit(DeclExceptionType declExceptionType, A arg) {
            arg.registerException(declExceptionType.type_);
            return null;
        }

        @Override
        public R visit(DeclExceptionVariant declExceptionVariant, A arg) {
            arg.registerException(declExceptionVariant.stellaident_, declExceptionVariant.type_);
            return null;
        }
    }

    public class LocalDeclVisitor<R extends Type, A extends ContextAndExpectedType> implements org.syntax.stella.Absyn.LocalDecl.Visitor<R, A> {
        public R visit(org.syntax.stella.Absyn.ALocalDecl aLocalDecl, A arg) { /* Code for ALocalDecl goes here */
            aLocalDecl.decl_.accept(new DeclVisitor<>(), arg);
            return null;
        }
    }

    public class AnnotationVisitor<R, A> implements org.syntax.stella.Absyn.Annotation.Visitor<R, A> {
        public R visit(org.syntax.stella.Absyn.InlineAnnotation inlineAnnotation, A arg) { /* Code for InlineAnnotation goes here */
            return null;
        }
    }

    public class ParamDeclVisitor<R extends Pair<String, T>, T extends Type, A extends ContextAndExpectedType> implements org.syntax.stella.Absyn.ParamDecl.Visitor<R, A> {
        public R visit(org.syntax.stella.Absyn.AParamDecl aParamDecl, A arg) { /* Code for AParamDecl goes here */
            arg.scopes.getLast().put(aParamDecl.stellaident_, aParamDecl.type_.accept(new TypeVisitor<>(), arg));
            return (R) new Pair<>(aParamDecl.stellaident_, arg.findVariableType(aParamDecl.stellaident_));
        }
    }

    public class ReturnTypeVisitor<R extends Type, A extends ContextAndExpectedType> implements org.syntax.stella.Absyn.ReturnType.Visitor<R, A> {
        public R visit(org.syntax.stella.Absyn.NoReturnType noReturnType, A arg) { /* Code for NoReturnType goes here */
            return null;
        }

        public R visit(org.syntax.stella.Absyn.SomeReturnType someReturnType, A arg) { /* Code for SomeReturnType goes here */
            return someReturnType.type_.accept(new TypeVisitor<>(), arg);
        }
    }

    public class ThrowTypeVisitor<R extends Type, A extends ContextAndExpectedType> implements org.syntax.stella.Absyn.ThrowType.Visitor<R, A> {
        public R visit(org.syntax.stella.Absyn.NoThrowType noThrowType, A arg) { /* Code for NoThrowType goes here */
            return null;
        }

        public R visit(org.syntax.stella.Absyn.SomeThrowType someThrowType, A arg) { /* Code for SomeThrowType goes here */
            for (var type : someThrowType.listtype_) {
                type.accept(new TypeVisitor<>(), arg);
            }
            return null;
        }
    }

    public class TypeVisitor<R extends Type, A extends ContextAndExpectedType> implements org.syntax.stella.Absyn.Type.Visitor<R, A> {
        public R visit(org.syntax.stella.Absyn.TypeFun typeFun, A arg) { /* Code for TypeFun goes here */
            var listType = new ListType();
            for (var type : typeFun.listtype_) {
                listType.add(type.accept(new TypeVisitor<>(), arg));
            }
            return (R) new TypeFun(listType, typeFun.type_.accept(new TypeVisitor<>(), arg));
        }

        @Override
        public R visit(TypeForAll p, A arg) {
            return null;
        }

        public R visit(org.syntax.stella.Absyn.TypeRec typeRec, A arg) { /* Code for TypeRec goes here */

            typeRec.type_.accept(new TypeVisitor<>(), arg);
            return null;
        }

        public R visit(org.syntax.stella.Absyn.TypeSum typeSum, A arg) { /* Code for TypeSum goes here */
            return (R) new TypeSum(typeSum.type_1.accept(new TypeVisitor<>(), arg), typeSum.type_2.accept(new TypeVisitor<>(), arg));
        }

        public R visit(org.syntax.stella.Absyn.TypeTuple typeTuple, A arg) { /* Code for TypeTuple goes here */
            for (var type : typeTuple.listtype_) {
                type.accept(new TypeVisitor<>(), arg);
            }
            return (R) typeTuple;
        }

        public R visit(org.syntax.stella.Absyn.TypeRecord typeRecord, A arg) { /* Code for TypeRecord goes here */
            for (var recordFieldType : typeRecord.listrecordfieldtype_) {
                recordFieldType.accept(new RecordFieldTypeVisitor<>(), arg);
            }
            return (R) typeRecord;
        }

        public R visit(org.syntax.stella.Absyn.TypeVariant typeVariant, A arg) { /* Code for TypeVariant goes here */
            var listVariantFieldType = new ListVariantFieldType();
            for (var variantFieldType : typeVariant.listvariantfieldtype_) {
                Pair<String, R> pair = variantFieldType.accept(new VariantFieldTypeVisitor<>(), arg);
                if (pair.getValue() == null) {
                    listVariantFieldType.add(new AVariantFieldType(pair.getKey(), new NoTyping()));
                } else {
                    listVariantFieldType.add(new AVariantFieldType(pair.getKey(), new SomeTyping(pair.getValue())));
                }
            }
            return (R) new TypeVariant(listVariantFieldType);
        }

        public R visit(org.syntax.stella.Absyn.TypeList typeList, A arg) { /* Code for TypeList goes here */
            var itemType = typeList.type_.accept(new TypeVisitor<>(), arg);
            var listType = new TypeList(itemType);
            return (R) listType;
        }

        public R visit(org.syntax.stella.Absyn.TypeBool typeBool, A arg) { /* Code for TypeBool goes here */
            return (R) typeBool;
        }

        public R visit(org.syntax.stella.Absyn.TypeNat typeNat, A arg) { /* Code for TypeNat goes here */
            return (R) typeNat;
        }

        public R visit(org.syntax.stella.Absyn.TypeUnit typeUnit, A arg) { /* Code for TypeUnit goes here */
            return (R) typeUnit;
        }

        @Override
        public R visit(TypeTop typeTop, A arg) {
            return null;
        }

        @Override
        public R visit(TypeBottom typeBottom, A arg) {
            return null;
        }

        @Override
        public R visit(TypeRef typeRef, A arg) {
            return (R) typeRef;
        }

        public R visit(org.syntax.stella.Absyn.TypeVar typeVar, A arg) { /* Code for TypeVar goes here */
            return (R) arg.findVariableType(typeVar.stellaident_);
        }

        @Override
        public R visit(TypeAuto p, A arg) {
            return null;
        }
    }

    public class MatchCaseVisitor<StellaString extends java.lang.String, A extends ContextAndExpectedType> implements org.syntax.stella.Absyn.MatchCase.Visitor<StellaString, A> {
        public StellaString visit(org.syntax.stella.Absyn.AMatchCase aMatchCase, A arg) { /* Code for AMatchCase goes here */
            StellaString stellaident = aMatchCase.pattern_.accept(new PatternVisitor<>(), arg);
            aMatchCase.expr_.accept(new ExprVisitor<>(), arg);
            return stellaident;
        }
    }

    public class OptionalTypingVisitor<R extends Type, A extends ContextAndExpectedType> implements org.syntax.stella.Absyn.OptionalTyping.Visitor<R, A> {
        public R visit(org.syntax.stella.Absyn.NoTyping noTyping, A arg) { /* Code for NoTyping goes here */
            return null;
        }

        public R visit(org.syntax.stella.Absyn.SomeTyping someTyping, A arg) { /* Code for SomeTyping goes here */
            return someTyping.type_.accept(new TypeVisitor<>(), arg);
        }
    }

    public class PatternDataVisitor<R extends String, A extends ContextAndExpectedType> implements org.syntax.stella.Absyn.PatternData.Visitor<R, A> {
        public R visit(org.syntax.stella.Absyn.NoPatternData noPatternData, A arg) { /* Code for NoPatternData goes here */
            if (arg.expected_pattern != null) {
                throw new RuntimeException("ERROR_UNEXPECTED_NULLARY_VARIANT_PATTERN");
            }
            return null;
        }

        public R visit(org.syntax.stella.Absyn.SomePatternData somePatternData, A arg) { /* Code for SomePatternData goes here */
            if (arg.expected_pattern == null) {
                throw new RuntimeException("ERROR_UNEXPECTED_NON_NULLARY_VARIANT_PATTERN");
            }
            somePatternData.pattern_.accept(new PatternVisitor<>(), arg);
            return null;
        }
    }

    public class ExprDataVisitor<R extends Type, A extends ContextAndExpectedType> implements org.syntax.stella.Absyn.ExprData.Visitor<R, A> {
        public R visit(org.syntax.stella.Absyn.NoExprData noExprData, A arg) { /* Code for NoExprData goes here */
            if (arg.expected == null) {
                return null;
            }
            throw new RuntimeException("ERROR_MISSING_DATA_FOR_LABEL");
        }

        public R visit(org.syntax.stella.Absyn.SomeExprData someExprData, A arg) { /* Code for SomeExprData goes here */
            if (arg.expected == null) {
                throw new RuntimeException("ERROR_UNEXPECTED_DATA_FOR_NULLARY_LABEL");
            }
            return someExprData.expr_.accept(new ExprVisitor<>(), arg);
        }
    }

    public class PatternVisitor<StellaString extends java.lang.String, A extends ContextAndExpectedType> implements org.syntax.stella.Absyn.Pattern.Visitor<StellaString, A> {

        public StellaString visit(org.syntax.stella.Absyn.PatternVariant patternVariant, A arg) { /* Code for PatternVariant goes here */
            if (arg.expected_pattern == null) {
                throw new RuntimeException("ERROR_UNEXPECTED_PATTERN_FOR_TYPE");
            }
            if (arg.expected_pattern instanceof TypeVariant typeVariant) {
                for (var variantFieldType : typeVariant.listvariantfieldtype_) {
                    var type = variantFieldType.accept(new VariantFieldTypeVisitor<>(), arg);
                    if (type.getKey().equals(patternVariant.stellaident_)) {
                        arg.expected_pattern = type.getValue();
                        patternVariant.patterndata_.accept(new PatternDataVisitor<>(), arg);
                        return null;
                    }
                }
            }
            throw new RuntimeException("ERROR_UNEXPECTED_PATTERN_FOR_TYPE");
        }

        public StellaString visit(org.syntax.stella.Absyn.PatternInl patternInl, A arg) { /* Code for PatternInl goes here */
            if (arg.expected_pattern == null) {
                throw new RuntimeException("ERROR_UNEXPECTED_PATTERN_FOR_TYPE");
            }
            if (arg.expected_pattern instanceof TypeSum typeSum) {
                arg.expected_pattern = typeSum.type_1;
                patternInl.pattern_.accept(new PatternVisitor<>(), arg);
                return null;
            }
            throw new RuntimeException("ERROR_UNEXPECTED_PATTERN_FOR_TYPE");
        }

        public StellaString visit(org.syntax.stella.Absyn.PatternInr patternInr, A arg) { /* Code for PatternInr goes here */
            if (arg.expected_pattern == null) {
                throw new RuntimeException("ERROR_UNEXPECTED_PATTERN_FOR_TYPE");
            }
            if (arg.expected_pattern instanceof TypeSum typeSum) {
                arg.expected_pattern = typeSum.type_2;
                patternInr.pattern_.accept(new PatternVisitor<>(), arg);
                return null;
            }
            throw new RuntimeException("ERROR_UNEXPECTED_PATTERN_FOR_TYPE");
        }

        public StellaString visit(org.syntax.stella.Absyn.PatternTuple patternTuple, A arg) { /* Code for PatternTuple goes here */
            for (var pattern : patternTuple.listpattern_) {
                pattern.accept(new PatternVisitor<>(), arg);
            }
            return null;
        }

        public StellaString visit(org.syntax.stella.Absyn.PatternRecord patternRecord, A arg) { /* Code for PatternRecord goes here */
            for (var labelledPattern : patternRecord.listlabelledpattern_) {
                labelledPattern.accept(new LabelledPatternVisitor<>(), arg);
            }
            return null;
        }

        public StellaString visit(org.syntax.stella.Absyn.PatternList patternList, A arg) { /* Code for PatternList goes here */
            for (var pattern : patternList.listpattern_) {
                pattern.accept(new PatternVisitor<>(), arg);
            }
            return null;
        }

        public StellaString visit(org.syntax.stella.Absyn.PatternCons patternCons, A arg) { /* Code for PatternCons goes here */
            patternCons.pattern_1.accept(new PatternVisitor<>(), arg);
            patternCons.pattern_2.accept(new PatternVisitor<>(), arg);
            return null;
        }

        public StellaString visit(org.syntax.stella.Absyn.PatternFalse patternFalse, A arg) { /* Code for PatternFalse goes here */
            return null;
        }

        public StellaString visit(org.syntax.stella.Absyn.PatternTrue patternTrue, A arg) { /* Code for PatternTrue goes here */
            return null;
        }

        public StellaString visit(org.syntax.stella.Absyn.PatternUnit patternUnit, A arg) { /* Code for PatternUnit goes here */
            return null;
        }

        public StellaString visit(org.syntax.stella.Absyn.PatternInt patternInt, A arg) { /* Code for PatternInt goes here */

            return null;
        }

        public StellaString visit(org.syntax.stella.Absyn.PatternSucc patternSucc, A arg) { /* Code for PatternSucc goes here */
            patternSucc.pattern_.accept(new PatternVisitor<>(), arg);
            return null;
        }

        public StellaString visit(org.syntax.stella.Absyn.PatternVar patternVar, A arg) { /* Code for PatternVar goes here */
            if (arg.expected_pattern == null) {
                throw new RuntimeException("ERROR_UNEXPECTED_PATTERN_FOR_TYPE");
            }
            arg.scopes.getLast().put(patternVar.stellaident_, arg.expected_pattern);
            return null;
        }

        @Override
        public StellaString visit(PatternCastAs p, A arg) {
            return null;
        }

        @Override
        public StellaString visit(PatternAsc p, A arg) {
            return null;
        }
    }

    public class LabelledPatternVisitor<R extends String, A extends ContextAndExpectedType> implements org.syntax.stella.Absyn.LabelledPattern.Visitor<R, A> {
        public R visit(org.syntax.stella.Absyn.ALabelledPattern aLabelledPattern, A arg) { /* Code for ALabelledPattern goes here */
            aLabelledPattern.pattern_.accept(new PatternVisitor<>(), arg);
            return null;
        }
    }

    public class BindingVisitor<R extends Pair<String, ReT>, ReT extends Type, A extends ContextAndExpectedType> implements org.syntax.stella.Absyn.Binding.Visitor<R, A> {
        public R visit(org.syntax.stella.Absyn.ABinding aBinding, A arg) { /* Code for ABinding goes here */
            ReT type = aBinding.expr_.accept(new ExprVisitor<>(), arg);
            arg.expected = null;
            return (R) new Pair<>(aBinding.stellaident_, type);
        }
    }

    public class ExprVisitor<R extends Type, A extends ContextAndExpectedType> implements org.syntax.stella.Absyn.Expr.Visitor<R, A> {
        public R visit(org.syntax.stella.Absyn.Sequence sequence, A arg) { /* Code for Sequence goes here */
            var expectedType = arg.expected;
            arg.expected = TypeKind.TYPE_UNIT.getStellaType();
            sequence.expr_1.accept(new ExprVisitor<>(), arg);

            arg.expected = expectedType;
            var actualType = sequence.expr_2.accept(new ExprVisitor<>(), arg);

            checkType(actualType, expectedType);
            return (R) actualType;
        }

        @Override
        public R visit(Assign assign, A arg) {
            checkType(TypeKind.TYPE_UNIT.getStellaType(), arg.expected);

            arg.expected = null;
            var actualReferenceType = assign.expr_1.accept(new ExprVisitor<>(), arg);

            if (!(actualReferenceType instanceof TypeRef typeRef)) {
                throw new RuntimeException("ERROR_NOT_A_REFERENCE");
            }

            var expectedValueType = typeRef.type_;
            arg.expected = expectedValueType;
            var actualValueType = assign.expr_2.accept(new ExprVisitor<>(), arg);

            checkType(actualValueType, expectedValueType);
            return (R) TypeKind.TYPE_UNIT.getStellaType();
        }

        public R visit(org.syntax.stella.Absyn.If anIf, A arg) { /* Code for If goes here */
            var expectedType = arg.expected;
            arg.expected = TypeKind.TYPE_BOOL.getStellaType();

            var conditionType = anIf.expr_1.accept(new ExprVisitor<>(), arg);
            checkType(conditionType, TypeKind.TYPE_BOOL.getStellaType());

            arg.expected = expectedType;
            var thenType = anIf.expr_2.accept(new ExprVisitor<>(), arg);

            arg.expected = thenType;
            var elseType = anIf.expr_3.accept(new ExprVisitor<>(), arg);

            checkType(thenType, elseType);
            return (R) thenType;
        }

        public R visit(org.syntax.stella.Absyn.Let let, A arg) { /* Code for Let goes here */
            arg.scopes.add(new HashMap<>());

            var expectedType = arg.expected;
            for (var patternBinding : let.listpatternbinding_) {
                arg.expected = null;
                patternBinding.accept(new PatternBindingVisitor<>(), arg);
            }
            arg.expected = expectedType;
            var actualType = let.expr_.accept(new ExprVisitor<>(), arg);
            checkType(actualType, expectedType);

            arg.scopes.removeLast();
            return (R) actualType;
        }

        public R visit(org.syntax.stella.Absyn.LetRec letRec, A arg) { /* Code for LetRec goes here */
            for (var patternBinding : letRec.listpatternbinding_) {
                patternBinding.accept(new PatternBindingVisitor<>(), arg);
            }
            letRec.expr_.accept(new ExprVisitor<>(), arg);
            return null;
        }

        @Override
        public R visit(TypeAbstraction typeAbstraction, A arg) {
            return null;
        }

        public R visit(org.syntax.stella.Absyn.LessThan lessThan, A arg) { /* Code for LessThan goes here */
            lessThan.expr_1.accept(new ExprVisitor<>(), arg);
            lessThan.expr_2.accept(new ExprVisitor<>(), arg);
            return null;
        }

        public R visit(org.syntax.stella.Absyn.LessThanOrEqual lessThanOrEqual, A arg) { /* Code for LessThanOrEqual goes here */
            lessThanOrEqual.expr_1.accept(new ExprVisitor<>(), arg);
            lessThanOrEqual.expr_2.accept(new ExprVisitor<>(), arg);
            return null;
        }

        public R visit(org.syntax.stella.Absyn.GreaterThan greaterThan, A arg) { /* Code for GreaterThan goes here */
            greaterThan.expr_1.accept(new ExprVisitor<>(), arg);
            greaterThan.expr_2.accept(new ExprVisitor<>(), arg);
            return null;
        }

        public R visit(org.syntax.stella.Absyn.GreaterThanOrEqual greaterThanOrEqual, A arg) { /* Code for GreaterThanOrEqual goes here */
            greaterThanOrEqual.expr_1.accept(new ExprVisitor<>(), arg);
            greaterThanOrEqual.expr_2.accept(new ExprVisitor<>(), arg);
            return null;
        }

        public R visit(org.syntax.stella.Absyn.Equal equal, A arg) { /* Code for Equal goes here */
            equal.expr_1.accept(new ExprVisitor<>(), arg);
            equal.expr_2.accept(new ExprVisitor<>(), arg);
            return null;
        }

        public R visit(org.syntax.stella.Absyn.NotEqual notEqual, A arg) { /* Code for NotEqual goes here */
            notEqual.expr_1.accept(new ExprVisitor<>(), arg);
            notEqual.expr_2.accept(new ExprVisitor<>(), arg);
            return null;
        }

        public R visit(org.syntax.stella.Absyn.TypeAsc typeAsc, A arg) { /* Code for TypeAsc goes here */
            var type = typeAsc.type_;
            checkType(type, arg.expected);

            arg.expected = type;
            var actualType = typeAsc.expr_.accept(new ExprVisitor<>(), arg);
            checkType(actualType, type);

            return (R) actualType;
        }

        @Override
        public R visit(TypeCast typeCast, A arg) {
            return null;
        }

        public R visit(org.syntax.stella.Absyn.Abstraction abstraction, A arg) { /* Code for Abstraction goes here */
            arg.scopes.add(new HashMap<>());

            var listParamTypes = new ListType();
            for (var paramDecl : abstraction.listparamdecl_) {
                listParamTypes.add(paramDecl.accept(new ParamDeclVisitor<>(), arg).getValue());
            }
            if (arg.expected != null) {
                if (arg.expected instanceof TypeFun typeFun) {
                    if (typeFun.listtype_.size() != abstraction.listparamdecl_.size()) {
                        throw new RuntimeException("ERROR_INCORRECT_NUMBER_OF_ARGUMENTS");
                    }
                    for (var i = 0; i < typeFun.listtype_.size(); i++) {
                        try {
                            checkType(listParamTypes.get(i), typeFun.listtype_.get(i));
                        } catch (Exception e) {
                            throw new RuntimeException("ERROR_UNEXPECTED_TYPE_FOR_PARAMETER");
                        }
                    }
                    var result = arg.expected;

                    arg.expected = typeFun.type_;
                    var returnType = abstraction.expr_.accept(new ExprVisitor<>(), arg);
                    checkType(returnType, typeFun.type_);

                    arg.scopes.removeLast();
                    return (R) result;
                } else {
                    throw new RuntimeException("ERROR_UNEXPECTED_LAMBDA");
                }
            }
            var returnType = abstraction.expr_.accept(new ExprVisitor<>(), arg);

            arg.scopes.removeLast();
            return (R) new TypeFun(listParamTypes, returnType);
        }

        public R visit(org.syntax.stella.Absyn.Variant variant, A arg) { /* Code for Variant goes here */
            if (arg.expected == null) {
                throw new RuntimeException("ERROR_AMBIGUOUS_VARIANT_TYPE");
            }
            if (arg.expected instanceof TypeVariant typeVariant) {
                for (var typeVar : typeVariant.listvariantfieldtype_) {
                    arg.expected = null;
                    var var = typeVar.accept(new VariantFieldTypeVisitor<>(), arg);

                    if (var.getKey().equals(variant.stellaident_)) {
                        arg.expected = var.getValue();
                        var type = variant.exprdata_.accept(new ExprDataVisitor<>(), arg);
                        checkType(type, var.getValue());

                        return (R) typeVariant;
                    }
                }
                throw new RuntimeException("ERROR_UNEXPECTED_VARIANT_LABEL");
            }
            throw new RuntimeException("ERROR_UNEXPECTED_VARIANT");
        }

        public R visit(org.syntax.stella.Absyn.Match match, A arg) { /* Code for Match goes here */
            if (match.listmatchcase_.isEmpty()) {
                throw new RuntimeException("ERROR_ILLEGAL_EMPTY_MATCHING");
            }
            var expectedType = arg.expected;

            arg.expected = null;
            var actualType = match.expr_.accept(new ExprVisitor<>(), arg);

            for (var listMatchCase : match.listmatchcase_) {
                arg.scopes.add(new HashMap<>());

                var aMatchCase = (AMatchCase) listMatchCase;
                arg.expected = null;
                arg.expected_pattern = actualType;
                aMatchCase.pattern_.accept(new PatternVisitor<>(), arg);

                arg.expected = expectedType;
                arg.expected_pattern = null;
                var actualMatchCaseType = aMatchCase.expr_.accept(new ExprVisitor<>(), arg);
                checkType(actualMatchCaseType, expectedType);

                if (expectedType == null) {
                    expectedType = actualMatchCaseType;
                }

                arg.scopes.removeLast();
            }
            checkExhaustiveness(match.listmatchcase_, actualType);
            return (R) expectedType;
        }

        public R visit(org.syntax.stella.Absyn.List list, A arg) { /* Code for List goes here */
            if (arg.expected == null) {
                Type elementType = null;
                for (var expression : list.listexpr_) {
                    arg.expected = elementType;
                    var actualListType = expression.accept(new ExprVisitor<>(), arg);
                    checkType(actualListType, elementType);

                    if (elementType == null) {
                        elementType = actualListType;
                    }
                }
                if (elementType == null) throw new RuntimeException("ERROR_AMBIGUOUS_LIST_TYPE");
                return (R) new TypeList(elementType);
            } else {
                if (arg.expected instanceof TypeList typeList) {
                    var expectedItemType = typeList.type_;
                    for (var expression : list.listexpr_) {
                        arg.expected = expectedItemType;
                        var actualListType = expression.accept(new ExprVisitor<>(), arg);
                        checkType(actualListType, expectedItemType);
                    }
                    return (R) typeList;
                } else {
                    throw new RuntimeException("ERROR_UNEXPECTED_LIST");
                }
            }
        }

        public R visit(org.syntax.stella.Absyn.Add add, A arg) { /* Code for Add goes here */
            add.expr_1.accept(new ExprVisitor<>(), arg);
            add.expr_2.accept(new ExprVisitor<>(), arg);
            return null;
        }

        public R visit(org.syntax.stella.Absyn.Subtract subtract, A arg) { /* Code for Subtract goes here */
            subtract.expr_1.accept(new ExprVisitor<>(), arg);
            subtract.expr_2.accept(new ExprVisitor<>(), arg);
            return null;
        }

        public R visit(org.syntax.stella.Absyn.LogicOr logicOr, A arg) { /* Code for LogicOr goes here */
            logicOr.expr_1.accept(new ExprVisitor<>(), arg);
            logicOr.expr_2.accept(new ExprVisitor<>(), arg);
            return null;
        }

        public R visit(org.syntax.stella.Absyn.Multiply multiply, A arg) { /* Code for Multiply goes here */
            multiply.expr_1.accept(new ExprVisitor<>(), arg);
            multiply.expr_2.accept(new ExprVisitor<>(), arg);
            return null;
        }

        public R visit(org.syntax.stella.Absyn.Divide divide, A arg) { /* Code for Divide goes here */
            divide.expr_1.accept(new ExprVisitor<>(), arg);
            divide.expr_2.accept(new ExprVisitor<>(), arg);
            return null;
        }

        public R visit(org.syntax.stella.Absyn.LogicAnd logicAnd, A arg) { /* Code for LogicAnd goes here */
            logicAnd.expr_1.accept(new ExprVisitor<>(), arg);
            logicAnd.expr_2.accept(new ExprVisitor<>(), arg);
            return null;
        }

        @Override
        public R visit(Ref ref, A arg) {
            var expectedType = arg.expected;
            if (arg.expected != null) {
                if (!(arg.expected instanceof TypeRef typeRef)) {
                    throw new RuntimeException("ERROR_UNEXPECTED_TYPE_FOR_EXPRESSION");
                }
                arg.expected = typeRef.type_;
            }

            var actualType = ref.expr_.accept(new ExprVisitor<>(), arg);
            checkType(new TypeRef(actualType), expectedType);
            return (R) new TypeRef(actualType);
        }

        @Override
        public R visit(Deref deref, A arg) {
            var expectedType = arg.expected;
            if (arg.expected != null) {
                arg.expected = new TypeRef(arg.expected);
            }

            var actualType = deref.expr_.accept(new ExprVisitor<>(), arg);
            if (!(actualType instanceof TypeRef typeRef)) {
                throw new RuntimeException("ERROR_NOT_A_REFERENCE");
            }
            checkType(typeRef.type_, expectedType);
            return (R) typeRef.type_;
        }

        public R visit(org.syntax.stella.Absyn.Application application, A arg) { /* Code for Application goes here */
            var expectedType = arg.expected;

            arg.expected = null;
            var funcType = application.expr_.accept(new ExprVisitor<>(), arg);

            if (funcType instanceof TypeFun typeFun) {
                if (typeFun.listtype_.size() != application.listexpr_.size()) {
                    throw new RuntimeException("ERROR_INCORRECT_NUMBER_OF_ARGUMENTS");
                }
                for (var i = 0; i < typeFun.listtype_.size(); i++) {
                    var argumentType = typeFun.listtype_.get(i);

                    arg.expected = argumentType;
                    var actualType = application.listexpr_.get(i).accept(new ExprVisitor<>(), arg);
                    checkType(actualType, argumentType);
                }
                checkType(typeFun.type_, expectedType);
                arg.expected = null;
                return (R) typeFun.type_;
            } else {
                throw new RuntimeException("ERROR_NOT_A_FUNCTION");
            }
        }

        @Override
        public R visit(TypeApplication typeApplication, A arg) {
            return null;
        }

        public R visit(org.syntax.stella.Absyn.DotRecord dotRecord, A arg) { /* Code for DotRecord goes here */
            var expectedType = arg.expected;

            arg.expected = null;
            var actualRecordType = dotRecord.expr_.accept(new ExprVisitor<>(), arg);
            if (actualRecordType instanceof TypeRecord typeRecord) {
                for (var recordFieldType : typeRecord.listrecordfieldtype_) {
                    arg.expected = null;
                    var actualRecordFieldType = recordFieldType.accept(new RecordFieldTypeVisitor<>(), arg);

                    if (actualRecordFieldType.getKey().equals(dotRecord.stellaident_)) {
                        checkType(actualRecordFieldType.getValue(), expectedType);
                        return (R) actualRecordFieldType.getValue();
                    }
                }
                throw new RuntimeException("ERROR_UNEXPECTED_FIELD_ACCESS");
            } else {
                throw new RuntimeException("ERROR_NOT_A_RECORD");
            }
        }

        public R visit(org.syntax.stella.Absyn.DotTuple dotTuple, A arg) { /* Code for DotTuple goes here */
            var expectedType = arg.expected;

            arg.expected = null;
            var actualTupleType = dotTuple.expr_.accept(new ExprVisitor<>(), arg);
            if (actualTupleType instanceof TypeTuple typeTuple) {
                if (dotTuple.integer_ > typeTuple.listtype_.size() || dotTuple.integer_ < 1) {
                    throw new RuntimeException("ERROR_TUPLE_INDEX_OUT_OF_BOUNDS");
                }
                var actualType = typeTuple.listtype_.get(dotTuple.integer_ - 1);
                checkType(actualType, expectedType);
                return (R) actualType;
            } else {
                throw new RuntimeException("ERROR_NOT_A_TUPLE");
            }
        }

        public R visit(org.syntax.stella.Absyn.Tuple tuple, A arg) { /* Code for Tuple goes here */
            var listType = new ListType();
            if (arg.expected == null) {
                for (var expr : tuple.listexpr_) {
                    arg.expected = null;
                    var type = expr.accept(new ExprVisitor<>(), arg);
                    listType.add(type);
                }
                return (R) new TypeTuple(listType);
            } else {
                if (arg.expected instanceof TypeTuple typeTuple) {
                    if (typeTuple.listtype_.size() != tuple.listexpr_.size()) {
                        throw new RuntimeException("ERROR_UNEXPECTED_TUPLE_LENGTH");
                    }

                    for (var i = 0; i < typeTuple.listtype_.size(); i++) {
                        arg.expected = typeTuple.listtype_.get(i);
                        var type = tuple.listexpr_.get(i).accept(new ExprVisitor<>(), arg);
                        checkType(type, typeTuple.listtype_.get(i));
                        listType.add(type);
                    }
                    return (R) new TypeTuple(listType);
                } else {
                    throw new RuntimeException("ERROR_UNEXPECTED_TUPLE");
                }
            }
        }

        public R visit(org.syntax.stella.Absyn.Record aRecord, A arg) { /* Code for Record goes here */
            if (arg.expected == null) {
                var listRecordFieldType = new ListRecordFieldType();
                for (var binding : aRecord.listbinding_) {
                    arg.expected = null;
                    var actualType = binding.accept(new BindingVisitor<>(), arg);
                    listRecordFieldType.add(new ARecordFieldType(actualType.getKey(), actualType.getValue()));
                }
                arg.expected = null;
                return (R) new TypeRecord(listRecordFieldType);
            } else {
                if (arg.expected instanceof TypeRecord typeRecord) {
                    if (aRecord.listbinding_.size() > typeRecord.listrecordfieldtype_.size()) {
                        throw new RuntimeException("ERROR_UNEXPECTED_RECORD_FIELDS");
                    }
                    if (aRecord.listbinding_.size() < typeRecord.listrecordfieldtype_.size()) {
                        throw new RuntimeException("ERROR_MISSING_RECORD_FIELDS");
                    }
                    for (var i = 0; i < aRecord.listbinding_.size(); i++) {
                        arg.expected = null;
                        var binding = aRecord.listbinding_.get(i).accept(new BindingVisitor<>(), arg);

                        arg.expected = null;
                        var expectedBinding = typeRecord.listrecordfieldtype_.get(i).accept(new RecordFieldTypeVisitor<>(), arg);

                        if (!binding.getKey().equals(expectedBinding.getKey())) {
                            throw new RuntimeException("ERROR_UNEXPECTED_RECORD_FIELDS");
                        }
                        checkType(binding.getValue(), expectedBinding.getValue());
                    }
                    return (R) typeRecord;
                } else {
                    throw new RuntimeException("ERROR_UNEXPECTED_RECORD");
                }
            }
        }

        public R visit(org.syntax.stella.Absyn.ConsList consList, A arg) { /* Code for ConsList goes here */
            if (arg.expected == null) {
                var itemType = consList.expr_1.accept(new ExprVisitor<>(), arg);

                arg.expected = new TypeList(itemType);
                var listType = consList.expr_2.accept(new ExprVisitor<>(), arg);
                checkType(listType, new TypeList(itemType));

                return (R) listType;
            } else {
                if (arg.expected instanceof TypeList typeList) {
                    var expectedTypeListType = typeList.type_;
                    var expectedListType = new TypeList(expectedTypeListType);

                    arg.expected = expectedTypeListType;
                    var actualType = consList.expr_1.accept(new ExprVisitor<>(), arg);
                    checkType(actualType, expectedTypeListType);

                    arg.expected = expectedListType;
                    var listType = consList.expr_2.accept(new ExprVisitor<>(), arg);
                    checkType(listType, expectedListType);

                    return (R) listType;
                }
                throw new RuntimeException("ERROR_UNEXPECTED_LIST");
            }
        }

        public R visit(org.syntax.stella.Absyn.Head head, A arg) { /* Code for Head goes here */
            if (arg.expected != null) {
                var listExpectedType = new TypeList(arg.expected);

                arg.expected = listExpectedType;
                var actualType = head.expr_.accept(new ExprVisitor<>(), arg);
                checkType(actualType, listExpectedType);

                return (R) listExpectedType.type_;
            }
            var actualType = head.expr_.accept(new ExprVisitor<>(), arg);
            if (actualType instanceof TypeList) {
                return (R) actualType;
            }
            throw new RuntimeException("ERROR_NOT_A_LIST");
        }

        public R visit(org.syntax.stella.Absyn.IsEmpty isEmpty, A arg) { /* Code for IsEmpty goes here */
            checkType(TypeKind.TYPE_BOOL.getStellaType(), arg.expected);

            arg.expected = null;
            var maybeList = isEmpty.expr_.accept(new ExprVisitor<>(), arg);

            if (maybeList instanceof TypeList) {
                return (R) TypeKind.TYPE_BOOL.getStellaType();
            }
            throw new RuntimeException("ERROR_NOT_A_LIST");
        }

        public R visit(org.syntax.stella.Absyn.Tail tail, A arg) { /* Code for Tail goes here */
            if (arg.expected != null) {
                var expectedType = arg.expected;
                var actualType = tail.expr_.accept(new ExprVisitor<>(), arg);
                checkType(actualType, expectedType);

                return (R) actualType;
            }
            var actualType = tail.expr_.accept(new ExprVisitor<>(), arg);
            if (actualType instanceof TypeList) {
                return (R) actualType;
            }
            throw new RuntimeException("ERROR_NOT_A_LIST");
        }

        @Override
        public R visit(Panic panic, A arg) {
            if (arg.expected != null) {
                return (R) arg.expected;
            }
            throw new RuntimeException("ERROR_AMBIGUOUS_PANIC_TYPE");
        }

        @Override
        public R visit(Throw aThrow, A arg) {
            var exceptionType = arg.getException();
            if (exceptionType == null) {
                throw new RuntimeException("ERROR_EXCEPTION_TYPE_NOT_DECLARED");
            }

            if (arg.expected == null) {
                throw new RuntimeException("ERROR_AMBIGUOUS_THROW_TYPE");
            }

            var expectedType = arg.expected;
            arg.expected = exceptionType;
            aThrow.expr_.accept(new ExprVisitor<>(), arg);
            return (R) expectedType;
        }

        @Override
        public R visit(TryCatch tryCatch, A arg) {

            var exceptionType = arg.getException();
            if (exceptionType == null) {
                throw new RuntimeException("ERROR_EXCEPTION_TYPE_NOT_DECLARED");
            }

            var expectedType = arg.expected;
            var actualTryType = tryCatch.expr_1.accept(new ExprVisitor<>(), arg);
            checkType(actualTryType, expectedType);

            arg.scopes.add(new HashMap<>());
            arg.expected_pattern = exceptionType;
            tryCatch.pattern_.accept(new PatternVisitor<>(), arg);

            arg.expected = expectedType;
            var actualWithType = tryCatch.expr_2.accept(new ExprVisitor<>(), arg);
            checkType(actualWithType, expectedType);

            arg.scopes.removeLast();
            checkType(actualTryType, actualWithType);

            return (R) actualTryType;
        }

        @Override
        public R visit(TryWith tryWith, A arg) {
            var expectedType = arg.expected;

            var actualTryType = tryWith.expr_1.accept(new ExprVisitor<>(), arg);
            checkType(actualTryType, expectedType);

            var actualWithType = tryWith.expr_2.accept(new ExprVisitor<>(), arg);
            checkType(actualWithType, expectedType);
            checkType(actualTryType, actualWithType);
            return (R) actualTryType;
        }

        public R visit(org.syntax.stella.Absyn.Inl inl, A arg) { /* Code for Inl goes here */
            if (arg.expected == null) {
                throw new RuntimeException("ERROR_AMBIGUOUS_SUM_TYPE");
            }
            if (arg.expected instanceof TypeSum typeSum) {
                var leftType = typeSum.type_1;
                arg.expected = leftType;
                var actualType = inl.expr_.accept(new ExprVisitor<>(), arg);
                checkType(actualType, leftType);
                return (R) typeSum;
            }
            throw new RuntimeException("ERROR_UNEXPECTED_INJECTION");
        }

        public R visit(org.syntax.stella.Absyn.Inr inr, A arg) { /* Code for Inr goes here */
            if (arg.expected == null) {
                throw new RuntimeException("ERROR_AMBIGUOUS_SUM_TYPE");
            }
            if (arg.expected instanceof TypeSum typeSum) {
                var rightType = typeSum.type_2;
                arg.expected = rightType;
                var actualType = inr.expr_.accept(new ExprVisitor<>(), arg);
                arg.expected = null;
                checkType(actualType, rightType);
                return (R) typeSum;
            }
            throw new RuntimeException("ERROR_UNEXPECTED_INJECTION");
        }

        public R visit(org.syntax.stella.Absyn.Succ succ, A arg) { /* Code for Succ goes here */
            checkType(TypeKind.TYPE_NAT.getStellaType(), arg.expected);
            var type = succ.expr_.accept(new ExprVisitor<>(), arg);
            checkType(type, TypeKind.TYPE_NAT.getStellaType());
            return (R) type;
        }

        public R visit(org.syntax.stella.Absyn.LogicNot logicNot, A arg) { /* Code for LogicNot goes here */
            logicNot.expr_.accept(new ExprVisitor<>(), arg);
            return null;
        }

        public R visit(org.syntax.stella.Absyn.Pred pred, A arg) { /* Code for Pred goes here */
            pred.expr_.accept(new ExprVisitor<>(), arg);
            return null;
        }

        public R visit(org.syntax.stella.Absyn.IsZero isZero, A arg) { /* Code for IsZero goes here */
            checkType(TypeKind.TYPE_BOOL.getStellaType(), arg.expected);
            arg.expected = TypeKind.TYPE_NAT.getStellaType();
            checkType(isZero.expr_.accept(new ExprVisitor<>(), arg), TypeKind.TYPE_NAT.getStellaType());
            return TypeKind.TYPE_BOOL.getStellaType().accept(new TypeVisitor<>(), arg);
        }

        public R visit(org.syntax.stella.Absyn.Fix fix, A arg) { /* Code for Fix goes here */
            var expectedType = arg.expected;
            if (arg.expected != null) {
                var newListType = new ListType();
                newListType.add(expectedType);

                var newExpectedType = new TypeFun(newListType, expectedType);
                arg.expected = newExpectedType;
                var actualType = fix.expr_.accept(new ExprVisitor<>(), arg);

                checkType(actualType, newExpectedType);
                return (R) expectedType;
            }

            if (fix.expr_.accept(new ExprVisitor<>(), arg) instanceof TypeFun typeFun) {
                if (typeFun.listtype_.size() != 1) {
                    throw new RuntimeException("ERROR_UNEXPECTED_NUMBER_OF_PARAMETERS_IN_LAMBDA");
                }
                checkType(typeFun.type_, typeFun.listtype_.get(0));
                return (R) typeFun.type_;
            }
            throw new RuntimeException("ERROR_NOT_A_FUNCTION");
        }

        public R visit(org.syntax.stella.Absyn.NatRec natRec, A arg) { /* Code for NatRec goes here */
            var expectedType = arg.expected;

            arg.expected = TypeKind.TYPE_NAT.getStellaType();
            var natType = natRec.expr_1.accept(new ExprVisitor<>(), arg);
            checkType(natType, TypeKind.TYPE_NAT.getStellaType());

            arg.expected = expectedType;
            var type = natRec.expr_2.accept(new ExprVisitor<>(), arg);
            checkType(type, expectedType);

            var functionType = new TypeFun(new ListType(), new TypeFun(new ListType(), type));
            functionType.listtype_.add(natType);
            ((TypeFun) functionType.type_).listtype_.add(type);

            arg.expected = null;
            checkType(natRec.expr_3.accept(new ExprVisitor<>(), arg), functionType);
            return (R) type;
        }

        public R visit(org.syntax.stella.Absyn.Fold fold, A arg) { /* Code for Fold goes here */
            fold.type_.accept(new TypeVisitor<>(), arg);
            fold.expr_.accept(new ExprVisitor<>(), arg);
            return null;
        }

        public R visit(org.syntax.stella.Absyn.Unfold unfold, A arg) { /* Code for Unfold goes here */
            unfold.type_.accept(new TypeVisitor<>(), arg);
            unfold.expr_.accept(new ExprVisitor<>(), arg);
            return null;
        }

        public R visit(org.syntax.stella.Absyn.ConstTrue constTrue, A arg) { /* Code for ConstTrue goes here */
            checkType(TypeKind.TYPE_BOOL.getStellaType(), arg.expected);
            return (R) TypeKind.TYPE_BOOL.getStellaType();
        }

        public R visit(org.syntax.stella.Absyn.ConstFalse constFalse, A arg) { /* Code for ConstFalse goes here */
            checkType(TypeKind.TYPE_BOOL.getStellaType(), arg.expected);
            return (R) TypeKind.TYPE_BOOL.getStellaType();
        }

        public R visit(org.syntax.stella.Absyn.ConstUnit constUnit, A arg) { /* Code for ConstUnit goes here */
            checkType(TypeKind.TYPE_UNIT.getStellaType(), arg.expected);
            return (R) TypeKind.TYPE_UNIT.getStellaType();
        }

        public R visit(org.syntax.stella.Absyn.ConstInt constInt, A arg) { /* Code for ConstInt goes here */
            checkType(TypeKind.TYPE_NAT.getStellaType(), arg.expected);
            if (constInt.integer_ < 0) {
                throw new RuntimeException("ERROR_ILLEGAL_NEGATIVE_LITERAL");
            }
            return TypeKind.TYPE_NAT.getStellaType().accept(new TypeVisitor<>(), arg);
        }

        @Override
        public R visit(ConstMemory constMemory, A arg) {
            if (arg.expected == null) {
                throw new RuntimeException("ERROR_AMBIGUOUS_REFERENCE_TYPE");
            }
            if (!(arg.expected instanceof TypeRef typeRef)) {
                throw new RuntimeException("ERROR_UNEXPECTED_MEMORY_ADDRESS");
            }
            return (R) typeRef;
        }

        public R visit(org.syntax.stella.Absyn.Var variable, A arg) { /* Code for Var goes here */
            var type = arg.findVariableType(variable.stellaident_);
            if (type == null) {
                throw new RuntimeException("ERROR_UNDEFINED_VARIABLE");
            }
            checkType(type, arg.expected);
            return (R) type;
        }

        @Override
        public R visit(TryCastAs p, A arg) {
            return null;
        }
    }

    public class PatternBindingVisitor<R extends Type, A extends ContextAndExpectedType> implements org.syntax.stella.Absyn.PatternBinding.Visitor<R, A> {
        public R visit(org.syntax.stella.Absyn.APatternBinding aPatternBinding, A arg) { /* Code for APatternBinding goes here */
            arg.expected_pattern = aPatternBinding.expr_.accept(new ExprVisitor<>(), arg);
            ;
            aPatternBinding.pattern_.accept(new PatternVisitor<>(), arg);
            return null;
        }
    }

    public class VariantFieldTypeVisitor<R extends Pair<String, ReT>, ReT extends Type, A extends ContextAndExpectedType> implements org.syntax.stella.Absyn.VariantFieldType.Visitor<R, A> {
        public R visit(org.syntax.stella.Absyn.AVariantFieldType aVariantFieldType, A arg) { /* Code for AVariantFieldType goes here */
            ReT type = aVariantFieldType.optionaltyping_.accept(new OptionalTypingVisitor<>(), arg);
            return (R) new Pair<>(aVariantFieldType.stellaident_, type);
        }
    }

    public class RecordFieldTypeVisitor<R extends Pair<String, ReT>, ReT extends Type, A extends ContextAndExpectedType> implements org.syntax.stella.Absyn.RecordFieldType.Visitor<R, A> {
        public R visit(org.syntax.stella.Absyn.ARecordFieldType aRecordFieldType, A arg) { /* Code for ARecordFieldType goes here */
            return (R) new Pair<>(aRecordFieldType.stellaident_, aRecordFieldType.type_);
        }
    }
}
