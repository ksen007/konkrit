package edu.berkeley.cs.tedd;
import bsh.EvalError;
import bsh.Interpreter;

import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: ksen
 * Date: 7/5/11
 * Time: 3:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class BshTest {
    public static void main(String[] args) throws EvalError {

        Interpreter i = new Interpreter();  // Construct an interpreter
        i.set("foo", 5);                    // Set variables
        i.set("date", new Date() );

        Date date = (Date)i.get("date");    // retrieve a variable

        i.eval("bar = foo*10");
        System.out.println( i.get("bar") );


    }

}
