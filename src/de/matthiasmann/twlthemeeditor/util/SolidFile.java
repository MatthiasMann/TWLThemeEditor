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

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Matthias Mann
 */
public class SolidFile extends URLStreamHandler {

    private File file;
    private Thread shutdownHook;
    private final RandomAccessFile raf;
    private final HashMap<String, Entry> entries;
    private final byte[] copyBuffer;

    public SolidFile(File file) throws FileNotFoundException {
        this.file = file;
        this.raf = new RandomAccessFile(file, "rw");
        this.entries = new HashMap<String, Entry>();
        this.copyBuffer = new byte[4096];

        shutdownHook = new Thread() {
            @Override
            public void run() {
                shutdownHook = null;    // don't try to remove it !
                try {
                    SolidFile.this.close();
                } catch(IOException ignore) {
                }
            }
        };

        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    public void addEntry(String name, InputStream is) throws IOException {
        long pos = raf.getFilePointer();
        long size = 0;
        int read;
        while((read=is.read(copyBuffer)) > 0) {
            raf.write(copyBuffer, 0, read);
            size += read;
        }
        Entry entry = new Entry(pos, size);
        Entry prev = entries.get(name);
        if(prev != null) {
            while(prev.next != null) {
                prev = prev.next;
            }
            prev.next = entry;
        } else {
            entries.put(name, entry);
        }
    }

    public Entry getEntry(String name) {
        return entries.get(name);
    }

    public byte[] readEntry(Entry entry) throws IOException {
        byte[] result = new byte[(int)entry.size];
        synchronized(raf) {
            raf.seek(entry.offset);
            int pos = 0;
            while(pos < result.length) {
                int read = raf.read(result, pos, result.length - pos);
                if(read <= 0) {
                    throw new EOFException();
                }
                pos += read;
            }
        }
        return result;
    }

    @Override
    protected URLConnection openConnection(URL u) throws IOException {
        return new URLConnection(u) {
            Entry entry;

            @Override
            public void connect() throws IOException {
                if(entry == null) {
                    entry = openEntry();
                }
            }

            private Entry openEntry() throws IOException {
                String path = url.getPath();
                if(!path.startsWith("/")) {
                    throw new FileNotFoundException(path);
                }
                Entry e = getEntry(path.substring(1));
                int idx  = url.getPort();
                for(; idx > 0 && e != null ; idx--) {
                    e = e.next;
                }
                if(idx != 0 || e == null) {
                    throw new FileNotFoundException(path);
                }
                return e;
            }
            
            @Override
            public int getContentLength() {
                if(entry == null) {
                    return -1;
                }
                return (int)Math.min(Integer.MAX_VALUE, entry.size);
            }

            @Override
            public InputStream getInputStream() throws IOException {
                connect();
                return SolidFile.this.getInputStream(entry);
            }
        };
    }

    public URL makeURL(final String name, final int idx) {
        return AccessController.doPrivileged(new PrivilegedAction<URL>() {
            public URL run() {
                try {
                    return new URL("solid", "localhost", idx, "/".concat(name), SolidFile.this);
                } catch(MalformedURLException ex) {
                    Logger.getLogger(SolidFile.class.getName()).log(Level.SEVERE, "Can't create URL", ex);
                    return null;
                }
            }
        });
    }

    public InputStream getInputStream(final Entry entry) {
        return new InputStream() {
            long pos;
            long mark;

            @Override
            public int read() throws IOException {
                byte[] b = new byte[1];
                if(read(b, 0, 1) != 1) {
                    return -1;
                }
                return b[0] & 255;
            }

            @Override
            public int available() throws IOException {
                return (int)Math.min(Integer.MAX_VALUE, entry.size - pos);
            }

            @Override
            public synchronized void mark(int readlimit) {
                mark = pos;
            }

            @Override
            public boolean markSupported() {
                return true;
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                if(len <= 0) {
                    return 0;
                }
                int avail = available();
                if(avail == 0) {
                    return -1;
                }
                if(len > avail) {
                    len = avail;
                }
                int read;
                synchronized(raf) {
                    raf.seek(entry.offset + pos);
                    read = raf.read(b, off, len);
                }
                if(read > 0) {
                    pos += read;
                }
                return read;
            }

            @Override
            public synchronized void reset() throws IOException {
                pos = mark;
            }

            @Override
            public long skip(long n) throws IOException {
                if(n <= 0) {
                    return 0;
                }
                long avail = entry.size - pos;
                if(n > avail) {
                    n = avail;
                }
                pos += n;
                return n;
            }
        };
    }

    public void close() throws IOException {
        synchronized(raf) {
            raf.close();
        }
        if(file != null) {
            file.delete();
            file = null;
        }
        if(shutdownHook != null) {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
            shutdownHook = null;
        }
    }

    public static class Entry {
        final long offset;
        final long size;
        Entry next;

        public Entry(long offset, long size) {
            this.offset = offset;
            this.size = size;
        }

        public Entry getNext() {
            return next;
        }
    }
}
