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
package de.matthiasmann.twlthemeeditor.gui;

import de.matthiasmann.twlthemeeditor.Main;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;

/**
 *
 * @author Matthias Mann
 */
public class NewProjectSettings {

    private final File folder;
    private final String projectName;

    public NewProjectSettings(File folder, String projectName) {
        this.folder = folder;
        this.projectName = projectName;
    }

    public File getFolder() {
        return folder;
    }

    public String getProjectName() {
        return projectName;
    }

    @Override
    public String toString() {
        return getFile(projectName).getPath();
    }

    private static final String BLUEPRINT_FILENAME = "new_project_blueprint.xml";
    private static final String[] FONT_FILENAMES = {"font.fnt", "font_00.png"};

    public File[] getFileList() {
        ArrayList<File> files = new ArrayList<File>();
        files.add(getFile(projectName));
        for(String fileName : FONT_FILENAMES) {
            files.add(getFile(fileName));
        }
        return files.toArray(new File[files.size()]);
    }

    public File createProject() throws IOException {
        File projectFile = getFile(projectName);

        copy(BLUEPRINT_FILENAME, projectFile);
        for(String fileName : FONT_FILENAMES) {
            copy(fileName, getFile(fileName));
        }

        return projectFile;
    }

    private File getFile(String name) {
        return new File(folder, name);
    }

    private URL getResource(String name) {
        return Main.class.getResource(name);
    }

    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[4096];
        int read;
        while((read=in.read(buffer)) > 0) {
            out.write(buffer, 0, read);
        }
    }

    private static void copy(URL src, File dst) throws IOException {
        InputStream in = src.openStream();
        try {
            FileOutputStream out = new FileOutputStream(dst);
            try {
                copy(in, out);
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }

    private void copy(String srcName, File dst) throws IOException {
        URL src = getResource(srcName);
        if(src == null) {
            throw new IOException("Can't locate resource: " + srcName);
        }
        copy(src, dst);
    }
}
