package edu.berkeley.cs.konkrit.core;

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
import java.awt.*;
import java.io.*;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.text.Document;

public class Console extends JFrame {
    private JTextArea textArea = new JTextArea();
    private JTextArea watchArea1 = new JTextArea();
    private JTextArea watchArea2 = new JTextArea();
    private int rest;

    public Console(){
        rest = 0;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        textArea.setEditable(false);
        textArea.setRows(32);
        textArea.setColumns(50);
        getContentPane().add(new JScrollPane(textArea), BorderLayout.CENTER);
        watchArea1.setEditable(false);
        watchArea1.setRows(5);
        watchArea1.setColumns(25);
        watchArea1.setBorder(BorderFactory.createTitledBorder("Previous Watch List"));
        watchArea2.setEditable(false);
        watchArea2.setRows(5);
        watchArea2.setColumns(25);
        watchArea2.setBorder(BorderFactory.createTitledBorder("Current Watch List"));


        FlowLayout layout = new FlowLayout();
        JPanel south = new JPanel(layout);
        getContentPane().add(south, BorderLayout.SOUTH);

        south.add(new JScrollPane(watchArea1));
        south.add(new JScrollPane(watchArea2));

        pack();
        setVisible(true);
    }

    public void addText(StatementTextWithDepth line) {
        addText(line.toString());
    }

    public void clear() {
        textArea.setText("");
        rest = 0;
    }

    public void removeTail() {
        int end = textArea.getDocument().getLength();
        if (rest <= end) {
            textArea.replaceRange("",rest,end);
        }
    }

    public void addTail(StatementTextWithDepth line) {
        addTail(line.toString());
    }

    public void addText(String s) {
        textArea.append(s);
        textArea.append("\n");
        rest = textArea.getDocument().getLength();
        textArea.setCaretPosition(rest);
    }

    public void addTail(String s) {
        textArea.append(s);
        textArea.append("\n");
        textArea.setCaretPosition(textArea.getDocument().getLength());
    }

    public void changeWatch() {
        try {
            Document doc = watchArea2.getDocument();
            watchArea1.setText(doc.getText(0,doc.getLength()));
            watchArea2.setText("");
        } catch(Exception e) {
            System.err.println(e);
            e.printStackTrace();
        }
    }

    public void clearWatch() {
        watchArea2.setText("");
    }

    public void addWatchText(String s) {
        watchArea2.append(s+"\n");
    }
}
