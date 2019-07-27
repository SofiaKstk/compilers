package SymbolTable;

import syntaxtree.*;
import java.io.FileOutputStream;
import java.util.LinkedHashMap;
import visitor.GJNoArguDepthFirst;

public class MiniJavaVisitorLL extends GJNoArguDepthFirst<String>{

    // Producing llvm code
    SymbolTable symtable;
    FileOutputStream fos;
    ClassContents cl;
    String is_bool;
    String st;
    String ident;
    String prm;
    String arr;
    int index;
    Method mth;
    int oob_c;
    int if_c;
    int loop_c;
    int arr_alloc_c;
    int andclause_c;

    public String emit(String str){
        try { 
            this.fos.write(str.getBytes());
        }
        catch (Exception e) {
            System.out.println("Error" + e);
        }

        return null;
    }

    public String getType(String str){
        if (str == "boolean")
            return "i1";
        else if (str == "int")
            return "i32";
        else if (str == "int[]")
            return "i32*";
        else
            return "i8*";
    }

    //get all methods from parents and current into one map
    public LinkedHashMap<String, Method> getAllMethods(ClassContents cl){

        LinkedHashMap<String, Method> all = new LinkedHashMap<>();
        for (String m : cl.methods.keySet()){
            Method mth = cl.methods.get(m);
            all.put(cl.name+"."+mth.name, mth);
        }
        ClassContents par = cl.parent;
        while (par != null){
            for (String m : par.methods.keySet()){
                Method mth = par.methods.get(m);
                
                boolean flag = false;
                for (String m2 : all.keySet()){
                    Method mth2 = all.get(m2);
                    if(m.equals(mth2.name)){
                        flag = true;
                        break;
                    }
                }
                if(!flag)
                    all.put(par.name+"."+mth.name, mth);
            }
            par = par.parent;
        }
        return all;
    }

    public int getOffset(String var){
        ClassContents curr = this.cl;
        if (curr.parent != null){
            ClassContents par = curr.parent;
            // ClassContents kid = curr
            while (par != null && par.getVar(var) != null){
                curr = par;
                par = par.parent;
            }
        }

        int origOff = curr.varStack +8;
        for (String v : curr.variables.keySet()){
            if (!var.equals(v)){
                Variable vr = curr.getVar(v);
                int off;
                if(vr.type == "int")
                    off = 4;
                else if( vr.type == "boolean")
                    off = 1;
                else
                    off = 8;
                origOff += off;
            }
            else
                break;
        }
        return origOff;
    }

    public int getMOffset(String cl, String meth){
        ClassContents c = this.symtable.getClass(cl);
        LinkedHashMap<String, Method> all = getAllMethods(c);
        int place = 0;
        for(String m : all.keySet()){
            String part = m.substring(m.indexOf(".")+1);
            Method mth = c.getMethod(part);
            if(mth.name.equals(meth))
                break;
            place +=1;
        }
        return place;
    }


    public MiniJavaVisitorLL(SymbolTable symtable, FileOutputStream fos) {
        this.is_bool = "no";
        this.st = "";
        this.ident = "";
        this.arr = "";
        this.prm = null;
        this.index = -1;
        this.oob_c = -1;
        this.if_c = -1;
        this.loop_c = -1;
        this.arr_alloc_c = -1;
        this.andclause_c = -1;
        this.symtable = symtable;
        this.fos = fos;

        //remove main from Methods
        Object main = this.symtable.classes.keySet().iterator().next();
        ClassContents mn = this.symtable.classes.get(main);
        mn.methods.remove("main");


        for (String name : this.symtable.classes.keySet()){
            ClassContents cl = this.symtable.getClass(name);

            LinkedHashMap<String, Method> meths = getAllMethods(cl);

            emit("@."+cl.name+"_vtable = global ["+meths.size()+" x i8*] [");
            
            boolean flag = false;
            for (String m : meths.keySet()){
                Method meth = meths.get(m);
                if(flag)
                    emit(", ");
                flag = true;
                emit("i8* bitcast (");
                
                //return type
                emit(getType(meth.type)+" ");

                //parameters
                emit("(i8*");
                for (String p : meth.parameters)
                    emit(", "+getType(p));  

                emit(")* @"+m+" to i8*)"); 
            }
            emit("]\n");
        }

        emit("\ndeclare i8* @calloc(i32, i32)\ndeclare i32 @printf(i8*, ...)\n"
        +"declare void @exit(i32)\n\n@_cint = constant [4 x i8] c\"%d\\0a\\00\"\n"
        +"@_cOOB = constant [15 x i8] c\"Out of bounds\\0a\\00\"\ndefine void @print_int(i32 %i) {\n\t"
        +"%_str = bitcast [4 x i8]* @_cint to i8*\n\tcall i32 (i8*, ...) @printf(i8* %_str, i32 %i)\n\tret void\n}"
        +"\n\ndefine void @throw_oob() {\n\t%_str = bitcast [15 x i8]* @_cOOB to i8*\n\t"
        +"call i32 (i8*, ...) @printf(i8* %_str)\n\n\tcall void @exit(i32 1)\n\tret void\n}\n");
    }

    /**
    * f1 -> Identifier()
    * f14 -> ( VarDeclaration() )*
    * f15 -> ( Statement() )*
    */
    public String visit(MainClass n) {
        emit("define i32 @main() {\n");
        String name = n.f1.accept(this);
        this.cl = symtable.getClass(name);
        Method main = new Method("main", "void");
        this.mth = main;

        n.f14.accept(this);
        n.f15.accept(this);

        emit("\n\tret i32 0\n}\n\n");
        return null;
    }

    /**
    * f4 -> ( MethodDeclaration() )*
    */
    public String visit(ClassDeclaration n) {
        String name = n.f1.accept(this);
        this.cl = symtable.getClass(name);

        n.f4.accept(this);
        return null;
    }

    /**
    * f6 -> ( MethodDeclaration() )*
    */
    public String visit(ClassExtendsDeclaration n) {
        String name = n.f1.accept(this);
        this.cl = symtable.getClass(name);

        n.f6.accept(this);
        return null;
    }

    /**
    * f1 -> Type()
    * f2 -> Identifier()
    * f4 -> ( FormalParameterList() )?
    * f7 -> ( VarDeclaration() )*
    * f8 -> ( Statement() )*
    * f10 -> Expression()
    */
    public String visit(MethodDeclaration n) {
        this.index = -1;
        String type = n.f1.accept(this);
        String name = n.f2.accept(this);
        this.mth = cl.getMethod(name);
        emit("define "+getType(type)+" @"+this.cl.name+"."+name+"(i8* %this");
        n.f4.accept(this);
        emit(") {\n");

        emit(this.st);
        emit("\n");
        this.st = "";
        n.f7.accept(this);
        emit("\n");
        n.f8.accept(this);
        emit("\n");
        String reg = n.f10.accept(this);
        if (!reg.startsWith("%_") && reg.startsWith("%") && reg != "%this"){
            this.index += 1;
            emit("\n\t%_"+this.index+" = load "+getType(type)+", "+getType(type)+"* "+reg);
            reg = "%_"+this.index;                
        }
        emit("\n\tret "+getType(type)+" "+reg);
        emit("\n}\n\n");
        this.index = 0;
        return null;
    }

    /**
    * f0 -> Type()
    * f1 -> Identifier()
    */
    public String visit(FormalParameter n) {
        String type = n.f0.accept(this);
        String name = n.f1.accept(this);
        String t = getType(type);
        emit(", "+t+" %."+name);
        this.st  = this.st + "\n\t%"+name+" = alloca "+t+"\n\tstore "+t+" %."+name+", "+t+"* %"+name;
        return null;
    }

    /**
    * f0 -> Type()
    * f1 -> Identifier()
    * f2 -> ";"
    */
    public String visit(VarDeclaration n) {
        String type = n.f0.accept(this);
        String name = n.f1.accept(this);
        if(this.mth.name == "main"){
            Variable v = new Variable(name, type);
            this.mth.putVar(v);
        }
        emit("\n\t%"+name+" = alloca "+getType(type));
        return null;
    }

    /**
    * f0 -> Identifier()
    * f1 -> "="
    * f2 -> Expression()
    */
    public String visit(AssignmentStatement n) {
        String reg = n.f2.accept(this);
        if (!reg.startsWith("%_") && reg.startsWith("%") && reg != "%this"){
            this.index += 1;
            String name = reg.substring(1);
            Variable v = this.mth.getVar(name);
            // maybe getVar from class and getelementptr.....
            emit("\n\t%_"+this.index+" = load "+getType(v.type)+", "+getType(v.type)+"* "+reg);
            reg = "%_"+this.index;                
        }
        String name = n.f0.accept(this);
        Variable var = this.mth.getVar(name);
        if (var == null){
            var = this.cl.getVar(name);
            int off = getOffset(name);
            String t = getType(var.type);
            this.index += 1;
            emit("\n\t%_"+this.index+" = getelementptr i8, i8* %this, i32 "+off);
            this.index += 1;        
            emit("\n\t%_"+this.index+" = bitcast i8* %_"+(this.index -1)+" to "+t+"*");
            name = "%_"+this.index;
        }
        else{
            name = "%"+name;
        }
        String t = getType(var.type);
        emit("\n\tstore "+t+" "+reg+", "+t+"* "+name+"\n");
        return null;
    }

    /**
    * f0 -> Identifier()
    * f1 -> "["
    * f2 -> Expression()
    * f3 -> "]"
    * f4 -> "="
    * f5 -> Expression()
    */
    public String visit(ArrayAssignmentStatement n) {
        String name = n.f0.accept(this);
        Variable var = this.mth.getVar(name);
        if (var == null){
            var = this.cl.getVar(name);
            int off = getOffset(name);
            this.index += 1;
            emit("\n\t%_"+this.index+" = getelementptr i8, i8* %this, i32 "+off);
            this.index += 1;
            emit("\n\t%_"+this.index+" = bitcast i8* %_"+(this.index -1)+" to i32**");
            this.index += 1;
            emit("\n\t%_"+this.index+" = load i32*, i32** %_"+(this.index-1));                
        }
        else{
            this.index += 1;
            emit("\n\t%_"+this.index+" = load i32*, i32** %"+name);
        }
        
        int elem = this.index;

        String ind = n.f2.accept(this);        
        this.index+=1;
        String ret = "%_"+this.index;
        emit("\n\t%_"+this.index+" = load i32, i32* %_"+(this.index-1));
        if (!ind.startsWith("%_") && ind.startsWith("%")){
            this.index+=1;
            emit("\n\t%_"+this.index+" = load i32, i32* "+ind);
            ind = "%_"+this.index;
        }
        this.index+=1;
        emit("\n\t%_"+this.index+" = icmp ult i32 "+ind+", "+ret);
        this.oob_c+=3;
        String label0 = "oob"+(this.oob_c-2);
        String label1 = "oob"+(this.oob_c-1);
        String label2 = "oob"+(this.oob_c);
        emit("\n\tbr i1 %_"+this.index+", label %"+label0+", label %"+label1);

        emit("\n\n"+label0+":");
        this.index+=1;
        emit("\n\t%_"+this.index+" = add i32 "+ind+", 1");
        this.index+=1;
        String arr = "%_"+this.index;
        emit("\n\t%_"+this.index+" = getelementptr i32, i32* %_"+elem+", i32 %_"+(this.index-1)+"\n");
        String ass = n.f5.accept(this);
        if(ass.startsWith("%") && !ass.startsWith("%_")){
            this.index+=1;
            emit("\n\t%_"+this.index+" = load i32, i32* "+ass);
            ass = "%_"+this.index;
        }

        emit("\n\tstore i32 "+ass+", i32* "+arr);
        emit("\n\tbr label %"+label2);

        emit("\n\n"+label1+":\n\tcall void @throw_oob()\n\tbr label %"+label2);
        emit("\n\n"+label2+":");
        return null;
    }

    /**
    * f2 -> Expression()
    * f4 -> Statement()
    * f6 -> Statement()
    */
    public String visit(IfStatement n) {
        String exp = n.f2.accept(this);
        if (!exp.startsWith("%_") && exp.startsWith("%")){
            this.index += 1;
            emit("\n\t%_"+this.index+" = load i1, i1* "+exp);
            exp = "%_"+this.index;                
        }
        this.if_c+=3;
        String label0 = "if"+(this.if_c-2);
        String label1 = "if"+(this.if_c-1);
        String label2 = "if"+(this.if_c);
        emit("\n\tbr i1 "+exp+", label %"+label0+", label %"+label1);
        emit("\n\n"+label0+":");
        n.f4.accept(this);
        emit("\n\tbr label %"+label2);

        emit("\n\n"+label1+":");
        n.f6.accept(this);
        emit("\n\tbr label %"+label2);

        emit("\n\n"+label2+":");
        return null;
    }

    /**
    * f2 -> Expression()
    * f4 -> Statement()
    */
    public String visit(WhileStatement n) {
        this.loop_c+=3;
        String label0 = "loop"+(this.loop_c-2);
        String label1 = "loop"+(this.loop_c-1);
        String label2 = "loop"+(this.loop_c);
        emit("\n\tbr label %"+label0);

        emit("\n\n"+label0+":");
        String expr = n.f2.accept(this);
        if (!expr.startsWith("%_") && expr.startsWith("%")){
            this.index += 1;
            emit("\n\t%_"+this.index+" = load i1, i1* "+expr);
            expr = "%_"+this.index;                
        }
        emit("\n\tbr i1 "+expr+", label %"+label1+", label %"+label2);

        emit("\n\n"+label1+":");
        n.f4.accept(this);
        emit("\n\tbr label %"+label0);

        emit("\n\n"+label2+":");
        return null;
    }

    /**
    * f2 -> Expression()
    */
    public String visit(PrintStatement n) {
        String reg = n.f2.accept(this);
        if(this.is_bool == "yes"){
            this.index += 1;
            emit("\n\t%_"+this.index+" = zext i1 "+reg+" to i32");
        }
        else{
            if (!reg.startsWith("%_") && reg.startsWith("%")){
                this.index += 1;
                emit("\n\t%_"+this.index+" = load i32, i32* "+reg);
                reg = "%_"+this.index;                
            }
        }
        


        emit("\n\tcall void (i32) @print_int(i32 "+reg+")");
        this.is_bool = "no";
        return null;
    }

    /**
    * f0 -> Clause()
    * f2 -> Clause()
    */
    public String visit(AndExpression n) {
        String c1 = n.f0.accept(this);
        if (!c1.startsWith("%_") && c1.startsWith("%")){
            this.index += 1;
            emit("\n\t%_"+this.index+" = load i1, i1* "+c1);
            c1 = "%_"+this.index;                
        }
        this.andclause_c+=4;
        String label0 = "andclause"+(this.andclause_c-3);
        String label1 = "andclause"+(this.andclause_c-2);
        String label2 = "andclause"+(this.andclause_c-1);
        String label3 = "andclause"+(this.andclause_c);        
        emit("\n\tbr label %"+label0);

        emit("\n"+label0+":");
        emit("\n\tbr i1 "+c1+", label %"+label1+", label %"+label3);

        emit("\n"+label1+":");
        String c2 = n.f2.accept(this);
        if (!c2.startsWith("%_") && c2.startsWith("%")){
            this.index += 1;
            emit("\n\t%_"+this.index+" = load i1, i1* "+c2);
            c2 = "%_"+this.index;                
        }
        emit("\n\tbr label %"+label2);
        
        emit("\n"+label2+":");
        emit("\n\tbr label %"+label3);
        
        emit("\n"+label3+":");
        this.index += 1;
        emit("\n\t%_"+this.index+" = phi i1 [ 0, %"+label0+" ], [ "+c2+", %"+label2+" ]");

        this.is_bool = "yes";
        return "%_"+this.index;
    }
    
    /**
    * f0 -> PrimaryExpression()
    * f1 -> "<"
    * f2 -> PrimaryExpression()
    */
    public String visit(CompareExpression n) {
        String reg = n.f0.accept(this);
        if(!reg.startsWith("%_") && reg.startsWith("%")){
            this.index += 1;        
            emit("\n\t%_"+this.index+" = load i32, i32* "+reg);
            reg = "%_"+this.index;
        }
        String reg2 = n.f2.accept(this);
        if(!reg2.startsWith("%_") && reg2.startsWith("%")){
            this.index += 1;        
            emit("\n\t%_"+this.index+" = load i32, i32* "+reg2);
            reg2 = "%_"+this.index;
        }
        this.index += 1;
        emit("\n\t%_"+this.index+" = icmp slt i32 "+reg+", "+reg2);
        this.is_bool = "yes";
        return "%_"+this.index;
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "+"
    * f2 -> PrimaryExpression()
    */
    public String visit(PlusExpression n) {
        String reg = n.f0.accept(this);
        if(!reg.startsWith("%_") && reg.startsWith("%")){
            this.index += 1;        
            emit("\n\t%_"+this.index+" = load i32, i32* "+reg);
            reg = "%_"+this.index;
        }
        String reg2 = n.f2.accept(this);
        if(!reg2.startsWith("%_") && reg2.startsWith("%")){
            this.index += 1;        
            emit("\n\t%_"+this.index+" = load i32, i32* "+reg2);
            reg2 = "%_"+this.index;
        }
        this.index += 1;
        emit("\n\t%_"+this.index+" = add i32 "+reg+", "+reg2);
        return "%_"+this.index;
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "-"
    * f2 -> PrimaryExpression()
    */
    public String visit(MinusExpression n) {
        String reg = n.f0.accept(this);
        if(!reg.startsWith("%_") && reg.startsWith("%")){
            this.index += 1;        
            emit("\n\t%_"+this.index+" = load i32, i32* "+reg);
            reg = "%_"+this.index;
        }
        String reg2 = n.f2.accept(this);
        if(!reg2.startsWith("%_") && reg2.startsWith("%")){
            this.index += 1;        
            emit("\n\t%_"+this.index+" = load i32, i32* "+reg2);
            reg2 = "%_"+this.index;
        }
        this.index += 1;
        emit("\n\t%_"+this.index+" = sub i32 "+reg+", "+reg2);
        this.is_bool = "no";
        return "%_"+this.index;
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "*"
    * f2 -> PrimaryExpression()
    */
    public String visit(TimesExpression n) {
        String reg = n.f0.accept(this);
        if(!reg.startsWith("%_") && reg.startsWith("%")){
            this.index += 1;        
            emit("\n\t%_"+this.index+" = load i32, i32* "+reg);
            reg = "%_"+this.index;
        }
        String reg2 = n.f2.accept(this);
        if(!reg2.startsWith("%_") && reg2.startsWith("%")){
            this.index += 1;        
            emit("\n\t%_"+this.index+" = load i32, i32* "+reg2);
            reg2 = "%_"+this.index;
        }
        this.index += 1;
        emit("\n\t%_"+this.index+" = mul i32 "+reg+", "+reg2);
        return "%_"+this.index;
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "["
    * f2 -> PrimaryExpression()
    * f3 -> "]"
    */
    public String visit(ArrayLookup n) {
        String arr = n.f0.accept(this);
        String ind = n.f2.accept(this);

        if(!arr.startsWith("%_") && arr.startsWith("%")){
            this.index+=1;
            emit("\n\t%_"+this.index+" = load i32*, i32** "+arr);
            arr = "%_"+this.index;
        }
        this.index+=1;
        String ret = "%_"+this.index;        
        emit("\n\t%_"+this.index+" = load i32, i32* "+arr);

        if(!ind.startsWith("%_") && ind.startsWith("%")){
            this.index+=1;
            emit("\n\t%_"+this.index+" = load i32, i32* "+ind);
            ind = "%_"+this.index;
        }

        this.index+=1;
        emit("\n\t%_"+this.index+" = icmp ult i32 "+ind+", "+ret);
        this.oob_c+=3;
        String label0 = "oob"+(this.oob_c-2);
        String label1 = "oob"+(this.oob_c-1);
        String label2 = "oob"+(this.oob_c);
        emit("\n\tbr i1 %_"+this.index+", label %"+label0+", label %"+label1);

        emit("\n\n"+label0+":");
        this.index+=1;
        emit("\n\t%_"+this.index+" = add i32 %_"+(this.index-2)+", 1");
        this.index+=1;
        emit("\n\t%_"+this.index+" = getelementptr i32, i32* "+arr+", i32 %_"+(this.index-1));
        this.index+=1;
        emit("\n\t%_"+this.index+" = load i32, i32* %_"+(this.index-1));
        emit("\n\tbr label %"+label2);

        this.oob_c+=1;
        emit("\n\n"+label1+":\n\tcall void @throw_oob()\n\tbr label %"+label2); 

        emit("\n\n"+label2+":");
        this.is_bool = "no";
        return "%_"+this.index;
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> "length"
    */
    public String visit(ArrayLength n) {
        String reg = n.f0.accept(this);
        if(!reg.startsWith("%_") && reg.startsWith("%")){
            Variable v = this.mth.getVar(reg.substring(1));
            if(v == null){
                this.index += 1;
                emit("\n\t%_"+this.index+" = getelementptr i32, i32* "+reg+", i32 0");
            }
            else{
                this.index += 1;
                emit("\n\t%_"+this.index+" = load "+getType(v.type)+", "+getType(v.type)+"* "+reg);
            }
            reg = "%_"+this.index;
        }
        this.index += 1;
        emit("\n\t%_"+this.index+" = load i32, i32* "+reg);
        return "%_"+this.index;
    }

    /**
    * f0 -> PrimaryExpression()
    * f2 -> Identifier()
    * f4 -> ( ExpressionList() )?
    */
    public String visit(MessageSend n) {
        String reg = n.f0.accept(this);
        if (!reg.startsWith("%_") && reg.startsWith("%") && reg != "%this"){
            Variable v = this.mth.getVar(reg.substring(1));
            // maybe cl.getVar for classes variables, getelementptr...
            this.index += 1;
            emit("\n\t%_"+this.index+" = load "+getType(v.type)+", "+getType(v.type)+"* "+reg);
            reg = "%_"+this.index;
        }

        String clas = this.ident;
        if (this.symtable.getClass(clas) == null){
            Variable var = this.mth.getVar(clas);
            if (var == null)
                var = this.cl.getVar(clas);
            clas =  var.type;
        }
        String id = n.f2.accept(this);
        this.prm = null;
        n.f4.accept(this);
        String expr = this.prm;

        int off = getMOffset(clas, id);
        emit("\n\t; "+clas+"."+id+" : "+off);
        this.index += 1;
        emit("\n\t%_"+this.index+" = bitcast i8* "+reg+" to i8***");
        this.index += 1;
        emit("\n\t%_"+this.index+" = load i8**, i8*** %_"+(this.index-1));
        this.index += 1;
        emit("\n\t%_"+this.index+" = getelementptr i8*, i8** %_"+(this.index-1)+", i32 "+off);
        this.index += 1;
        emit("\n\t%_"+this.index+" = load i8*, i8** %_"+(this.index-1));
        this.index += 1;
        String bit = "%_"+this.index;
        emit("\n\t%_"+this.index+" = bitcast i8* %_"+(this.index-1)+" to ");
        //return type
        ClassContents cl = this.symtable.getClass(clas);
        Method meth = cl.getMethod(id);
        emit(getType(meth.type)+" ");

        //parameters
        emit("(i8*");
        for (String p : meth.parameters)
            emit(", "+getType(p));
        
        emit(")*");

        //load and get types
        String params[] = null;
        if(expr != null){
            params = expr.split(" ");
            int i = 0;
            for (String p : meth.parameters){
                String type = getType(p);
                if (!params[i].startsWith("%_") && params[i].startsWith("%") && params[i] != "%this"){
                    this.index += 1;
                    emit("\n\t%_"+this.index+" = load "+type+", "+type+"* "+params[i]);
                    params[i] = "%_"+this.index;
                }
                params[i] = type + " "+ params[i];
                i+=1;
            }
        }

        this.index += 1;       
        emit("\n\t%_"+this.index+" = call "+getType(meth.type)+" "+bit+"(i8* "+reg);
        if (expr != null){
            for (String p : params)
                emit(", "+p);
        }
        emit(")");
        if(meth.type == "boolean")
            this.is_bool = "yes";
        else
            this.is_bool = "no";
        return "%_"+this.index;
    }

    /**
    * f0 -> "this"
    */
    public String visit(ThisExpression n) {
        this.ident = this.cl.name;
        return "%this";
    }

    /**
    * f0 -> "new"
    * f1 -> "int"
    * f2 -> "["
    * f3 -> Expression()
    * f4 -> "]"
    */
    public String visit(ArrayAllocationExpression n) {
        String reg = n.f3.accept(this);
        if (!reg.startsWith("%_") && reg.startsWith("%")){
            this.index += 1;
            emit("\n\t%_"+this.index+" = load i32, i32* "+reg);
            reg = "%_"+this.index;
        }

        this.index+=1;
        emit("\n\t%_"+this.index+" = icmp slt i32 "+reg+", 0");
        this.arr_alloc_c+=2;
        String label0 = "arr_alloc"+(this.arr_alloc_c-1);
        String label1 = "arr_alloc"+(this.arr_alloc_c);

        emit("\n\tbr i1 %_"+this.index+", label %"+label0+", label %"+label1);
        emit("\n\n"+label0+":\n\tcall void @throw_oob()\n\tbr label %"+label1);
        emit("\n\n"+label1+":");
        this.index+=1;
        emit("\n\t%_"+this.index+" = add i32 "+reg+", 1");
        this.index+=1;
        emit("\n\t%_"+this.index+" = call i8* @calloc(i32 4, i32 %_"+(this.index-1)+")");
        this.index+=1;
        emit("\n\t%_"+this.index+" = bitcast i8* %_"+(this.index-1)+" to i32*");

        emit("\n\tstore i32 "+reg+", i32* %_"+this.index);
        return "%_"+this.index;
    }

    /**
    * f0 -> "new"
    * f1 -> Identifier()
    * f2 -> "("
    * f3 -> ")"
    */
    public String visit(AllocationExpression n) {
        String type = n.f1.accept(this);     
        this.ident = type;
        ClassContents c = this.symtable.getClass(type);
        this.index += 1;
        emit("\n\t%_"+this.index+" = call i8* @calloc(i32 1, i32 "+(c.varStackEnd+8)+")");
        this.index += 1;        
        emit("\n\t%_"+this.index+" = bitcast i8* %_"+(this.index-1)+" to i8***");

        LinkedHashMap<String, Method> meths = getAllMethods(c);
        this.index += 1;        
        emit("\n\t%_"+this.index+" = getelementptr [ "+meths.size()+" x i8*], [ "+meths.size()+" x i8*]* @."+c.name+"_vtable, i32 0, i32 0");        
        emit("\n\tstore i8** %_"+this.index+", i8*** %_"+(this.index-1));
        return "%_"+(this.index-2);
    }

    /**
    * f0 -> "!"
    * f1 -> Clause()
    */
    public String visit(NotExpression n) {
        String c = n.f1.accept(this);
        if (!c.startsWith("%_") && c.startsWith("%") && c != "%this"){
            this.index += 1;
            emit("\n\t%_"+this.index+" = load i1, i1* "+c);
            c = "%_"+this.index;
        }
        this.index += 1;
        emit("\n\t%_"+this.index+" = xor i1 1, "+c);
        return "%_"+this.index;
    }

    /**
    * f0 -> "("
    * f1 -> Expression()
    * f2 -> ")"
    */
    public String visit(BracketExpression n) {
        return n.f1.accept(this);
    }

    /**
    * f0 -> Expression()
    * f1 -> ExpressionTail()
    */
    public String visit(ExpressionList n) {
        String e = n.f0.accept(this);
        this.prm = e;
        n.f1.accept(this);
        return null;
    }

    /**
    * f0 -> ( ExpressionTerm() )*
    */
    public String visit(ExpressionTail n) {
        return n.f0.accept(this);
    }

    /**
    * f0 -> ","
    * f1 -> Expression()
    */
    public String visit(ExpressionTerm n) {
        this.prm = this.prm + " " + n.f1.accept(this);
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
    public String visit(PrimaryExpression n) {
        String r = n.f0.accept(this);
        if(n.f0.which == 3){
            this.ident = r;
            Variable var = this.mth.getVar(r);
            if (var == null){         
                var = this.cl.getVar(r);
                int off = getOffset(r);
                this.index += 1;
                emit("\n\t%_"+this.index+" = getelementptr i8, i8* %this, i32 "+off);
                this.index += 1;
                emit("\n\t%_"+this.index+" = bitcast i8* %_"+(this.index -1)+" to "+getType(var.type)+"*");
                this.index += 1;
                emit("\n\t%_"+this.index+" = load "+getType(var.type)+", "+getType(var.type)+"* %_"+(this.index-1));                
                if(var.type == "boolean")
                    this.is_bool = "yes";
                return "%_"+this.index;
            }
            else{
                if(var.type == "boolean")
                    this.is_bool = "yes";
                return "%"+r;
            }
        }
        else
            return r;
    }

    /**
    * f0 -> "int"
    * f1 -> "["
    * f2 -> "]"
    */
    public String visit(ArrayType n) {
        return "int[]";
    }

    /**
    * f0 -> "boolean"
    */
    public String visit(BooleanType n) {
        return "boolean";
    }

    /**
    * f0 -> "int"
    */
    public String visit(IntegerType n) {
        return "int";
    }

    /**
    * f0 -> <IDENTIFIER>
    */
    public String visit(Identifier n) {
        return n.f0.toString();
    }

    /**
    * f0 -> <INTEGER_LITERAL>
    */
    public String visit(IntegerLiteral n) {
        return n.f0.toString();
    }

   /**
    * f0 -> "true"
    */
    public String visit(TrueLiteral n) {
        return "1";
    }

    /**
    * f0 -> "false"
    */
    public String visit(FalseLiteral n) {
        return "0";
    }
}