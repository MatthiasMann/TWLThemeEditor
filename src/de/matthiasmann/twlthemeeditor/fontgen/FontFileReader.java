/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* $Id: FontFileReader.java 489450 2006-12-21 19:54:40Z spepping $ */
 
package de.matthiasmann.twlthemeeditor.fontgen;

import java.io.IOException;

/**
 * Reads a TrueType font file into a byte array and
 * provides file like functions for array access.
 */
public class FontFileReader {
	/** The size of the file */
    private int fsize;      // file size
    /** The current position in the file */
    private int current;    // current position in file
    /** The file contents */
    private byte[] file;

    public FontFileReader(byte[] data) throws IOException {
    	file = data;
        this.fsize = this.file.length;
        this.current = 0;
    }

    /**
     * Set current file position to offset
     *
     * @param offset The new offset to set
     * @throws IOException In case of an I/O problem
     */
    public void seekSet(int offset) throws IOException {
        if (offset >= fsize || offset < 0) {
            throw new java.io.EOFException("Reached EOF, file size=" + fsize + " offset=" + offset);
        }
        current = offset;
    }

    /**
     * Skip a given number of bytes.
     *
     * @param add The number of bytes to advance
     * @throws IOException In case of an I/O problem
     */
    public void skip(int add) throws IOException {
        seekSet(current + add);
    }

    /**
     * Returns current file position.
     *
     * @return int The current position.
     */
    public int getCurrentPos() {
        return current;
    }

    /**
     * Returns the size of the file.
     *
     * @return int The filesize
     */
    public int getFileSize() {
        return fsize;
    }

    /**
     * Read 1 signed byte.
     *
     * @return One byte
     * @throws IOException If EOF is reached
     */
    public final byte readTTFByte() throws IOException {
        if (current >= fsize) {
            throw new java.io.EOFException("Reached EOF, file size=" + fsize);
        }

        return file[current++];
    }

    /**
     * Read 1 unsigned byte.
     *
     * @return One unsigned byte
     * @throws IOException If EOF is reached
     */
    public final int readTTFUByte() throws IOException {
        return readTTFByte() & 0xFF;
    }

    /**
     * Read 2 bytes signed.
     *
     * @return One signed short
     * @throws IOException If EOF is reached
     */
    public final short readTTFShort() throws IOException {
        final int ret = (readTTFUByte() << 8) + readTTFUByte();
        final short sret = (short)ret;
        return sret;
    }

    /**
     * Read 2 bytes unsigned.
     *
     * @return One unsigned short
     * @throws IOException If EOF is reached
     */
    public final int readTTFUShort() throws IOException {
        final int ret = (readTTFUByte() << 8) + readTTFUByte();
        return ret;
    }

    /**
     * Read 4 bytes.
     *
     * @return One signed integer
     * @throws IOException If EOF is reached
     */
    public final int readTTFLong() throws IOException {
        int ret = readTTFUByte();    // << 8;
        ret = (ret << 8) + readTTFUByte();
        ret = (ret << 8) + readTTFUByte();
        ret = (ret << 8) + readTTFUByte();
        return ret;
    }

    /**
     * Read an ISO-8859-1 string of len bytes.
     *
     * @param len The length of the string to read
     * @return A String
     * @throws IOException If EOF is reached
     */
    public final String readTTFString(int len) throws IOException {
        if ((len + current) > fsize) {
            throw new java.io.EOFException("Reached EOF, file size=" + fsize);
        }

        byte[] tmp = new byte[len];
        System.arraycopy(file, current, tmp, 0, len);
        current += len;
        final String encoding;
        if ((tmp.length > 0) && (tmp[0] == 0)) {
            encoding = "UTF-16BE";
        } else {
            encoding = "ISO-8859-1";
        }
        return new String(tmp, encoding);
    }
}