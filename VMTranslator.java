import java.util.List;
import java.util.Arrays;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import java.io.File;
import java.io.FileWriter;

class Translator{
    //used to create unique label names for eq,gt, and lt
    private static int compare = 1;
    
    //the commands that will require the arithLogic function; used in main to determine how to parse through a command
    private static List<String> functions = Arrays.asList("add","sub","neg","eq","gt","lt","and","or","not");
    
    //handles push and pop commands
    public static String pushPop(String s, String fileName){
        String command = s.substring(0,s.indexOf(" ")); //either push or pop
        s = s.substring(s.indexOf(" ") + 1);
        
        String segment = s.substring(0,s.indexOf(" ")); //the type of memory segment to access
        s = s.substring(s.indexOf(" ") + 1);
        
        int i = Integer.parseInt(s); //the position of the chunk of memory to access (or the number to push if constant)
        
        String out = ""; //will output the .asm code to be added to the file
        
        //Accesses the spot in the memory determined by the segment and i
        if(segment.equals("constant"))
            out = out + "@" + i + "\nD=A\n";
        else if(segment.equals("local"))
            out = out + "@LCL\nD=M\n@" + i + "\nD=D+A\n";
        else if(segment.equals("argument"))
            out = out + "@ARG\nD=M\n@" + i + "\nD=D+A\n";
        else if(segment.equals("this"))
            out = out + "@THIS\nD=M\n@" + i + "\nD=D+A\n";
        else if(segment.equals("that"))
            out = out + "@THAT\nD=M\n@" + i + "\nD=D+A\n";
        else if(segment.equals("pointer"))
            out = out + "@3\nD=A\n@" + i + "\nD=D+A\n";
        else if(segment.equals("temp"))
            out = out + "@5\nD=A\n@" + i + "\nD=D+A\n";
        else if(segment.equals("static")) //creates a variable (from RAM[16]) onward) using i
            out = out + "@" + fileName + "." + i + "\nD=M\n";
     
        //performs the actual push/pop using that memory
        if(command.equals("push")){
            if(!segment.equals("static")){
                if(!segment.equals("constant")) //an extra step is needed to access the data, that a constant doesn't have to deal with
                    out = out + "A=D\nD=M\n";
                out = out + "@SP\nA=M\nM=D\nD=A+1\n@SP\nM=D\n";
            }
            else //accounts for the differences with static memory
                out = out + "@SP\nA=M\nM=D\nD=A+1\n@SP\nM=D\n";
        }
        else if(command.equals("pop")){
            if(!segment.equals("static"))
                out = out + "@R13\nM=D\n@SP\nAM=M-1\nD=M\n@R13\nA=M\nM=D\n";
            else //accounts for the differences with static memory
                out = out + "@R13\nM=D\n@SP\nAM=M-1\nD=M\n@" + fileName + "." + i + "\nM=D\n";
        }
        return out;
    }
    
    //handles every other command (the arithmetic and logical ones)
    public static String arithLogic(String s){
        String out = "";
            if(s.equals("add"))
                out = "@SP\nM=M-1\nA=M\nD=M\nA=A-1\nD=D+M\nM=D\nD=A+1\n@SP\nM=D\n";
            else if(s.equals("sub"))
                out = "@SP\nM=M-1\nA=M\nD=M\nA=A-1\nD=M-D\nM=D\nD=A+1\n@SP\nM=D\n";
            else if(s.equals("and"))
                out = "@SP\nM=M-1\nA=M\nD=M\nA=A-1\nD=D&M\nM=D\nD=A+1\n@SP\nM=D\n";
            else if(s.equals("or"))
                out = "@SP\nM=M-1\nA=M\nD=M\nA=A-1\nD=D|M\nM=D\nD=A+1\n@SP\nM=D\n";
            else if(s.equals("neg"))
                out = "@SP\nM=M-1\nA=M\nM=-M\nD=A+1\n@SP\nM=D\n";
            else if(s.equals("not"))
                out = "@SP\nM=M-1\nA=M\nM=!M\nD=A+1\n@SP\nM=D\n";
            else if(s.equals("eq"))
                out = "@SP\nM=M-1\nA=M\nD=M\nM=0\nA=A-1\nD=M-D\n@EQ" + compare + "\nD;JEQ\n@0\nD=A\n@SP\nA=M-1\nM=D\n@SP\n@doneEQ" + compare + "\n0;JMP\n(EQ" + compare + ")\nD=-1\n@SP\nA=M-1\nM=D\n@SP\n(doneEQ" + compare + ")\n";
            else if(s.equals("gt"))
                out = "@SP\nM=M-1\nA=M\nD=M\nM=0\nA=A-1\nD=M-D\n@EQ" + compare + "\nD;JGT\n@0\nD=A\n@SP\nA=M-1\nM=D\n@SP\n@doneEQ" + compare + "\n0;JMP\n(EQ" + compare + ")\nD=-1\n@SP\nA=M-1\nM=D\n@SP\n(doneEQ" + compare + ")\n";
            else if(s.equals("lt"))
                out = "@SP\nM=M-1\nA=M\nD=M\nM=0\nA=A-1\nD=M-D\n@EQ" + compare + "\nD;JLT\n@0\nD=A\n@SP\nA=M-1\nM=D\n@SP\n@doneEQ" + compare + "\n0;JMP\n(EQ" + compare + ")\nD=-1\n@SP\nA=M-1\nM=D\n@SP\n(doneEQ" + compare + ")\n";
        compare++;
        return out;
    }
    
    
    public static void main(String[] args){
        BufferedReader reader;
        try{
            //name of the input file
            String inputFile = args[0];
            //begin reading from the input file
            reader = new BufferedReader(new FileReader(inputFile));
            //used to create other file names
            String outFile = inputFile.substring(0,inputFile.indexOf('.')) + ".asm";
            

            //create output file
            File file = new File(outFile);
            FileWriter writer = new FileWriter(outFile);
            
            String line = reader.readLine();
            int lineNum = 0;
            while(line != null){
                if(functions.indexOf(line) != -1){ //arithmetic & logic
                    writer.write(arithLogic(line));
                }
                else if((line.contains("push") || line.contains("pop")) && (line.indexOf('/') != 0 && line.indexOf('/') != 1)){ //push & pop
                    writer.write(pushPop(line,inputFile.substring(0,inputFile.indexOf('.'))));
                }
                line = reader.readLine();
            }
            reader.close();
            writer.close();
          
        } catch(IOException e){
            System.err.format("ERROR: Incorrect or Nonexistent File");
        }
    }
}