package SymbolTable;

import syntaxtree.*;
import java.util.LinkedHashMap;

public class SymbolTable {

    public LinkedHashMap<String, ClassContents> classes; 

    public SymbolTable() {
		classes = new LinkedHashMap<>();
	}

    ClassContents getClass(String name){
        return classes.get(name);
    }
}