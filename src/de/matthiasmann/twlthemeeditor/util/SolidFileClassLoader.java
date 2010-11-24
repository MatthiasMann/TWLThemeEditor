/*
 * Copyright (c) 2008-2010, Matthias Mann
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
package de.matthiasmann.twlthemeeditor.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 *
 * @author Matthias Mann
 */
public class SolidFileClassLoader extends ClassLoader {

    private final SolidFile solidFile;

    public SolidFileClassLoader(ClassLoader parent, SolidFile solidFile) {
        super(parent);
        this.solidFile = solidFile;
    }

    public void close() {
        try {
            solidFile.close();
        } catch(IOException ex) {
            getLogger().log(Level.SEVERE, "Can't close SolidFile", ex);
        }
    }

    @Override
    protected URL findResource(String name) {
        if(solidFile.getEntry(name) != null) {
            return solidFile.makeURL(name, 0);
        }
        return null;
    }

    @Override
    protected Enumeration<URL> findResources(String name) throws IOException {
        ArrayList<URL> list = Collections.list(getParent().getResources(name));
        SolidFile.Entry entry = solidFile.getEntry(name);
        for(int idx=0 ; entry != null ; idx++,entry=entry.getNext()) {
            URL url = solidFile.makeURL(name, idx);
            if(url != null) {
                list.add(url);
            }
        }
        return Collections.enumeration(list);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        String entryName = name.replace('.', '/') + ".class";
        SolidFile.Entry entry = solidFile.getEntry(entryName);
        if(entry != null) {
            try {
                byte[] tmp = solidFile.readEntry(entry);
                return defineClass(name, tmp, 0, tmp.length);
            } catch(IOException ex) {
                throw new ClassNotFoundException(name, ex);
            }
        }
        throw new ClassNotFoundException(name);
    }

    public static SolidFileClassLoader create(ClassLoader parent, File ... roots) throws IOException {
        SolidFile solidFile = new SolidFile(File.createTempFile("sfcl", ".bin"));

        for(File root : roots) {
            try {
                if(root.isDirectory()) {
                    collectFolder(solidFile, root, "");
                } else {
                    collectJAR(solidFile, root);
                }
            } catch(IOException ex) {
                getLogger().log(Level.SEVERE, "Can't process root: " + root, ex);
            }
        }

        return new SolidFileClassLoader(parent, solidFile);
    }

    private static void collectFolder(SolidFile solidFile, File folder, String path) {
        for(File file : folder.listFiles()) {
            if(file.isDirectory()) {
                collectFolder(solidFile, file, path + file.getName() + "/");
            } else {
                try {
                    FileInputStream fis = new FileInputStream(file);
                    try {
                        solidFile.addEntry(path.concat(file.getName()), fis);
                    } finally {
                        fis.close();
                    }
                } catch (IOException ex) {
                    getLogger().log(Level.SEVERE, "Can't read file", ex);
                }
            }
        }
    }

    private static void collectJAR(SolidFile solidFile, File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        try {
            ZipInputStream zis = new ZipInputStream(fis);
            try {
                ZipEntry zipEntry;
                while((zipEntry=zis.getNextEntry()) != null) {
                    if(!zipEntry.isDirectory()) {
                        solidFile.addEntry(zipEntry.getName(), zis);
                    }
                }
            } finally {
                zis.close();
            }
        } finally {
            fis.close();
        }
    }

    private static Logger getLogger() {
        return Logger.getLogger(SolidFileClassLoader.class.getName());
    }
}
