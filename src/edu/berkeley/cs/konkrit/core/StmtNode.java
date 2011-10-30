package edu.berkeley.cs.tedd.core;

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
public abstract class StmtNode  implements Serializable {
    private String stmt;
    StmtNode parent;
    Object lastValue;

    private void writeObject(java.io.ObjectOutputStream out)
        throws IOException {
    }

    private void readObject(java.io.ObjectInputStream in)
        throws IOException, ClassNotFoundException {
    }


    public static StmtNode createNode(String stmt) {
        StmtNode ret = null;
        if (stmt.startsWith("since ")) {
            stmt = stmt.substring(6).trim();
            ret = new CondStmtNode(stmt);
        } else if (stmt.matches("\\s*\\w+\\s*=\\s*\\w+\\s*\\(.*")){
            return new InvokeAssignStmtNode(stmt);
        } else if (stmt.matches("\\s*@\\w+\\s*\\(.*")){
            return new InlineInvokeStmtNode(stmt);
        } else {
            ret = new NonCondStmtNode(stmt);
        }
        return ret;
    }

    protected StmtNode(String stmt) {
        setStmt(stmt);
    }

    public void setStmt(String stmt) {
        if (!stmt.endsWith(";")) {
            stmt = stmt+";";
        }
        this.stmt = stmt;
    }

    public String getStmt() {
        return stmt;
    }

    public void setParent(StmtNode parent) {
        this.parent = parent;
    }

    abstract public StmtNode nextStmt();

    abstract protected void setNextStmt(StmtNode child);

    private StmtNode step(Interpreter intp, NameSpace ns) throws EvalError {
        lastValue = intp.eval(getStmt(),ns);
        return nextStmt();
    }

    final public StmtNode forward(Interpreter intp, NameSpace ns) throws EvalError, TeddError {
        StmtNode ret = nextStmt();
        if (ret==null) {
            throw new TeddError("No more statement to execute.");
        }
        ret.step(intp, ns);
        return ret;
    }

    final public StmtNode insert(String stmt) {
        StmtNode ret = createNode(stmt);
        ret.setNextStmt(nextStmt());
        setNextStmt(ret);
        return ret;
    }

    final public StmtNode remove() throws TeddError {
        StmtNode ret = nextStmt();
        if (ret != null) {
            setNextStmt(ret.nextStmt());
        } else {
            throw new TeddError("There is no next statement.  Nothing is removed.");
        }
        return ret;
    }

    final public StmtNode replace(String stmt) throws TeddError {
        remove();
        return insert(stmt);
//        StmtNode ret = nextStmt();
//        if (stmt.startsWith("since ")) {
//            if (ret instanceof CondStmtNode) {
//                stmt = stmt.substring(6).trim();
//                ret.setStmt(stmt);
//            } else {
//                throw new TeddError("Cannot replace a regular statement with a \"since\" statement.");
//            }
//        } else {
//             if (ret instanceof NonCondStmtNode) {
//                 ret.setStmt(stmt);
//             } else {
//                 throw new TeddError("Cannot replace a \"since\" statement with a regular statement.");
//             }
//        }
//        return ret;
    }

    public void printStmt(PrintWriter out, Function fun) {
        if (isReturn() && fun.isInline())
            out.print("break "+fun.getFunctionName()+";");
        else {
            String ret = getStmt().replace("Assert.check", "assert ");
            out.print(ret);
        }
    }

    final public boolean isReturn() {
        return getStmt().matches("return\\W.*");
    }
}
