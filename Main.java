import syntaxtree.*;
import java.io.*;
import SymbolTable.*;

class Main {
    public static void main (String [] args){
        if (args.length == 0){
            System.err.println("No files given");
            System.exit(1);
        }
        else {
            for (String arg:args){
                try {
                    FileInputStream fis = new FileInputStream(arg); 
                    MiniJavaParser parser = new MiniJavaParser(fis);
                    Goal root = parser.Goal();
                    SymbolTable symTable = new SymbolTable();
                    MiniJavaVisitorSymbol table = new MiniJavaVisitorSymbol(symTable);
                    String isFine = root.accept(table, null);
                    if(isFine == "not"){
                        System.out.println("One or more errors have been detected.");
                        continue;
                    }
                    MiniJavaVisitorForward forward = new MiniJavaVisitorForward(symTable);
                    isFine = root.accept(forward, null);
                    if(isFine == "not"){
                        System.out.println("One or more errors have been detected.");
                        continue;
                    }
                    MiniJavaVisitorTypeCheck types = new MiniJavaVisitorTypeCheck(symTable);
                    
                    isFine = root.accept(types, null);
                    if(isFine == "not"){
                        System.out.println("One or more errors have been detected.");
                        continue;
                    }
		
		            System.out.println("No errors were detected.");



                    System.out.println("Let's produce llmv code!");

                    File old = new File(arg);
                    String file = old.getName();
                    int pos = file.lastIndexOf(".");
                    file = file.substring(0, pos);
                    file = file.concat(".ll");
                    file = "llvm/"+file;
                    File f = new File(file);

                    if(f.exists())
	                    f.delete();
                    FileOutputStream fos = new FileOutputStream(f, true);

                    MiniJavaVisitorLL ll = new MiniJavaVisitorLL(symTable, fos);
                    root.accept(ll);


                    try {
                        if (fos != null) {
                            fos.close();
                        }
                    }
                    catch (Exception ioe) {
                        System.out.println("Error while closing stream: " + ioe);
                    }
 
                    System.out.println("All done!");
                    
                }
                catch(Exception ex){
                    System.err.println("In file "+arg+": "+ ex.getMessage());
                }  
            }
        }
    }
}