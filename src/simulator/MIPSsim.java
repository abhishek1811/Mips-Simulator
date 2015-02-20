package simulator;

import java.util.List;
import java.util.HashMap;
import java.nio.file.Path;
import java.io.FileWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.nio.file.Files;
import java.io.IOException;
import java.util.Collections;
import java.io.BufferedWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * @author Abhishek
 */

public class MIPSsim  {

	static int step = 0;
	static int tokennum = 0 ;
	final static String rFile = "registers.txt";
	final static String dFile = "datamemory.txt";
	final static String iFile = "instructions.txt";
	final static String resultFile = "simulation.txt";
	static Charset Encoding = StandardCharsets.UTF_8;
	public static Registers r = new Registers();
	public static DataMemory d = new DataMemory();
	public static ResultBuffer rb = new ResultBuffer();
	public static AddressBuffer adb = new AddressBuffer();
	public static ArithmeticBuffer aib = new ArithmeticBuffer();
	public static InstructionMemory i = new InstructionMemory();
	public static InstructionBuffer inb = new InstructionBuffer();
	public static LoadInstructionBuffer lib = new LoadInstructionBuffer();
	

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub

		MIPSsim p = new MIPSsim();
		ArrayList<String> dataList = readFile(dFile);
		ArrayList<String> instructionList = readFile(iFile);
		ArrayList<String> registerList = readFile(rFile);
		
		d.setMyValues(dataList);
		i.setInstruction(instructionList);
		r.setMyValues(registerList);
		do{
			p.executeState();
		}while(p.canTrigger());
		p.executeState();
		
	}
	
	static public ArrayList<String> readFile(String filename)throws IOException{
		Path path = null;
		try{
			path = Paths.get(filename);
			
		}
		catch(Exception e){
			System.out.println("No such file exist");
		}
		return (ArrayList<String>) Files.readAllLines(path,Encoding);
	}
	
	Boolean canTrigger(){
		return ( (rb.instruction.size()!=0) || (adb.instruction!="") || (aib.instruction!="") 
				|| (lib.instruction!="") || (inb.instruction!="") || (i.instruction.size() >0) );
	}
	String removeBrackets(List<String> a){
		String temp = a.toString().replace("[", "");
		return temp.replace("]", "").replace(" ", "");
	}
	public  void writeInFile()throws IOException{
	
		FileWriter output = new FileWriter(MIPSsim.resultFile,true);
		try(BufferedWriter w = new BufferedWriter(output)){
			w.write("STEP "+MIPSsim.step+":");
			w.newLine();
			w.write("INM:"+ removeBrackets(   i.state()) ) ;
			w.newLine();
			w.write("INB:"+removeBrackets( inb.state()) );
			w.newLine();
			w.write("AIB:"+ removeBrackets(  aib.state()) );
			w.newLine();
			w.write("LIB:"+removeBrackets( lib.state()) );
			w.newLine();
			w.write("ADB:"+removeBrackets(  adb.state()) );
			w.newLine();
			w.write("REB:"+removeBrackets(  rb.state()) );
			w.newLine();
			List<String> temp = ((Registers) r).state();
			Collections.sort(temp);
			w.write("RGF:"+removeBrackets( temp)); 
			w.newLine();
			w.write("DAM:"+removeBrackets(  d.state())+"\n");
			w.newLine();
			MIPSsim.step++;
		}
	}

	void executeState() throws IOException{
		
		writeInFile();
		if(rb.instruction.size()!=0){
			r.writeInRGF(rb); // writes in result buffer and then empty's result buffer instruction.//FIX its arrayList now.
		}else{}
		
		if((adb.instruction!="") && (d.valuesMap.containsKey(adb.operand1)) ){
			adb.setDataAddress(d);
			rb.setResultWithADB(adb);
		}else{}
		
		if(aib.instruction!=""){
			aib.asu();
			rb.setResultWithAIB(aib);
		}else{}
		
		if((lib.instruction!="")&&(adb.instruction =="")){
			lib.addr();
			adb.setMyInstructions(lib);
		}else{}
		
		if( (inb.instruction!="") && (inb.isArithmetic())){
			aib.setMyInstructions(inb);
		}else{}
		
		if( (inb.instruction!="") && (!inb.isArithmetic())){
			lib.setMyInstructions(inb);
		}else{}
		
		if( (i.instruction.size() >0) ){ 
			Boolean checkArithmetic = ( (r.valuesMap.containsKey(i.operand1.get(0))) && (r.valuesMap.containsKey(i.operand2.get(0))) );
			Boolean checkLoad = (r.valuesMap.containsKey(i.operand1.get(0)));
			if(i.isArithmetic() & checkArithmetic){
				i.decodeInstruction(0, r, i);
				inb.setInstructionBuffer(i);
			}
			else if(!i.isArithmetic() & checkLoad){
				i.decodeInstruction(0, r, i);
				inb.setInstructionBuffer(i);
			}	
		}else{}
	}
	
}

class Buffer{
	
	String instruction = "";
	String operation = "";
	String result = "";
	String operand1 = "";
	String operand2 = "";
	
	public void setMyInstructions(InstructionBuffer inb){
		instruction = inb.instruction;
		operation = inb.operation;
		result = inb.result;
		operand1 = inb.operand1;
		operand2 = inb.operand2;
		inb.instruction = "";	
		
	}
	
	public List<String> state(){
		List<String> temp = new ArrayList<>();
		temp.add(instruction);
		return temp;
	}
}

class AddressBuffer extends Buffer{
	// we need to copy all from load buffer since we have to perform operations here.
	String resultOp = "";
	public void setMyInstructions(LoadInstructionBuffer lib ){
		instruction = "<"+lib.result+","+ lib.resultOp+">";
		operation = lib.operation;
		result = lib.result;
		operand1 = lib.resultOp;
		lib.instruction = "";
	}
	public void setDataAddress(Object d ){
		resultOp = ((DataMemory) d).valuesMap.get(operand1)+"";
		
	}
}

class LoadInstructionBuffer extends Buffer{
	// set my instruction is being done by common where everything is set.
	String resultOp = "";
	void addr(){
		resultOp =  (Integer.parseInt(operand1)+Integer.parseInt(operand2))+"";
	}
}

class ArithmeticBuffer extends Buffer{
	// set my instruction is being done by common where everything is set.
	String resultOp = "";
	void asu(){
		if(operation.equals("ADD"))
			resultOp = (Integer.parseInt(operand1)+Integer.parseInt(operand2))+"";
		else if(operation.equals("SUB")){
			resultOp = (Integer.parseInt(operand1)-Integer.parseInt(operand2))+"";
		}		
	}
	
}

class InstructionBuffer extends Buffer {
		
	Boolean isArithmetic(){
		return (( operation.equals("ADD") )|| (operation.equals("SUB")));
	}
	
	public void setInstructionBuffer(InstructionMemory i) throws IOException{
		instruction = i.instruction.get(0);
		operation = i.operation.get(0);
		result = i.result.get(0);
		operand1 = i.operand1.get(0);
		operand2 = i.operand2.get(0);
		removeInstructionFromMemory(i);
		MIPSsim.tokennum++;	
	}
	
	void removeInstructionFromMemory(InstructionMemory i){
		i.instruction.remove(0);
		i.operation.remove(0);
		i.result.remove(0);
		i.operand1.remove(0);
		i.operand2.remove(0);
	}

}
class Memory{
	
	ArrayList<String> instruction = new ArrayList<String>();
	ArrayList<String> operation = new ArrayList<String>();
	ArrayList<String> result = new ArrayList<String>();
	ArrayList<String> operand1 = new ArrayList<String>();
	ArrayList<String> operand2 = new ArrayList<String>();
	HashMap<String,Integer> valuesMap = new HashMap<String,Integer>();
	
	void setMyValues(ArrayList<String>list){
		if(list.size()<=8){
			for(int j= 0;j < list.size();j++){
				instruction.add( list.get(j));
				String str = instruction.get(j);
				int pos = str.indexOf(",");
				valuesMap.put(str.substring(str.indexOf("<")+1, pos ), Integer.parseInt(str.substring(pos+1, str.indexOf(">")) ));
				result.add (str.substring(str.indexOf("<")+1, pos ));
				operand1.add(str.substring(pos+1, str.indexOf(">")));
			}
		}else{
			System.out.println("Token out of bound! Max token allowed :8");
		}
		
	}

	public List<String> state(){
		List<String> temp = new ArrayList<>();
		for(int i=0;i< instruction.size();i++){
			temp.add(instruction.get(i));
		}	
		return temp;
	}
}
class ResultBuffer extends Memory{
	
	public void setResultWithAIB(ArithmeticBuffer aib){

			instruction.add(instruction.size(), "<"+aib.result+","+aib.resultOp+">");
			result.add( result.size() , aib.result );
			operand1.add  (operand1.size(), aib.resultOp );
			aib.instruction = "";

	}
	public void setResultWithADB(AddressBuffer adb){
		
		instruction.add(instruction.size() , "<"+ adb.result+","+ adb.resultOp+">");
		result.add (result.size(), adb.result );
		operand1.add (operand1.size(), adb.resultOp );
		adb.instruction = "";
	}
	public void clearResultBuffer(){
		instruction.remove(0);
		result.remove(0);
		operand1.remove(0);
	}

}

class InstructionMemory extends Memory {

	public void setInstruction(ArrayList<String>list){
		if(list.size()<=8){
			for(int j= 0;j < list.size();j++){
				instruction.add(list.get(j));
				String str = instruction.get(j);		
				int pos = str.indexOf(",");
				operation.add(str.substring(str.indexOf("<")+1, pos ));
				result.add( str.substring(pos+1, str.indexOf(",",pos+1) ));
				pos = str.indexOf(",",pos+1);
				operand1.add( str.substring(pos+1 , str.indexOf(",",pos+1)) );
				pos = str.indexOf(",",pos+1);
				operand2.add( str.substring(pos+1 , str.indexOf(">",pos+1)));
				//System.out.println(operation.get(j)+" "+result.get(j)+" "+operand1.get(j)+" "+operand2.get(j));
			}
		}
		else{
			System.out.println("Instructions token cannot be more than 8");
			System.exit(1);
		}	
	}
	public void  decodeInstruction(int tokennum,Registers r,InstructionMemory i ) throws IOException{
		
		//System.out.println("Token Triggered: "+instruction.get(0));
		if((operation.get(0).equals("ADD"))||(operation.get(0).equals("SUB"))){
			String op1 = operand1.get(0);
			String op2 = operand2.get(0);
			String newOp1 = instruction.get(0).replace(op1, (r.valuesMap.get(op1)).toString() );
			instruction.set(0, newOp1);
			String newOp2 = instruction.get(0).replace(op2, (r.valuesMap.get(op2)).toString() );
			instruction.set(0,newOp2);
			operand1.set(0, (r.valuesMap.get(op1)).toString());
			operand2.set(0, (r.valuesMap.get(op2)).toString());
		}
		else{
			String op1 = operand1.get(0);
			String newOp1 = instruction.get(0).replace(op1, (r.valuesMap.get(op1)).toString() );
			instruction.set(0, newOp1);
			operand1.set(0, (r.valuesMap.get(op1)).toString());
		}	
		
	}
	Boolean isArithmetic(){
		return (( operation.equals("ADD") )|| (operation.equals("SUB")));
	}

}

class Registers extends Memory{
	
	void writeInRGF(ResultBuffer rb){ 
		
		if( valuesMap.containsKey(rb.result.get(0)) ){
			int postion = result.indexOf( rb.result.get(0) );
			instruction.set(postion, rb.instruction.get(0) );
			valuesMap.remove( rb.result.get(0) ) ;
			valuesMap.put( rb.result.get(0) ,Integer.parseInt(rb.operand1.get(0)));
		}else{
			instruction.add( rb.instruction.get(0) );
			valuesMap.put( rb.result.get(0) ,Integer.parseInt(rb.operand1.get(0)));
		}
		rb.clearResultBuffer(); // add value and then clear from buffer
	}
}

class DataMemory extends Memory{
	
}