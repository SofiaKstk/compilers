package SymbolTable;

import syntaxtree.*;
import visitor.GJDepthFirst;

public class MiniJavaVisitorSymbol extends GJDepthFirst<String, Struct>{

    // Filling up SymbolTable
    SymbolTable symtable;
    int stack; 
    boolean isFine; 

    public MiniJavaVisitorSymbol(SymbolTable symtable) {
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
        ClassContents cl = new ClassContents(name, null);
        symtable.classes.put(name, cl);

        Method mth = new Method("main", "void");

        name = n.f11.accept(this, null);
        Variable var = new Variable(name, "String[]");

        mth.putVar(var);
        mth.putParam("String[]");

        n.f14.accept(this, mth);
        cl.putMeth(mth);
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
        if(symtable.getClass(name) != null){
            System.out.println(name+" has been declared previously.");
            isFine = false;
            //System.exit(1);
        }
        ClassContents cl = new ClassContents(name,null);
        System.out.println("-----------Class "+ name +"-----------");
        symtable.classes.put(name, cl);
        System.out.println("---Variables---");
        this.stack = 0;
        n.f3.accept(this, cl);
        cl.varStackEnd = this.stack;
        System.out.println("---Methods---");
        this.stack = 0;
        n.f4.accept(this, cl);
        cl.methStackEnd = this.stack;
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
        if(symtable.getClass(name) != null){
            System.out.println(name+" has been declared previously.");
            isFine = false;
            //System.exit(1);
        }
        String par = n.f3.accept(this, argu);
        ClassContents parent = symtable.getClass(par);
        if(parent == null){
            System.out.println("Class "+par+" has not been declared previously.");
        }
        ClassContents cl = new ClassContents(name, parent);
        symtable.classes.put(name, cl);
        System.out.println("-----------Class "+ name +"-----------");
        System.out.println("---Variables---");
        cl.varStack = this.stack = parent.varStackEnd;
        String st = n.f5.accept(this, cl);
        cl.varStackEnd = this.stack;
        System.out.println("---Methods---");
        cl.methStack = this.stack = parent.methStackEnd;
        n.f6.accept(this, cl);
        cl.methStackEnd = this.stack;
        return null;
    }

    /**
    * f0 -> Type()
    * f1 -> Identifier()
    * f2 -> ";"
    */
    public String visit(VarDeclaration n, Struct argu) {
        int num = this.stack;
        String type = n.f0.accept(this, null);
        String name = n.f1.accept(this, null);
        Variable var = new Variable(name, type);
        List l = (List) argu;
        if(!l.putVar(var)){
            System.out.println(name +" has been declared previously.");
            isFine = false;
            //System.exit(1);
        }
        if (l instanceof ClassContents){
            if (type == "int")
                this.stack += 4;
            else if (type == "boolean")
                this.stack += 1;
            else
                this.stack += 8;
            System.out.println(l.name+"."+name+" : "+num);
        }        
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
        String type = n.f1.accept(this, null);
        String name = n.f2.accept(this, null);
        Method mth = new Method(name, type);
        n.f4.accept(this, mth);
        ClassContents cl = (ClassContents) argu; 
        if (!cl.putMeth(mth)){
            System.out.println("A method called " + mth.name + " already exists in class "+ cl.name +".");
            isFine = false;
            //System.exit(1);
        }
        n.f7.accept(this, mth);
        if(cl.parent != null && cl.parent.getMethod(name) == null){
            int num = this.stack;
            this.stack += 8;
            System.out.println(cl.name+"."+name+" : "+num);
        }
        else if (cl.parent == null){
            int num = this.stack;
            this.stack += 8;
            System.out.println(cl.name+"."+name+" : "+num);
        }           
        return null;
    }

    /**
    * f0 -> Type()
    * f1 -> Identifier()
    */
    public String visit(FormalParameter n, Struct argu) {
        String type = n.f0.accept(this, null);
        String name = n.f1.accept(this, null);
        Variable var = new Variable(name, type);
        Method mth = (Method) argu;
        if (!mth.putVar(var)){
            System.out.println("A parameter with name: "+ name +" already exists.");
            isFine = false;
            //System.exit(1);
        }
        mth.putParam(type);
        return null;
    }

    /**
    * f0 -> ArrayType()
    *       | BooleanType()
    *       | IntegerType()
    *       | Identifier()
    */
    public String visit(Type n, Struct argu) {
        return n.f0.accept(this, null);
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