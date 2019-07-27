package SymbolTable;

public abstract class List extends Struct {
    public String name;
    abstract public boolean putVar(Variable var);
}