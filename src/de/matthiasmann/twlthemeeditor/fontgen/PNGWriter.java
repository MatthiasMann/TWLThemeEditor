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
package de.matthiasmann.twlthemeeditor.fontgen;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

/**
 *
 * @author Matthias Mann
 */
public class PNGWriter {

    private static final byte[] SIGNATURE = {(byte)137, 80, 78, 71, 13, 10, 26, 10};
    private static final int IHDR = (int)0x49484452;
    private static final int IDAT = (int)0x49444154;
    private static final int IEND = (int)0x49454E44;
    private static final byte COLOR_TRUEALPHA = 6;
    private static final byte COMPRESSION_DEFLATE = 0;
    private static final byte FILTER_NONE = 0;
    private static final byte INTERLACE_NONE = 0;
    private static final byte PAETH = 4;

    public static void write(OutputStream os, BufferedImage image, int height) throws IOException {
        DataOutputStream dos = new DataOutputStream(os);
        dos.write(SIGNATURE);

        Chunk cIHDR = new Chunk(IHDR);
        cIHDR.writeInt(image.getWidth());
        cIHDR.writeInt(height);
        cIHDR.writeByte(8); // 8 bit per component
        cIHDR.writeByte(COLOR_TRUEALPHA);
        cIHDR.writeByte(COMPRESSION_DEFLATE);
        cIHDR.writeByte(FILTER_NONE);
        cIHDR.writeByte(INTERLACE_NONE);
        cIHDR.writeTo(dos);

        Chunk cIDAT = new Chunk(IDAT);
        DeflaterOutputStream dfos = new DeflaterOutputStream(
            cIDAT, new Deflater(Deflater.BEST_COMPRESSION));

        int lineLen = image.getWidth() * 4;
        byte[] lineOut = new byte[lineLen+1];
        byte[] curLine = new byte[lineLen];
        byte[] prevLine = new byte[lineLen];

        for(int line=0 ; line<height ; line++) {
            for(int x=0,width=image.getWidth() ; x<width ; x++) {
                int rgb = image.getRGB(x, line);
                curLine[x*4+0] = (byte)(rgb >>> 16);
                curLine[x*4+1] = (byte)(rgb >>>  8);
                curLine[x*4+2] = (byte)(rgb       );
                curLine[x*4+3] = (byte)(rgb >>> 24);
            }

            lineOut[0] = PAETH;
            lineOut[1] = (byte)(curLine[0] - prevLine[0]);
            lineOut[2] = (byte)(curLine[1] - prevLine[1]);
            lineOut[3] = (byte)(curLine[2] - prevLine[2]);
            lineOut[4] = (byte)(curLine[3] - prevLine[3]);

            for(int x=4 ; x<lineLen ; x++) {
                int a = curLine[x-4] & 255;
                int b = prevLine[x] & 255;
                int c = prevLine[x-4] & 255;
                int p = a + b - c;
                int pa = p - a; if(pa < 0) pa = -pa;
                int pb = p - b; if(pb < 0) pb = -pb;
                int pc = p - c; if(pc < 0) pc = -pc;
                if(pa<=pb && pa<=pc)
                    c = a;
                else if(pb<=pc)
                    c = b;
                lineOut[x+1] = (byte)(curLine[x] - c);
            }

            dfos.write(lineOut);

            // swap the line buffers
            byte[] temp = curLine;
            curLine = prevLine;
            prevLine = temp;
        }

        dfos.finish();
        cIDAT.writeTo(dos);
        Chunk cIEND = new Chunk(IEND);
        cIEND.writeTo(dos);

        dos.flush();
    }

    public static void write(File file, BufferedImage image, int height) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        try {
            write(fos, image, height);
        } finally {
            fos.close();
        }
    }

    static class Chunk extends DataOutputStream {
        final CRC32 crc;
        final ByteArrayOutputStream baos;

        Chunk(int chunkType) throws IOException {
            this(chunkType, new ByteArrayOutputStream(), new CRC32());
        }
        private Chunk(int chunkType, ByteArrayOutputStream baos,
                      CRC32 crc) throws IOException {
            super(new CheckedOutputStream(baos, crc));
            this.crc = crc;
            this.baos = baos;

            writeInt(chunkType);
        }

        public void writeTo(DataOutputStream out) throws IOException {
            flush();
            out.writeInt(baos.size() - 4);
            baos.writeTo(out);
            out.writeInt((int)crc.getValue());
        }
    }
}
