package SymbolTable;

import syntaxtree.*;
import visitor.GJDepthFirst;
import java.util.*;

public class MiniJavaVisitorTypeCheck extends GJDepthFirst<String, Method>{

    // Type Checking
    SymbolTable symtable;
    ClassContents cl;
    Method mth;
    boolean isFine;


    public MiniJavaVisitorTypeCheck(SymbolTable symtable) {
        this.symtable = symtable;
        isFine = true;
    }

    boolean subClass(String classA, String classB) {
		if (classA.equals(classB))
			return true;
        ClassContents cl = symtable.getClass(classA);
		if (cl == null || cl.parent == null)
			return false;
		
        return subClass(cl.parent.name, classB);
	}

    boolean typeChecks(String typeA, String typeB){
        if (typeA == "int" && typeA == "int[]" && typeA == "boolean")
            return typeA.equals(typeB);
        return subClass(typeA, typeB);
    }

    /**
    * f0 -> MainClass()
    * f1 -> ( TypeDeclaration() )*
    * f2 -> <EOF>
    */
    public String visit(Goal n, Method argu) {
        super.visit(n, argu);
        if(!isFine)
            return "not";
        return "ok";
    }
        
    /**
    * f0 -> "class"
    * f1 -> Identifier()
    * f2 -> "{"
    * f3 -> "public"
    * f4 -> "static"
    * f5 -> "void"
    * f6 -> "main"
    * f7 -> "("
    * f8 -> "String"
    * f9 -> "["
    * f10 -> "]"
    * f11 -> Identifier()
    * f12 -> ")"
    * f13 -> "{"
    * f14 -> ( VarDeclaration() )*
    * f15 -> ( Statement() )*
    * f16 -> "}"
    * f17 -> "}"
    */
    public String visit(MainClass n, Method argu) {
       
        String name = n.f1.accept(this, argu);
        cl = symtable.getClass(name);
        mth = cl.getMethod("main");

        n.f15.accept(this, argu);
        return null;
    }
    
    /**
    * f0 -> "class"
    * f1 -> Identifier()
    * f2 -> "{"
    * f3 -> ( VarDeclaration() )*
    * f4 -> ( MethodDeclaration() )*
    * f5 -> "}"
    */
    public String visit(ClassDeclaration n, Method argu) {
        String name = n.f1.accept(this, argu);
        cl = symtable.getClass(name);

        n.f4.accept(this, argu);
        return null;
    }

    /**
    * f0 -> "class"
    * f1 -> Identifier()
    * f2 -> "extends"
    * f3 -> Identifier()
    * f4 -> "{"
    * f5 -> ( VarDeclaration() )*
    * f6 -> ( MethodDeclaration() )*
    * f7 -> "}"
    */
    public String visit(ClassExtendsDeclaration n, Method argu) {
        String name = n.f1.accept(this, argu);
        cl = symtable.getClass(name);

        n.f6.accept(this, argu);
        return null;
    }

    /**
    * f0 -> "public"
    * f1 -> Type()
    * f2 -> Identifier()
    * f3 -> "("
    * f4 -> ( FormalParameterList() )?
    * f5 -> ")"
    * f6 -> "{"
    * f7 -> ( VarDeclaration() )*
    * f8 -> ( Statement() )*
    * f9 -> "return"
    * f10 -> Expression()
    * f11 -> ";"
    * f12 -> "}"
    */
    public String visit(MethodDeclaration n, Method argu) {
        String name = n.f2.accept(this, argu);
        mth = cl.getMethod(name);

        n.f8.accept(this, argu);
        String retType = n.f10.accept(this, argu);
        if( !typeChecks(retType, mth.type)){
            System.out.println("Returned value in method "+mth.name+" should be of type "+mth.type+", not "+retType+".");
            isFine = false;            
            //System.exit(1);
        }
        return null;
    }

    /**
    * f0 -> Identifier()
    * f1 -> "="
    * f2 -> Expression()
    * f3 -> ";"
    */
    public String visit(AssignmentStatement n, Method argu) {
        String name = n.f0.accept(this, argu);
        Variable var = mth.getVar(name);
        if (var == null)
            var = cl.getVar(name);
        
        if (var == null){
            System.out.println("Variable "+name+" has not been declared.");
            isFine = false;            
            //System.exit(1);
        }

        String retType = n.f2.accept(this, argu);
        if( !typeChecks(retType, var.type)){
            System.out.println("Variable "+name+" is not of type "+retType+".");
            isFine = false;            
            //System.exit(1);
        }
        return null;
    }

    /**
    * f0 -> Identifier()
    * f1 -> "["
    * f2 -> Expression()
    * f3 -> "]"
    * f4 -> "="
    * f5 -> Expression()
    * f6 -> ";"
    */
    public String visit(ArrayAssignmentStatement n, Method argu) {
        String name = n.f0.accept(this, argu);
        Variable var = mth.getVar(name);
        if (var == null)
            var = cl.getVar(name);
        if (var == null){
            System.out.println("Variable "+name+" has not been declared.");
            isFine = false;            
            //System.exit(1);
        }
        if (var.type != "int[]"){
            System.out.println("Variable "+name+" is not an array.");
            isFine = false;            
            //System.exit(1);
        }

        String retType = n.f2.accept(this, argu);
        if( retType != "int"){
            System.out.println("Expression in between brackets should be an int, not "+retType+".");
            isFine = false;            
            //System.exit(1);
        }

        retType = n.f5.accept(this, argu);
        if( retType != "int"){
            System.out.println("Array "+name+" is of type int, not "+retType+".");
            isFine = false;            
            //System.exit(1);
        }
        return null;
    }

    /**
    * f0 -> "if"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    * f5 -> "else"
    * f6 -> Statement()
    */
    public String visit(IfStatement n, Method argu) {
        String retType = n.f2.accept(this, argu);
        if( retType != "boolean"){
            System.out.println("Expression in if statement should be boolean, not "+retType+".");
            isFine = false;            
            //System.exit(1);
        }

        n.f4.accept(this, argu);
        n.f6.accept(this, argu);
        return null;
    }

    /**
    * f0 -> "while"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    */
    public String visit(WhileStatement n, Method argu) {
        String retType = n.f2.accept(this, argu);
        if( retType != "boolean"){
            System.out.println("Expression in while statement should be boolean, not "+retType+".");
            isFine = false;            
            //System.exit(1);
        }
        n.f4.accept(this, argu);
        return null;
    }

    /**
    * f0 -> "System.out.println"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> ";"
    */
    public String visit(PrintStatement n, Method argu) {
        String retType = n.f2.accept(this, argu);
        if( retType != "boolean" && retType != "int"){
            System.out.println("Println expects int or boolean, not "+retType+".");
            isFine = false;            
            //System.exit(1);
        }
        return null;
    }

    /**
    * f0 -> Clause()
    * f1 -> "&&"
    * f2 -> Clause()
    */
    public String visit(AndExpression n, Method argu) {
        n.f0.accept(this, argu);
        n.f2.accept(this, argu);
        return "boolean";
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "<"
    * f2 -> PrimaryExpression()
    */
    public String visit(CompareExpression n, Method argu) {
        String retType = n.f0.accept(this, argu);
        if( retType != "int"){
            System.out.println("Left expression should be of type int, not "+retType+".");
            isFine = false;            
            //System.exit(1);
        }
        retType = n.f2.accept(this, argu);
        if(retType != "int"){
            System.out.println("Right expression should be of type int, not "+retType+".");
            isFine = false;            
            //System.exit(1);
        }
        return "boolean";
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "+"
    * f2 -> PrimaryExpression()
    */
    public String visit(PlusExpression n, Method argu) {
        String retType = n.f0.accept(this, argu);
        if(retType != "int"){
            System.out.println("Left expression should be of type int, not "+retType+".");
            isFine = false;            
            //System.exit(1);
        }

        retType = n.f2.accept(this, argu);
        if(retType != "int"){
            System.out.println("Right expression should be of type int, not "+retType+".");
            isFine = false;            
            //System.exit(1);
        }
        return "int";
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "-"
    * f2 -> PrimaryExpression()
    */
    public String visit(MinusExpression n, Method argu) {
        String retType = n.f0.accept(this, argu);
        if(retType != "int"){
            System.out.println("Left expression should be of type int, not "+retType+".");
            isFine = false;            
            //System.exit(1);
        }
        
        retType = n.f2.accept(this, argu);
        if(retType != "int"){
            System.out.println("Right expression should be of type int, not "+retType+".");
            isFine = false;            
            //System.exit(1);
        }
        return "int";
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "*"
    * f2 -> PrimaryExpression()
    */
    public String visit(TimesExpression n, Method argu) {
        String retType = n.f0.accept(this, argu);
        if(retType != "int"){
            System.out.println("Left expression should be of type int, not "+retType+".");
            isFine = false;            
            //System.exit(1);
        }
        
        retType = n.f2.accept(this, argu);
        if(retType != "int"){
            System.out.println("Right expression should be of type int, not "+retType+".");
            isFine = false;            
            //System.exit(1);
        }
        return "int";
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "["
    * f2 -> PrimaryExpression()
    * f3 -> "]"
    */
    public String visit(ArrayLookup n, Method argu) {
        String retType = n.f0.accept(this, argu);
        if(!retType.equals("int[]")){
            System.out.println("Expression should be of type int[], not "+retType+".");
            isFine = false;            
            //System.exit(1);
        }
        
        retType = n.f2.accept(this, argu);
        if(!retType.equals("int")){
            System.out.println("Expression should be of type int, not "+retType+".");
            isFine = false;            
            //System.exit(1);
        }
        return "int";
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> "length"
    */
    public String visit(ArrayLength n, Method argu) {
        String retType = n.f0.accept(this, argu);
        if(!retType.equals("int[]")){
            System.out.println("Expression should be of type int[], not "+retType+".");
            isFine = false;            
            //System.exit(1);
        }
        return "int";
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> Identifier()
    * f3 -> "("
    * f4 -> ( ExpressionList() )?
    * f5 -> ")"
    */
    public String visit(MessageSend n, Method argu) {
        String retType = n.f0.accept(this, argu);
        if (retType == "int" && retType == "int[]" && retType == "boolean"){
            System.out.println("Expression should be a class, not "+retType+".");
            isFine = false;            
            //System.exit(1);
        }

        ClassContents cl = symtable.getClass(retType);
        retType = n.f2.accept(this, argu);
        Method mth = cl.getMethod(retType);
        if (mth == null){
            System.out.println("Method "+retType+" was not declared in class "+cl.name+".");
            isFine = false;            
            //System.exit(1);
        }

        Method meth = new Method(mth.name, mth.type);
        n.f4.accept(this, meth);

        if (mth.parameters.size() != meth.parameters.size()){
            System.out.println("The number of parameters is not correct.");
            isFine = false;            
            //System.exit(1);
        }

        for (int i = 0; i < mth.parameters.size(); i++) {
            String typeA = mth.parameters.get(i);
            String typeB = meth.parameters.get(i);
            if (!typeChecks(typeB, typeA)){
                System.out.println("Parameters are not of the same type in method "+mth.name+".");
            isFine = false;                
                //System.exit(1);
            }
        }        

        return mth.type;
    }

    /**
    * f0 -> Expression()
    * f1 -> ExpressionTail()
    */
    public String visit(ExpressionList n, Method argu) {
        String retType = n.f0.accept(this, argu);
        argu.putParam(retType);
        n.f1.accept(this, argu);
        return null;
    }

    /**
    * f0 -> ","
    * f1 -> Expression()
    */
    public String visit(ExpressionTerm n, Method argu) {
        String retType = n.f1.accept(this, argu);
        argu.putParam(retType);
        return null;
    }

    /**
    * f0 -> IntegerLiteral()
    *       | TrueLiteral()
    *       | FalseLiteral()
    *       | Identifier()
    *       | ThisExpression()
    *       | ArrayAllocationExpression()
    *       | AllocationExpression()
    *       | BracketExpression()
    */
    public String visit(PrimaryExpression n, Method argu) {
        String retType = n.f0.accept(this, argu);

        if (n.f0.which == 3) {
            Variable var = mth.getVar(retType);
            if (var == null)
                var = cl.getVar(retType);
            if (var == null){
                System.out.println("Variable "+retType+" has not been declared.");
            isFine = false;                
                //System.exit(1);
            }
            retType = var.type;
        }

        return retType;
    }

    /**
    * f0 -> <INTEGER_LITERAL>
    */
    public String visit(IntegerLiteral n, Method argu) {
        return "int";
    }

    /**
    * f0 -> "true"
    */
    public String visit(TrueLiteral n, Method argu) {
        return "boolean";
    }

    /**
    * f0 -> "false"
    */
    public String visit(FalseLiteral n, Method argu) {
        return "boolean";
    }

    /**
    * f0 -> <IDENTIFIER>
    */
    public String visit(Identifier n, Method argu) {
        return n.f0.toString();
    }

    /**
    * f0 -> "this"
    */
    public String visit(ThisExpression n, Method argu) {
        return cl.name;
    }

    /**
    * f0 -> "new"
    * f1 -> "int"
    * f2 -> "["
    * f3 -> Expression()
    * f4 -> "]"
    */
    public String visit(ArrayAllocationExpression n, Method argu) {
        String retType = n.f3.accept(this, argu);
        if(!retType.equals("int")){
            System.out.println("Expression should be of type int, not "+retType+".");
            isFine = false;            
            //System.exit(1);
        }
        return "int[]";
    }

    /**
    * f0 -> "new"
    * f1 -> Identifier()
    * f2 -> "("
    * f3 -> ")"
    */
    public String visit(AllocationExpression n, Method argu) {
        String retType = n.f1.accept(this, argu);
        ClassContents cl = symtable.getClass(retType);
        if(cl == null){
            System.out.println("Class "+retType+" has not been declared.");
            isFine = false;            
            //System.exit(1);
        }
        return retType;
    }

    /**
    * f0 -> "!"
    * f1 -> Clause()
    */
    public String visit(NotExpression n, Method argu) {
        return n.f1.accept(this, argu);
    }

    /**
    * f0 -> "("
    * f1 -> Expression()
    * f2 -> ")"
    */
    public String visit(BracketExpression n, Method argu) {
        return n.f1.accept(this, argu);
    }
}