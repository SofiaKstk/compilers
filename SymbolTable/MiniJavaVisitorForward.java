package SymbolTable;

import syntaxtree.*;
import visitor.GJDepthFirst;


public class MiniJavaVisitorForward extends GJDepthFirst<String, Struct>{

    // Checking for correct forward declaration
    SymbolTable symtable;
    boolean isFine;

    public MiniJavaVisitorForward(SymbolTable symtable) {
        this.symtable = symtable;
        isFine = true;
    }

    /**
    * f0 -> MainClass()
    * f1 -> ( TypeDeclaration() )*
    * f2 -> <EOF>
    */
    public String visit(Goal n, Struct argu) {
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
    public String visit(MainClass n, Struct argu) {
       
        String name = n.f1.accept(this, null);
        ClassContents cl = symtable.getClass(name);
        Method mth = cl.getMethod("main");

        n.f14.accept(this, mth);
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
    public String visit(ClassDeclaration n, Struct argu) {
        String name = n.f1.accept(this, argu);
        ClassContents cl = symtable.getClass(name);
        
        n.f3.accept(this, cl);
        n.f4.accept(this, cl);
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
    public String visit(ClassExtendsDeclaration n, Struct argu) {
        
        String name = n.f1.accept(this, argu);
        ClassContents cl = symtable.getClass(name);
        
        n.f5.accept(this, cl);
        n.f6.accept(this, cl);
        return null;
    }

    /**
    * f0 -> Type()
    * f1 -> Identifier()
    * f2 -> ";"
    */
    public String visit(VarDeclaration n, Struct argu) {
        n.f0.accept(this, null);
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
    public String visit(MethodDeclaration n, Struct argu) {
        n.f1.accept(this, null);
        String name = n.f2.accept(this, null);
        ClassContents cl = (ClassContents) argu; 
        Method mth = cl.getMethod(name);
        n.f4.accept(this, mth);
        n.f7.accept(this, mth);    
        return null;
    }

    /**
    * f0 -> Type()
    * f1 -> Identifier()
    */
    public String visit(FormalParameter n, Struct argu) {
        n.f0.accept(this, null);
        return null;
    }

    /**
    * f0 -> ArrayType()
    *       | BooleanType()
    *       | IntegerType()
    *       | Identifier()
    */
    public String visit(Type n, Struct argu) {
        String type = n.f0.accept(this, null);
        if (type != "int" && type != "int[]" && type != "boolean"){
            if (symtable.getClass(type) == null){
                System.out.println("Type "+type+" has not been declared.");
                isFine = false;
                //System.exit(1);
            }
        }            
        return type;
    }

   /**
    * f0 -> "int"
    * f1 -> "["
    * f2 -> "]"
    */
    public String visit(ArrayType n, Struct argu) {
        return "int[]";
    }

    /**
    * f0 -> "boolean"
    */
    public String visit(BooleanType n, Struct argu) {
        return "boolean";
    }

    /**
    * f0 -> "int"
    */
    public String visit(IntegerType n, Struct argu) {
        return "int";
    }

    /**
    * f0 -> <IDENTIFIER>
    */
    public String visit(Identifier n, Struct argu) {
        return n.f0.toString();
    }

}