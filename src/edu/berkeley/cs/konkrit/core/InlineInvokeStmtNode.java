package edu.berkeley.cs.konkrit.core;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
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
public class InlineInvokeStmtNode extends NonCondStmtNode {
    protected InlineInvokeStmtNode(String stmt) {
        super(stmt);
        setStmt(stmt.substring(1).trim());
    }

    @Override
    public String toString() {
        return "@"+getStmt();
    }

    public String[] getComponents() {
        LinkedList<String> ret = new LinkedList<String>();
        Pattern p = Pattern.compile("(\\w+)\\s*\\(([^\\)]*)\\).*");
        Matcher m = p.matcher(getStmt());
        m.find();
        String fname  = m.group(1);
        ret.add(fname);
        String arguments = m.group(2);
        String args[] = arguments.split("\\,");
        for(String arg: args){
            arg = arg.trim();
            ret.add(arg);
        }
        String[] rets = new String[ret.size()];
        int i = 0;
        for(String s:ret) {
            rets[i] = s;
            i++;
        }
        return rets;
    }

    @Override
    public void printStmt(PrintWriter out, Function fun) {
        out.print(getStmt());
    }

}
