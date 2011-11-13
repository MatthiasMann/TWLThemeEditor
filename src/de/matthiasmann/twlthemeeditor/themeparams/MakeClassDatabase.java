/*
 * Copyright (c) 2008-2011, Matthias Mann
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of Matthias Mann nor the names of its contributors may
 *       be used to endorse or promote products derived from this software
 *       without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package de.matthiasmann.twlthemeeditor.themeparams;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.objectweb.asm.ClassReader;

/**
 *
 * @author Matthias Mann
 */
public class MakeClassDatabase {
    
    private File inputJARFile;
    private File databaseFile;

    public void setInputJARFile(File inputJARFile) {
        this.inputJARFile = inputJARFile;
    }

    public void setDatabaseFile(File databaseFile) {
        this.databaseFile = databaseFile;
    }
    
    public void execute() throws IOException {
        ClassDatabase db = new ClassDatabase(null);
        
        FileInputStream fis = new FileInputStream(inputJARFile);
        BufferedInputStream bis = new BufferedInputStream(fis);
        ZipInputStream jis = new ZipInputStream(bis);
        ZipEntry entry;
        while((entry=jis.getNextEntry()) != null) {
            if(entry.getName().endsWith(".class")) {
                try {
                    parse(jis, db);
                } catch(Exception ex) {
                    System.err.println("Could not parse " + entry.getName());
                    ex.printStackTrace();
                }
            }
        }
        
        FileOutputStream fos = new FileOutputStream(databaseFile);
        try {
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            try {
                ObjectOutputStream oos = new ObjectOutputStream(bos);
                try {
                    oos.writeObject(db);
                } finally {
                    oos.flush();
                    oos.close();
                }
            } finally {
                bos.flush();
                bos.close();
            }
        } finally {
            fos.close();
        }
    }
    
    private static void parse(InputStream in, ClassDatabase db) throws IOException {
        ClassReader cr = new ClassReader(in);
        ClassAnalyzer ca = new ClassAnalyzer();
        cr.accept(ca, 0);
        db.add(ca.getClassInfo());
    }
}
