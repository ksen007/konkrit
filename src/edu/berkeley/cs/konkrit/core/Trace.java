package edu.berkeley.cs.tedd.core;

import javax.swing.text.Document;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

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
public class Trace {
    private List<StatementTextWithDepth> statementHistory;
    private String currentTest;
    private Console console;
    private ArrayList<String> mocks;

    public Trace(Console console) {
        statementHistory = new LinkedList<StatementTextWithDepth>();
        this.console = console;
    }

    public void clear() {
        statementHistory.clear();
        if (console != null)
            console.clear();
    }

    public boolean isEmpty() {
        return statementHistory.isEmpty();
    }

    public StatementTextWithDepth add(int depth, StmtNode currentStmt ) {
        StatementTextWithDepth ret = new StatementTextWithDepth(depth,
                statementHistory.size()+1,currentStmt.toString());
        if (console != null) {
            console.removeTail();
            console.addText(ret);
        }
        statementHistory.add(ret);

        return ret;
    }

    private StatementTextWithDepth getNextStmt(int depth, StmtNode currentStmt) {
        StatementTextWithDepth ret2;
        String stmt;

        String mock = getMock();
        if(mock!=null) {
            stmt = "mock "+mock;
        } else {
            StmtNode tmp = currentStmt.nextStmt();
            if (tmp!=null) {
                stmt = tmp.toString();
            } else {
                if (depth == 0)
                    stmt = "** end of execution **";
                else
                    stmt = "no statement";
            }
        }
        
        ret2 = new StatementTextWithDepth(depth, statementHistory.size()+1,stmt);
        return ret2;
    }

    public void updateNext(int depth, StmtNode currentStmt) {
        if (console != null) {
            console.removeTail();
            console.addTail("------------------------------------------->\n");
            console.addTail(getNextStmt(depth,currentStmt));
        }
    }

    public void setCurrentTest(String sig, ArrayList<String> mocks) {
        this.currentTest = sig;
        this.mocks = mocks;
        if (console != null) {
            StatementTextWithDepth ret = new StatementTextWithDepth(0,0,sig);
            console.addText(ret);
        }
    }

    public String getCurrentTest() {
        return currentTest;
    }

    public void print(int depth) {
        System.out.println(new StatementTextWithDepth(0,0,currentTest));
        for(StatementTextWithDepth stmt:statementHistory) {
            if (depth >= stmt.getDepth())
                System.out.println(stmt);
        }
    }

    public void printNext(int depth, StmtNode currentStmt) {
        System.out.println("------------------------------------------->");
        System.out.println(getNextStmt(depth,currentStmt).toString());
    }

    public void setMock(String mockName) {
        int n = statementHistory.size();
        mocks.set(n,mockName);
    }

    public String getMock() {
        int n = statementHistory.size();
        if (mocks.size()<=n) {
            mocks.add(null);
        }
        return mocks.get(n);
    }

    public int size() {
        return statementHistory.size();
    }
}
