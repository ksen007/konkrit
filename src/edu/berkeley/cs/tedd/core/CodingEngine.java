package edu.berkeley.cs.tedd.core;

import bsh.*;

import java.io.*;
import java.util.*;

/**
 * Copyright (c) 2006-2011,
 * Koushik Sen    <ksen@cs.berkeley.edu>
 * All rights reserved.
 * <p/>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * <p/>
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * <p/>
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * <p/>
 * 3. The names of the contributors may not be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 * <p/>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
public class CodingEngine implements Serializable {
    private static final String globalFunctionNameVarName = "__tedd_functionName";
    private static final String globalArgumentsVarName = "__tedd_arguments";

    public static String formatFixedWidth(long value, int width) {
        String ret = value+"";
        if (ret.length()<width) {
            StringBuilder sb = new StringBuilder();
            int rest = width - ret.length();
            for(int i=0; i<rest;i++) {
                sb.append('0');
            }
            sb.append(ret);
            return sb.toString();
        }
        return ret;
    }


    Map<String, Function> functions;
    Map<String, Function> mocks;

    private LinkedHashMap<String, ArrayList<String>> tests;
    private Interpreter currentInterpreter = null;
    private String currentClass;

    private CallStack callStack;
    private  Console console;

    private LinkedHashSet<String> watchList;
    private Trace trace;
    private boolean isTesting;

    private void commonInit() {
        console = new Console();
        trace = new Trace(console);
        callStack = new CallStack();
        currentInterpreter = null;
    }

    public CodingEngine() {
        functions = new HashMap<String, Function>();
        mocks = new HashMap<String, Function>();
        tests = new LinkedHashMap<String,ArrayList<String>>();
        watchList = new LinkedHashSet<String>();

        commonInit();
    }

    private void writeObject(java.io.ObjectOutputStream out)
        throws IOException {
        out.writeObject(functions);
        out.writeObject(mocks);
        out.writeObject(tests);
        out.writeObject(watchList);
    }


    private void readObject(java.io.ObjectInputStream in)
        throws IOException, ClassNotFoundException {
        functions = (HashMap<String, Function>) in.readObject();
        mocks = (HashMap<String, Function>) in.readObject();
        tests = (LinkedHashMap<String, ArrayList<String>>) in.readObject();
        watchList = (LinkedHashSet<String>) in.readObject();
        commonInit();
    }



    private void reset() throws EvalError {
        trace.clear();
        currentInterpreter = createInterpreter();
        callStack.reset(currentInterpreter.getNameSpace());
    }

    private Interpreter createInterpreter() throws EvalError {
        Interpreter ret = new Interpreter();
        ret.setStrictJava(true);
        ret.eval("String " + globalFunctionNameVarName + " = null;");
        ret.eval("Object[] " + globalArgumentsVarName + " = null;");
        ret.eval("void invoke(String methodName, Object[] arguments) { global."+globalFunctionNameVarName+" = methodName; global."+globalArgumentsVarName+" = arguments; }");

        return ret;
    }

    private boolean handleFunctionCall() throws EvalError, TeddError, UtilEvalError {
        String functionName = (String) currentInterpreter.get(globalFunctionNameVarName);
        if (functionName==null) return false;
        Function fun = functions.get(functionName);
        if (fun==null) {
            throw new TeddError("Function "+functionName+" is not declared.");
        }

        callStack.push(fun,currentInterpreter.getNameSpace());

        Object[] arguments = (Object[]) currentInterpreter.get(globalArgumentsVarName);
        fun.setArguments(currentInterpreter,callStack.getNameSpace(),arguments);
        currentInterpreter.eval("global.__tedd_functionName = null;");


        return true;
    }

    private void handleFunctionReturn() throws EvalError, UtilEvalError {
        if (callStack.currentStmt.isReturn()) {
            boolean flag;
            do {
                flag = callStack.pop();
                if (callStack.isEmpty()) {
                    isTesting = false;
                    return;
                }
            } while(flag);
        }
    }

    public void declareFunction(String sig) throws TeddError {
        Function ret = new Function(sig);
        if(functions.containsKey(ret.getFunctionName())) {
            throw new TeddError("Function "+sig+" already declared.");
        } else {
            functions.put(ret.getFunctionName(), ret);
            System.out.println("Function "+sig+" declared.");
        }
    }

    public void unDeclareFunction(String sig) throws TeddError {
        Function ret = new Function(sig);
        if(functions.containsKey(ret.getFunctionName())) {
            functions.remove(ret.getFunctionName());
            System.out.println("Function "+sig+" un-declared.");
        } else {
            throw new TeddError("Function " + sig + " is not declared.");
        }
    }

    private ArrayList<String> getMocks(String sig) {
        ArrayList<String> ret;
        ret = tests.get(sig);
        if (ret==null) {
            ret = new ArrayList<String>();
            tests.put(sig,ret);
        }
        return ret;
    }

    public void editFunction(String sig) throws TeddError, EvalError, UtilEvalError {
        reset();
        isTesting = true;
        currentInterpreter.eval(sig, callStack.getNameSpace());
        if (!handleFunctionCall()) {
            throw new TeddError(sig + " must be declared.");
        } else {
            ArrayList<String> mocks = getMocks(sig);
            trace.setCurrentTest(sig,mocks);
            trace.updateNext(callStack.size(), callStack.currentStmt);
        }
    }

    public void print() {
        trace.print(callStack.size());
        printNext();
    }

    public void printNext() {
        trace.printNext(callStack.size(),callStack.currentStmt);
    }


    public void setMock(String mockName) throws TeddError {
        if (mockName!=null) {
            Function ret = new Function("void "+mockName+"();");
            if(!mocks.containsKey(ret.getFunctionName())) {
                mocks.put(ret.getFunctionName(), ret);
                System.out.println("Mock "+mockName+" declared.");
            }
        }
        trace.setMock(mockName);
        trace.updateNext(callStack.size(), callStack.currentStmt);
    }

    public void step() throws EvalError, TeddError, UtilEvalError {
        if (isTesting) {
            String mockName;
            if ((mockName = trace.getMock())!=null) {
                Function fun = mocks.get(mockName);
                if (fun==null) {
                    throw new TeddError("Mock "+mockName+" is not found.");
                }
                callStack.currentStmt = fun.getRoot();
                System.out.println(trace.add(callStack.size(), new NonCondStmtNode("mock "+mockName)));
                trace.updateNext(callStack.size(), callStack.currentStmt);
                //System.out.println("Doing mock "+mockName);
            } else if (callStack.currentStmt.nextStmt() != null) {
                callStack.currentStmt = callStack.currentStmt.forward(currentInterpreter, callStack.getNameSpace());
                System.out.println(trace.add(callStack.size(), callStack.currentStmt));
                handleFunctionCall();
                handleFunctionReturn();
                trace.updateNext(callStack.size(), callStack.currentStmt);
            } else {
                throw new TeddError("No more statement to execute.");
            }
        } else {
            throw new TeddError("Not in the middle of a test. use !test test-name to create a test.");
        }
    }

    private boolean isForwardPossible() {
        return trace.getMock()!=null || callStack.currentStmt.nextStmt()!=null;
    }

    public void next() throws EvalError, TeddError, UtilEvalError {
        int depth = callStack.size();
        step();
        while (callStack.size()>depth && isForwardPossible()) {
            step();
        }
    }

    public void Continue() throws EvalError, TeddError, UtilEvalError {
        while(isForwardPossible()) {
            step();
        }
    }

    public void rerun(int n) throws EvalError, TeddError, UtilEvalError {
        editFunction(trace.getCurrentTest());
        if (n==-1) Continue();
        for(int i=0; i<n; i++) {
            step();
        }
    }

    public void previous() throws EvalError, TeddError, UtilEvalError {
        rerun(trace.size()-1);
    }

    public void execute(String stmt) throws EvalError {
        currentInterpreter.eval(stmt, callStack.getNameSpace());
    }

    public void insert(String stmt) {
        callStack.currentStmt.insert(stmt);
        trace.updateNext(callStack.size(), callStack.currentStmt);
    }

    public void add(String stmt) throws TeddError {
        if (!isForwardPossible())
            callStack.currentStmt.insert(stmt);
        else
            throw new TeddError("Cannot add statement.  Next statement exists.  Try insert or step.");
    }

    public void remove() throws TeddError {
        callStack.currentStmt.remove();
        trace.updateNext(callStack.size(), callStack.currentStmt);
    }

    public void replace(String stmt) throws TeddError {
        callStack.currentStmt.replace(stmt);
        trace.updateNext(callStack.size(), callStack.currentStmt);
    }

    public void watch(String stmt) {
        if (watchList.contains(stmt)) {
            watchList.remove(stmt);
        } else {
            watchList.add(stmt);
        }
    }

    public void changeWatchList() {
        if (console != null)
            console.changeWatch();
    }

    public void executeWatchList() {
        if (console != null)
            console.clearWatch();
        System.out.println("Watch list: ");
        for(String stmt:watchList){
            try {
                String ret = (String)currentInterpreter.eval("\"" + stmt + " = \"+" + stmt + "+\" \"", callStack.getNameSpace());
                System.out.print(ret);
                if (console != null)
                    console.addWatchText(ret);
            } catch (Exception e) {
            }
        }
        System.out.println();
    }

    public void testAll() throws EvalError, TeddError {
        List<String> incomplete = new LinkedList<String>();
        List<String> failed = new LinkedList<String>();
        List<String> passed = new LinkedList<String>();
        for(String test:tests.keySet()) {
            System.out.println("*********************************");
            System.out.println("Testing "+test);
            try {
                editFunction(test);
                Continue();
                if (isTesting) {
                    System.err.println("******** Test incomplete:\""+test+"\" ***********");
                    incomplete.add(test);
                } else {
                    System.out.println("******** Test passed ***********");
                    passed.add(test);
                }
            } catch(EvalError ee) {
                System.err.println("******** Test failed:\""+test+"\" ***********");
                System.err.println(ee);
                ee.printStackTrace();
                failed.add(test);
            } catch (TeddError te) {
                System.err.println("I should not be here");
                System.err.println(te);
                te.printStackTrace();
                System.exit(-1);
            } catch (UtilEvalError utilEvalError) {
                System.err.println("******** Test failed:\""+test+"\" ***********");
                System.err.println(utilEvalError);
                utilEvalError.printStackTrace();
                failed.add(test);
            }

        }
        System.out.println("passed = " + passed);
        System.out.println("incomplete = " + incomplete);
        System.out.println("failed = " + failed);
    }


    public String getPrompt() {
        return callStack.getPrompt();
    }

    public void list() {
        System.out.println("******** Functions Declared *********");
        for(String fname:functions.keySet()) {
            Function foo = functions.get(fname);
            System.out.println(foo.getSig());
        }
        System.out.println("*************************************");
    }


    public void generate(String cname) throws IOException {
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(cname+".java")));
        out.println("// auto generated class");
        out.println();
        out.print("public class ");
        out.print(cname);
        out.println(" {");
        for(String fname:functions.keySet()) {
            functions.get(fname).generate(out,this);
            out.println();
        }

        out.print(Function.tab);
        out.println("public static void main(String args[]) {");
        for(String test:tests.keySet()) {
            out.print(Function.tab);
            out.print(Function.tab);
            out.println(test);
        }
        out.print(Function.tab);
        out.println("}");
        out.println("}");
        out.close();
    }

    public void dump(String filename) throws IOException {
        ObjectOutputStream out;
        out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(filename+".tedd")));
        out.writeObject(this);
        out.close();
    }

    public void save() throws IOException, TeddError {
        if (currentClass == null) {
            throw new TeddError("Current class name is not set.  Use saveas command.");
        } else {
            dump(currentClass);
            generate(currentClass);
            System.out.println("Saved.");
        }
    }

    public void saveas(String cname) throws IOException, TeddError {
        if ((new File(cname+".java")).exists()) {
            throw new TeddError(cname+".java already exists.  Pick a different class name");
        } else {
            currentClass = cname;
            dump(currentClass);
            generate(currentClass);
            System.out.println("Saved.");
        }
    }

    public void setCurrentClass(String currentClass) {
        this.currentClass = currentClass;
    }

}
