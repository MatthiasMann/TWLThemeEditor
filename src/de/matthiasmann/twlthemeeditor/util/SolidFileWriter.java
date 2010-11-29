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

import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author Matthias Mann
 */
public class SolidFileWriter {

    SolidFile solidFile;
    final byte[] buffer;
    int bufferPosition;
    long filePosition;

    public SolidFileWriter() throws IOException {
        this.solidFile = new SolidFile();
        buffer = new byte[65536];
        filePosition = 0;
    }

    public void addEntry(String name, InputStream is) throws IOException {
        ensureOpen();
        long pos = getFilePosition();
        long size = 0;
        int read;
        while((read=is.read(buffer, bufferPosition, buffer.length - bufferPosition)) > 0) {
            bufferPosition += read;
            if(bufferPosition*3 > buffer.length*2) {
                flush();
            }
            size += read;
        }
        solidFile.addEntry(name, pos, size);
    }

    public void addEntry(String name, byte[] data, int off, int size) throws IOException {
        ensureOpen();
        long pos = getFilePosition();
        if(buffer.length - bufferPosition >= size) {
            System.arraycopy(data, off, buffer, bufferPosition, size);
            bufferPosition += size;
        } else {
            flush();
            solidFile.raf.write(data, off, size);
            filePosition += size;
        }
        solidFile.addEntry(name, pos, size);
    }

    public SolidFile finish() throws IOException {
        ensureOpen();
        flush();
        SolidFile sf = solidFile;
        solidFile = null;
        return sf;
    }

    public void close() throws IOException {
        if(solidFile != null) {
            try {
                solidFile.close();
            } finally {
                solidFile = null;
            }
        }
    }

    private void ensureOpen() {
        if(solidFile == null) {
            throw new IllegalStateException("already finished");
        }
    }

    private long getFilePosition() {
        return filePosition + bufferPosition;
    }

    private void flush() throws IOException {
        if(bufferPosition > 0) {
            solidFile.raf.write(buffer, 0, bufferPosition);
            filePosition += bufferPosition;
            bufferPosition = 0;
        }
    }
}
