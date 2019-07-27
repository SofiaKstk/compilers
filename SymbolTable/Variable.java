package SymbolTable;

public class Variable extends Struct {

    public String name;
	public String type;

		
	public Variable(String name, String type) {
		this.name = name;
		this.type = type;
	}
}