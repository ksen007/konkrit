package edu.berkeley.cs.konkrit.core;

import bsh.NameSpace;
import bsh.Primitive;
import bsh.UtilEvalError;

import java.util.Stack;

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
public class CallStack {
    StmtNode currentStmt;
    private Function currentFunction;
    private Stack<Function> functionStack;
    private Stack<StmtNode> stmtStack;
    private Stack<NameSpace> nameSpaceStack;

    public CallStack() {
        currentStmt = new NonCondStmtNode("** Begin **");
        currentFunction = null;
        functionStack = new Stack<Function>();
        stmtStack = new Stack<StmtNode>();
        nameSpaceStack = new Stack<NameSpace>();
    }

    public void reset(NameSpace nameSpace) {
        nameSpaceStack.clear();
        stmtStack.clear();
        functionStack.clear();

        currentStmt = new NonCondStmtNode("** Begin **");
        currentFunction = null;
        nameSpaceStack.push(nameSpace);

    }

    public void push(Function fun, NameSpace oldNameSpace) {
        functionStack.push(currentFunction);
        currentFunction = fun;
        stmtStack.push(currentStmt);
        currentStmt = currentFunction.getRoot();

        if (!fun.isInline()) {
            NameSpace newNameSpace = new NameSpace(oldNameSpace,fun.getFunctionName());
            nameSpaceStack.push(newNameSpace);
        }

    }

    public boolean pop() throws UtilEvalError {
        if(!currentFunction.isInline()) {
            nameSpaceStack.pop();
        }
        if (stmtStack.peek() instanceof InvokeAssignStmtNode) {
            Object returnVal;
            returnVal = (currentStmt.lastValue==null)? Primitive.NULL:currentStmt.lastValue;
            nameSpaceStack.peek().setVariable(((InvokeAssignStmtNode)stmtStack.peek()).lhs,returnVal,true);
        }
        boolean flag = currentFunction.isInline() && currentFunction.getFunctionName().equals(functionStack.peek().getFunctionName());
        currentFunction = functionStack.pop();
        currentStmt = stmtStack.pop();
        return flag;

    }

    public int size() {
        return functionStack.size();
    }

    public String getPrompt() {
        StringBuilder sb = new StringBuilder("%");
        for (Function fun:functionStack) {
            if (fun!=null) {
                String fName = fun.getFunctionName();
                sb.append(fName);
                sb.append(":");
            }
        }
        if (currentFunction !=null)
            sb.append(currentFunction.getSig());
        sb.append("%");
        return sb.toString();
    }

    public boolean isEmpty() {
        return functionStack.isEmpty();
    }

    public NameSpace getNameSpace() {
        return nameSpaceStack.peek();
    }
}
