package edu.berkeley.cs.tedd.core;

import bsh.EvalError;
import bsh.UtilEvalError;
import jline.ConsoleReader;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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

public class Commands {

    private CodingEngine ce;

    @Command(": Execute next statement. Step over any declared function call in the statement.")
    public void next(String arg) throws EvalError, TeddError, UtilEvalError {
        ce.next();
        ce.changeWatchList();
    }

    @Command(": Execute next statement. Step into any declared function call in the statement.")
    public void step(String arg) throws EvalError, TeddError, UtilEvalError {
        ce.step();
        ce.changeWatchList();
    }

    @Command(": Rollback execution to the previous statement.")
    public void previous(String arg) throws EvalError, TeddError, UtilEvalError {
        ce.previous();
        ce.changeWatchList();
    }

    @Command(": Execute all the remaining statements.")
    public void cont(String arg) throws EvalError, TeddError, UtilEvalError {
        ce.Continue();
        ce.changeWatchList();
    }

//    @Command(": Restart the execution and execute all the statements.")
//    public void rerun() throws EvalError, TeddError, UtilEvalError {
//        rerun("-1");
//    }
//
    @Command("n: Restart the execution and execute n statements. Example \"rerun 0\"")
    public void rerun(String arg) throws EvalError, TeddError, UtilEvalError {
        if (arg.length()==0) arg = "-1";
        int n = 0;
        try {
            n = Integer.parseInt(arg);
        } catch (NumberFormatException e) {
            n = 0;
        }
        ce.rerun(n);
        ce.changeWatchList();
    }

    @Command("sig: Declare a new function to be defined. Example \"declare int foo(String bar, int v)\"")
    public void declare(String sig) throws TeddError {
        ce.declareFunction(sig);
    }

    @Command("function-name: Undeclare the function function-name. Example \"undeclare foo\"")
    public void undeclare(String fname) throws TeddError {
        ce.unDeclareFunction(fname);
    }

    @Command(": list all declared function names.")
    public void list(String fname) {
        ce.list();
    }

    @Command(": Remove next statement.")
    public void remove(String arg) throws TeddError {
        ce.remove();
    }

    @Command("statement: Insert and execute the next statement.")
    public void insert(String arg) throws EvalError, TeddError, UtilEvalError {
        ce.insert(arg);
        ce.step();
        ce.changeWatchList();
    }

    @Command("statement: Replace the next statement.")
    public void replace (String arg) throws TeddError {
        ce.replace(arg);
    }

    public void add(String arg) throws TeddError, EvalError, UtilEvalError {
        ce.add(arg);
        ce.step();
        ce.changeWatchList();
    }

    @Command("statement: Execute the statement, but do not add or insert the statement in the current function.")
    public void execute(String arg) throws EvalError {
        ce.execute(arg);
    }


    @Command(": print the statements executed so far in the current test.")
    public void print(String arg) {
        ce.print();
    }

    @Command("test-name: Declare and start executing the test function \"void test-name()\".")
    public void test(String arg) throws EvalError, TeddError, UtilEvalError {
        if (arg.length()==0) {
            throw new TeddError("test-name is not provided.");
        } else {
            try {
            ce.declareFunction("void "+arg+"();");
            } catch (TeddError te) {
                System.out.println(te);
            }
            ce.editFunction(arg+"();");
        }
    }

    @Command(": Test all tests declared so far.")
    public void all(String arg) throws EvalError, TeddError {
        ce.testAll();
    }

    @Command("expression: Print the value of the expression after every command.")
    public void watch(String arg) {
        ce.watch(arg);
    }

    @Command(": Save the current program.")
    public void save(String arg) throws IOException, TeddError {
        ce.save();
    }

    @Command("file-name: Save the current program in the file-name.")
    public void saveas(String arg) throws IOException, TeddError {
        ce.saveas(arg);
    }

    @Command("file-name: Open the file \"file-name\" as the current program.")
    public void open(String arg) throws IOException, ClassNotFoundException {
        ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(arg+".tedd")));
        ce = (CodingEngine) in.readObject();
        ce.setCurrentClass(arg);
        System.out.println("Loaded class "+arg);
    }

    @Command("mock-name: Create and execute the mock mock-name.")
    public void mock(String arg) throws TeddError, EvalError, UtilEvalError {
        if (arg.length()==0) {
            ce.setMock(null);
        } else {
            ce.setMock(arg);
            ce.step();
        }
    }


    @Command(": print usage of this tool.")
    public void usage(String arg) {
        System.out.println("Usage:");
        System.out.println("\tstatement: add the statement as the next statement and execute.");
        for (Method m : getClass().getMethods()) {
            if (m.isAnnotationPresent(Command.class)) {
                try {
                    Command annot = m.getAnnotation(Command.class);
                    if (annot != null) {
                        System.out.println("\t!"+m.getName()+" "+annot.value());
                    }
                } catch (Throwable ex) {
                    System.err.println(ex);
                    ex.printStackTrace();
                }
            }
        }

    }

    public void  processCommand() throws IOException {
        ConsoleReader br = new ConsoleReader();
        String line;
        ce = new CodingEngine();

        System.out.println(ce.getPrompt());
        while((line = br.readLine())!=null) {
            String command, rest = "";
            if (line.startsWith("!")) {
                line = line.substring(1);
                int idx = line.indexOf(' ');
                if (idx==-1) {
                    command = line.trim();
                } else {
                    command = line.substring(0,idx).trim();
                    rest = line.substring(idx).trim();
                }
            } else {
                command = null;
                rest = line.trim();
            }
            try {
                if (command==null) {
                    if (rest.length()>0)
                        add(rest);
                } else {
                    boolean isFound = false;
                    for (Method m : getClass().getMethods()) {
                        if (m.isAnnotationPresent(Command.class)) {
                            if (m.getName().startsWith(command)) {
                                Object[] args = new Object[1];
                                args[0] = rest;
                                isFound = true;
                                try {
                                    m.invoke(this,args);
                                } catch (IllegalAccessException e) {
                                    System.err.println("-----------------------------------");
                                    System.err.println(e);
                                    e.printStackTrace();
                                } catch (InvocationTargetException e) {
                                    throw e.getTargetException();
                                }

                                break;
                            }
                        }
                    }
                    if (!isFound) {
                        System.out.println("Command not found: !"+line);
                    }
                }
            } catch (TeddError te) {
                System.err.println(te);
            } catch (Throwable e) {
                System.err.println("-----------------------------------");
                System.err.println(e);
                e.printStackTrace();
            }
            ce.executeWatchList();
            System.out.println(ce.getPrompt());

        }

        System.out.println("Goodbye!");
        
        List hist = br.getHistory().getHistoryList();
        for(Object command:hist){
            System.out.println(command);
        }
    }


    public static void main(String[] args) throws IOException {
        Commands cmds = new Commands();
        System.out.println("type !usage to print usage of this tool.");
        cmds.processCommand();
        System.exit(0);
    }
}
