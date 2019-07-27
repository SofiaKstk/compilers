package SymbolTable;

import java.util.*;

public class Method extends List {

	public String type;
	public LinkedList<String> parameters;
    public HashMap<String, Variable> variables;

	public Method(String name, String type) {
		this.name = name;
		this.type = type;
		this.parameters = new LinkedList<>();
        this.variables = new HashMap<>();
	}

    public Variable getVar(String varName) {
		return variables.get(varName);
	}

    public void putParam(String type) {
		parameters.add(type);
	}

    public boolean putVar(Variable var) {
		if (variables.containsKey(var.name))
			return false;
		variables.put(var.name, var);
		return true;
	}

    public boolean signature(Method meth) {	
        if (meth.type.equals(type) && meth.parameters.equals(parameters)){
            return true;
		}		
        return false;
	}

}