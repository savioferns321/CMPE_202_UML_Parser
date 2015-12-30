package com.sjsu.savio.parser.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import net.sourceforge.plantuml.SourceStringReader;

public class NewParser {

	private static final String PUBLIC = "public";
	private static final String PRIVATE = "private";
	private static final String ABSTRACT = "abstract";

	//private static final String FINAL = "leaf";
	private static final String STATIC = "static";
	private static Set<String> dependencyBuffer = new HashSet<String>();
	private static Map<String, String> getSetMap = new HashMap<String, String>();
	private static Map<String, String> assocMap = new HashMap<String, String>();

	public static void main(String[] args) {
		String pkgName = args[0];
		String imgName = args[1];
		/*String pkgName ="C:/HDD/SJSU/Sem 1/202 - Paul Nguyen/Sagar design patterns/Sagar design patterns/Observer Pattern/src";
		
		String imgName = "C:/HDD/SJSU/Sem 1/202 - Paul Nguyen/Sagar design patterns/Sagar design patterns/Observer Pattern/src/output.png";
		*/

		String grammar="";
		try {
			grammar = generatePackageGrammar(pkgName);
			System.out.println(grammar);
		} catch (FileNotFoundException e1) {
			System.out.println(e1.getMessage());
			e1.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

				
		SourceStringReader reader = new SourceStringReader(grammar);
		try {
			reader.generateImage(new File(imgName));
			System.out.println(" Successfully printed output at "+imgName);
		} catch (IOException e) {
			System.out.println(e.getMessage()+"\n Possible cause : the file name given is not a valid file name");
		}
	}

	private static String generatePackageGrammar(String pkgName) throws ParseException, IOException {
		dependencyBuffer.clear();
		getSetMap.clear();
		List<CompilationUnit> cuList = new ArrayList<CompilationUnit>();
		List<String> classNames = new ArrayList<String>();
		List<String> interfaceNames = new ArrayList<String>();
		File packageFolder = new File(pkgName);


		if(!packageFolder.exists()){
			throw new FileNotFoundException("Folder "+pkgName+" does not exist");
		}

		StringBuffer grammarBuffer = new StringBuffer().append("@startuml\n skinparam classAttributeIconSize 0\n");
		FilenameFilter textFilter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".java");
			}
		};

		for(File javaFile : packageFolder.listFiles(textFilter)){
			cuList.add(JavaParser.parse(javaFile));
		}

		List<Object> visitorParameter = new ArrayList<Object>();
		visitorParameter.add(grammarBuffer);
		visitorParameter.add(interfaceNames);
		visitorParameter.add(classNames);
		for(CompilationUnit cu  : cuList){
			new ClassOrInterfaceVisitor().visit(cu, visitorParameter);
		}

		//Declare the class and interface names
		for(String interfaceName : interfaceNames){
			grammarBuffer.append("interface "+interfaceName+" <<interface>> \n");
		}

		//Declare the class and interface names
		for(String className : classNames){
			grammarBuffer.append("class "+className+" \n");
		}
		//Visit the methods
		for(CompilationUnit cu  : cuList){
			if(visitorParameter.size() == 3){
				visitorParameter.add(cu);
			}else{
				visitorParameter.remove(3);
				visitorParameter.add(3, cu);
			}
			new GeneralizationVisitor().visit(cu, visitorParameter);
			new MethodVisitor().visit(cu, visitorParameter);
			new ConstructorVisitor().visit(cu, visitorParameter);
			new FieldVisitor().visit(cu, visitorParameter);

			//TODO Fields

		}
		for(String dep : dependencyBuffer){
			grammarBuffer.append(dep);
		};
		Iterator<Map.Entry<String, String>> it = assocMap.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry pair = (Map.Entry)it.next();
	        grammarBuffer.append(pair.getValue()+"\n");
	    }
		grammarBuffer.append("@enduml");

		return grammarBuffer.toString();
	}

	private static class GeneralizationVisitor extends VoidVisitorAdapter<Object>{
		@Override
		public void visit(ClassOrInterfaceDeclaration obj, Object arg1) {
			List<Object> objList = (List<Object>)arg1;
			StringBuffer grammarBuffer = (StringBuffer)objList.get(0);
			List<String> interfaceList = (List<String>)objList.get(1);
			List<String> classList = (List<String>)objList.get(2);

			if(obj.getExtends()!= null){
				if(classList.contains(obj.getExtends().get(0).getName())){
					grammarBuffer.append(obj.getName()+" --|> "+obj.getExtends().get(0)+"\n");
				}
			}
			if(obj.getImplements()!= null){
				for(ClassOrInterfaceType decl : obj.getImplements()){
					if(interfaceList.contains(decl.getName())){
						grammarBuffer.append(obj.getName()+" ..|> "+decl.getName()+"\n");
					}					
				}
			}
		}
	}

	private static class ClassOrInterfaceVisitor extends VoidVisitorAdapter<Object>{
		@Override
		public void visit(ClassOrInterfaceDeclaration obj, Object arg1) {
			List<Object> objList = (List<Object>)arg1;
			if(obj.isInterface()){
				List<String> interfaceList = (List<String>) objList.get(1);
				interfaceList.add(obj.getName());
			}else{
				List<String> classList = (List<String>) objList.get(2);
				classList.add(obj.getName());
			}
		}
	}

	private static class MethodVisitor extends VoidVisitorAdapter<Object>{
		@Override
		public void visit(MethodDeclaration method, Object arg1) {
			List<Object> objList = (List<Object>)arg1;
			StringBuffer grammarBuffer = (StringBuffer)objList.get(0);
			List<String> interfaceList = (List<String>)objList.get(1);
			List<String> classList = (List<String>)objList.get(2);
			CompilationUnit currentCU = (CompilationUnit)objList.get(3);
			StringBuffer methodDec = new StringBuffer();
			String currentClassName = currentCU.getTypes().get(0).getName();

			if(getMethodModifiers(method).contains(PUBLIC) || interfaceList.contains(currentClassName)){
				methodDec.append(currentClassName+" : +"+method.getName());
				methodDec.append("(");
				methodDec = resolveParameters(methodDec, interfaceList, method, currentClassName);
				methodDec.append(") : "+method.getType());
				if(getMethodModifiers(method).contains(STATIC)){
					methodDec.append("{"+STATIC+"}");
				}
				if(getMethodModifiers(method).contains(ABSTRACT) || interfaceList.contains(currentClassName)){
					methodDec.append("{"+ABSTRACT+"}");
				}
				methodDec.append("\n");
				if(!interfaceList.contains(currentClassName) && !getMethodModifiers(method).contains(ABSTRACT)){
					methodDec = resolveInnerDeps(methodDec, method, interfaceList,currentClassName);
				}
				Pattern getSetPattern = Pattern.compile("([gs]et)([a-zA-Z0-9_]+)");
				Matcher matcher = getSetPattern.matcher(method.getName());
				if(matcher.find()){
					//It's a getter or a setter. Check in the map its there
					getSetMap.put(currentClassName+":"+matcher.group(2).toLowerCase()+":"+method.getName().toLowerCase(), methodDec.toString());
				}
				
				grammarBuffer.append(methodDec);
			}
		}
	}

	public static StringBuffer resolveInnerDeps(StringBuffer inputBuffer,MethodDeclaration method, List<String> interfaces, String classname){

		for(String interfaceStr : interfaces){
			if(method.getBody().getStmts()!= null){
				for(Statement statement : method.getBody().getStmts()){
					if(containsWord(statement.toString(), interfaceStr) && statement.toString().matches("^\\b"+interfaceStr+"\\b.*?")){
						dependencyBuffer.add(classname+" ..> "+interfaceStr+" : uses \n");
					}
				}
			}
			
		}		
		return inputBuffer;
	}

	public static StringBuffer resolveInnerDeps(StringBuffer inputBuffer,ConstructorDeclaration constructor, List<String> interfaces, String classname){

		for(String interfaceStr : interfaces){
			System.out.println(constructor.getDeclarationAsString());
			if(constructor.getBlock()!= null && constructor.getBlock().getStmts()!=null){
				for(Statement statement : constructor.getBlock().getStmts()){
					if(containsWord(statement.toString(), interfaceStr) && statement.toString().matches("^\\b"+interfaceStr+"\\b.*?")){
						inputBuffer.append(classname+" ..> "+interfaceStr+" : uses \n");
					}
				}
			}			
		}		
		return inputBuffer;
	}

	public static StringBuffer resolveParameters(StringBuffer inputBuffer, List<String>interfaces,MethodDeclaration method, String parentClass){
		
		for (Parameter parameter : method.getParameters()){
			inputBuffer.append(parameter.getId().getName()+" : "+parameter.getType().toString()+" , ");
			if(interfaces.contains(parameter.getType().toString()) && !interfaces.contains(parentClass)){
				dependencyBuffer.add(parentClass+" ..> "+parameter.getType().toString()+" : uses \n");
			}
		}
		if(!method.getParameters().isEmpty()){
			inputBuffer.setLength(inputBuffer.length()-3);
		}

		return inputBuffer;
	}

	public static StringBuffer resolveParameters(StringBuffer inputBuffer, List<String>interfaces, ConstructorDeclaration method, String parentClass){
		
		for (Parameter parameter : method.getParameters()){
			inputBuffer.append(parameter.getId().getName()+" : "+parameter.getType().toString()+" , ");
			if(interfaces.contains(parameter.getType().toString()) && !interfaces.contains(parentClass)){
				dependencyBuffer.add(parentClass+" ..> "+parameter.getType().toString()+" : uses \n");
			}
		}
		if(!method.getParameters().isEmpty()){
			inputBuffer.setLength(inputBuffer.length()-3);
		}

		return inputBuffer;
	}

	public static List<String> getMethodModifiers(MethodDeclaration method){

		String modifiers = method.getDeclarationAsString(true, false).substring(0, method.getDeclarationAsString(true, false).indexOf(method.getType().toString()));
		List<String> outputList = Arrays.asList(modifiers.split(" "));
		return outputList;
	}

	public static List<String> getMethodModifiers(ConstructorDeclaration constructor){

		String modifiers = constructor.getDeclarationAsString(true, false).substring(0, constructor.getDeclarationAsString(true, false).indexOf(constructor.getName().toString()));
		List<String> outputList = Arrays.asList(modifiers.split(" "));
		return outputList;
	}


	private static class ConstructorVisitor extends VoidVisitorAdapter<Object>{
		@Override
		public void visit(ConstructorDeclaration constructor, Object arg1) {
			List<Object> objList = (List<Object>)arg1;
			StringBuffer grammarBuffer = (StringBuffer)objList.get(0);
			List<String> interfaceList = (List<String>)objList.get(1);
			CompilationUnit currentCU = (CompilationUnit)objList.get(3);
			String currentClassName = currentCU.getTypes().get(0).getName();

			if(getMethodModifiers(constructor).contains(PUBLIC) || interfaceList.contains(currentClassName)){

				grammarBuffer.append(currentClassName+" : +"+constructor.getName());
				grammarBuffer.append("(");
				grammarBuffer = resolveParameters(grammarBuffer, interfaceList, constructor, currentClassName);
				grammarBuffer.append(")");
				if(getMethodModifiers(constructor).contains(STATIC)){
					grammarBuffer.append("{static}");
				}
				if(getMethodModifiers(constructor).contains(ABSTRACT) || interfaceList.contains(currentClassName)){
					grammarBuffer.append("{abstract}");
				}
				grammarBuffer.append("\n");
				if(!interfaceList.contains(currentClassName)){
					resolveInnerDeps(grammarBuffer, constructor, interfaceList,currentClassName);
				}				
			}
		}
	}

	private static class FieldVisitor extends VoidVisitorAdapter<Object>{
		
		@Override
		public void visit(FieldDeclaration field, Object arg1) {
			List<Object> objList = (List<Object>)arg1;
			StringBuffer grammarBuffer = (StringBuffer)objList.get(0);
			List<String> interfaceList = (List<String>)objList.get(1);
			List<String> classList = (List<String>)objList.get(2);
			CompilationUnit currentCU = (CompilationUnit)objList.get(3);
			String currentClassName = currentCU.getTypes().get(0).getName();
			String varName = field.getVariables().get(0).toString();
			String[] notation;
			List<String> allClassObjects = new ArrayList<>();
			allClassObjects.addAll(classList);
			allClassObjects.addAll(interfaceList);
			StringBuffer varBuffer = new StringBuffer();
			
			if(interfaceList.contains(currentClassName) || (getMethodModifiers(field).contains(PUBLIC) || getMethodModifiers(field).contains(PRIVATE))){
				
				notation = getAssociationNotation(field, allClassObjects);
				if(notation == null){
					//normal member					
					varBuffer.append(currentClassName+" : "+ (getMethodModifiers(field).contains(PUBLIC)? "+" : "-") +field.getVariables().get(0).getId() + " : "+field.getType().toStringWithoutComments()+"\n");
				}else{
					//Associated member
					String assocKey = getAssociationKey(currentClassName, notation[1]);
					if(assocMap.containsKey(assocKey)){
						//Association exists
						String assocValue = assocMap.get(assocKey);
						
						if(assocValue.startsWith(notation[1]+" ")){
							// Field --"" Curr
							assocMap.put(assocKey, assocValue.replace(notation[1]+" ", notation[1]+notation[0]+" "));
						}else if(assocValue.endsWith(" "+notation[1])){
							// Curr -- Field
							assocMap.put(assocKey, assocValue.replace(" "+notation[1], " "+notation[0]+notation[1]));
						}else if(assocValue.startsWith(notation[1]+"\"")){
							//Field"" -- Curr
							assocMap.put(assocKey, assocValue.replace(notation[1]+"\"", notation[1]+"\""+notation[0]));
						}else if(assocValue.endsWith("\""+notation[1])){
							//Field -- ""Curr
							assocMap.put(assocKey, assocValue.replace("\""+notation[1], notation[0]+"\""+notation[1]));
						}
						
						
					}else{
						//New association
						assocMap.put(assocKey, currentClassName+" -- "+notation[0]+notation[1]);
					}					
					
				}
				
				//Check for getter setters
				if(getMethodModifiers(field).contains(PRIVATE)&& getSetMap.containsKey(currentClassName+":"+varName+":"+"get"+varName) && getSetMap.containsKey(currentClassName+":"+varName+":"+"set"+varName)){
					String varStr = varBuffer.toString();
					String grammarStr = grammarBuffer.toString();
					grammarStr = grammarStr.replace(getSetMap.get(currentClassName+":"+varName+":"+"get"+varName), "\n");
					grammarStr = grammarStr.replace(getSetMap.get(currentClassName+":"+varName+":"+"set"+varName), "\n");
					grammarBuffer.setLength(0);
					grammarBuffer.append(grammarStr);
					varStr = varStr.replace("-", "+");
					varBuffer.setLength(0);
					varBuffer.append(varStr);
				}
			}
			grammarBuffer.append(varBuffer);
		}		
	}
	
	public static String getAssociationKey(String clazz, String field){
		String[] s= new String[]{clazz, field};
		Arrays.sort(s);
		return Arrays.toString(s).replaceAll("[\\[\\], ]", "");
	}
	
	public static List<String> getMethodModifiers(FieldDeclaration method){
		String modifiers = method.toStringWithoutComments().substring(0, method.toStringWithoutComments().indexOf(method.getType().toString()));
		List<String> outputList = Arrays.asList(modifiers.split(" "));
		return outputList;
	}
	
	
	public static String[] getAssociationNotation(FieldDeclaration field, List<String> classNames){
		if(classNames.contains(field.getType().toStringWithoutComments())){
			return new String[]{"\"1\"",field.getType().toStringWithoutComments()};
		}
		String collectionRegex = "([a-zA-Z]+)(<)([a-zA-Z]+)(>)";
		String arrayRegex = "([a-zA-Z]+)(\\[)([0-9]+)*(\\])";
		Pattern collectionPattern = Pattern.compile(collectionRegex);
		Matcher collectionMatcher = collectionPattern.matcher(field.getType().toStringWithoutComments());
		if(collectionMatcher.find() && classNames.contains(collectionMatcher.group(3))){
			//Class contains a Collection of the field

			return new String[]{"\"*\"",collectionMatcher.group(3)};
		}
		Pattern arrayPattern = Pattern.compile(arrayRegex);
		Matcher arrayMatcher = arrayPattern.matcher(field.getType().toStringWithoutComments());
		if(arrayMatcher.find() && classNames.contains(arrayMatcher.group(1))){
			//Class contains an array of the field

			return new String[]{"\"*\"",arrayMatcher.group(1)};
		}
		return null;
	}
	
	public static boolean containsWord(String from, String to){
		Pattern wordPattern = Pattern.compile("\\b"+to+"\\b");
		Matcher matcher = wordPattern.matcher(from);
		return matcher.find();
	}

}
