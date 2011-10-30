package edu.berkeley.cs.konkrit.core;

import bsh.EvalError;
import bsh.Interpreter;
import bsh.NameSpace;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;

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
public class CondStmtNode extends StmtNode {
    StmtNode falseNextStmt;
    StmtNode trueNextStmt;
    //private boolean lastOutcome;



    private void writeObject(java.io.ObjectOutputStream out)
        throws IOException {
        out.writeObject(getStmt());
        out.writeObject(falseNextStmt);
        out.writeObject(trueNextStmt);
    }
    
    private void readObject(java.io.ObjectInputStream in)
        throws IOException, ClassNotFoundException {
        lastValue = true;
        setStmt((String) in.readObject());
        falseNextStmt = (StmtNode)in.readObject();
        if (falseNextStmt !=null) falseNextStmt.setParent(this);
        trueNextStmt = (StmtNode)in.readObject();
        if (trueNextStmt !=null) trueNextStmt.setParent(this);
    }

    protected CondStmtNode(String stmt) {
        super(stmt);
        lastValue = true;
    }

    @Override
    public StmtNode nextStmt() {
        return ((Boolean)lastValue)?trueNextStmt:falseNextStmt;
    }

    @Override
    protected void setNextStmt(StmtNode child) {
        if ((Boolean)lastValue)
            trueNextStmt = child;
        else
            falseNextStmt = child;
        if(child!=null) {
            child.setParent(this);
        }
    }

    @Override
    public String toString() {
        return ((Boolean)lastValue)?"since "+getStmt():"since !("+getStmt()+")";
    }

    @Override
    public void printStmt(PrintWriter out, Function fun) {
        int idx;
        idx = getStmt().lastIndexOf(';');
        String ret = getStmt();
        if (idx >=0) {
            ret = getStmt().substring(0, idx);
        }
        out.print(ret);
    }
}
