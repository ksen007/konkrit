package edu.berkeley.cs.konkrit.core;

import bsh.EvalError;
import bsh.Interpreter;
import bsh.NameSpace;
import bsh.UtilEvalError;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
public class Function implements Serializable {
    private NonCondStmtNode root;
    private String functionName;
    private String returnType;
    private List<String> argumentTypes;
    private List<String> argumentNames;
    private String sig;
    private boolean isInline;
    private boolean isInsideGeneration;


    public Function(String sig) throws TeddError {
        this.sig = sig;
        this.isInsideGeneration = false;
        Pattern p = Pattern.compile("(\\w+)\\s+(@)?(\\w+)\\s*\\(([^\\)]*)\\).*");
        Matcher m = p.matcher(sig);
        m.find();
        returnType = m.group(1);
        String modifier = m.group(2);
        this.isInline = modifier!=null && modifier.trim().equals("@");
        if (isInline && !returnType.equals("void"))
            throw new TeddError("Return type of an inline function must be void.");
        functionName = m.group(3);
        String arguments = m.group(4);
        String args[] = arguments.split("\\,");
        argumentTypes = new LinkedList<String>();
        argumentNames = new LinkedList<String>();
        
        for(String arg: args){
            arg = arg.trim();
            int idx = arg.lastIndexOf(' ');
            if (idx != -1){
                String type = arg.substring(0,idx);
                String name = arg.substring(idx+1);
                argumentTypes.add(type);
                argumentNames.add(name);
            }
        }

        root = new NonCondStmtNode("** begin **");
        //System.out.println("Declared "+this);
    }

    public String getSig() {
        return sig;
    }

    public String getFunctionName() {
        return functionName;
    }

    public static void main(String[] args) throws TeddError {
        System.out.println((new Function(" void food() ;")));
    }

    @Override
    public String toString() {
        return isInline()+":"+returnType+":"+functionName+":"+ argumentTypes +":"+ argumentNames;
    }

    public NonCondStmtNode getRoot() {
        return root;
    }

    public void setArguments(Interpreter currentInterpreter, NameSpace newNameSpace, Object[] arguments) throws UtilEvalError, EvalError {
        int i = 0;
        Iterator<String> typeIter = argumentTypes.iterator();
        Iterator<String> nameIter = argumentNames.iterator();
        while(typeIter.hasNext()) {
            currentInterpreter.eval(typeIter.next()+" "+nameIter.next()+";",newNameSpace);
        }
        for (String argument: argumentNames) {
            newNameSpace.setVariable(argument,arguments[i],true);
            i++;
        }
    }


    final public static String tab = "    ";

    private void generate(PrintWriter out, String prefix, StmtNode stmt, CodingEngine ce) {
        if (stmt == null) return;
        if (stmt instanceof InlineInvokeStmtNode) {
            InlineInvokeStmtNode is = (InlineInvokeStmtNode) stmt;
            String[] components = is.getComponents();
            Function callee = ce.functions.get(components[0]);
            for (int i=0; i<components.length-1;i++) {
                out.print(prefix);
                out.print(callee.argumentNames.get(i));
                out.print(" = ");
                out.print(components[i+1]);
                out.println(";");
            }
            if (callee.isInsideGeneration) {
                out.print(prefix);
                out.print("continue ");
                out.print(components[0]);
                out.println(";");
            } else {
                callee.isInsideGeneration = true;
                out.print(prefix);
                out.print(components[0]);
                out.println(": while(true) {");
                callee.generate(out,prefix+tab,callee.root.nextStmt(),ce);
                callee.isInsideGeneration = false;
                out.print(prefix);
                out.println("}");
                generate(out, prefix, stmt.nextStmt(), ce);
            }
        } else if (stmt instanceof InvokeAssignStmtNode || stmt instanceof NonCondStmtNode) {
            out.print(prefix);
            stmt.printStmt(out,this);
            out.println();
            generate(out,prefix,stmt.nextStmt(),ce);
        } else if (stmt instanceof CondStmtNode) {
            out.print(prefix);
            out.print("if (");
            stmt.printStmt(out, this);
            out.println(") {");
            if (((CondStmtNode)stmt).trueNextStmt == null) {
                out.println("// *************** uncovered branch ***************");
            }
            generate(out,prefix+tab,((CondStmtNode)stmt).trueNextStmt,ce);
            out.print(prefix);
            out.println("} else {");
            if (((CondStmtNode)stmt).falseNextStmt == null) {
                out.println("// *************** uncovered branch ***************");
            }
            generate(out,prefix+tab,((CondStmtNode)stmt).falseNextStmt,ce);
            out.print(prefix);
            out.println("}");
        }
    }

    public void generate(PrintWriter out, CodingEngine ce) {
        if (isInline) return;
        out.print(tab);
        out.print("public static ");
        out.print(returnType);
        out.print(' ');
        out.print(functionName);
        out.print('(');
        for(int i=0; i<argumentNames.size();i++) {
            if (i>0) {
                out.print(", ");
            }
            out.print(argumentTypes.get(i));
            out.print(' ');
            out.print(argumentNames.get(i));
        }
        out.println(") {");
        generate(out,tab+tab,root.nextStmt(), ce);
        out.print(tab);
        out.println("}");

    }

//    private NonCondStmtNode root;
//    private String functionName;
//    private String returnType;
//    private List<String> argumentTypes;
//    private List<String> argumentNames;
//    private String sig;
//    private boolean isInline;

    public boolean isInline() {
        return isInline;
    }
}
