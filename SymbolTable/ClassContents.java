package SymbolTable;

import java.util.*;

public class ClassContents extends List {

    public LinkedHashMap<String, Variable> variables;
    public LinkedHashMap<String, Method> methods;
    public ClassContents parent;
	public int varStack;
	public int varStackEnd;	
	public int methStack;
	public int methStackEnd;


    public ClassContents(String name, ClassContents par) {
		this.name = name;
        variables = new LinkedHashMap<>();
		methods = new LinkedHashMap<>();
		this.parent = par;
		varStack = 0;
		methStack = 0;
		varStackEnd = 0;
		methStackEnd = 0;
	}

    public Variable getVar(String varName) {
		Variable var = variables.get(varName);
		if (var != null)
			return var;    
		if (parent != null)
			return parent.getVar(varName);
		return null;
	}

    public Method getMethod(String methName) {
		Method mth = methods.get(methName);
		if (mth != null)
			return mth;
		if (parent != null)
			return parent.getMethod(methName);
		return null;
	}

    public boolean putVar(Variable var) {
		if (variables.containsKey(var.name))
			return false;
		variables.put(var.name, var);
		return true;
	}

    public boolean putMeth(Method meth) {
		if (methods.containsKey(meth.name))
			return false;

		Method mth = getMethod(meth.name);
		if (mth != null){
			if (!mth.signature(meth))
				return false;	
		}
		methods.put(meth.name, meth);
		return true;
	}

}