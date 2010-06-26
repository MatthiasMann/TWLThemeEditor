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

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Map;

/**
 *
 * @author Matthias Mann
 */
public final class FontData {

    private final Font javaFont;
    private final float size;
    private final int upem;
    private final IntMap<IntMap<Integer>> kerning;
    private final IntMap<Integer> charWidth;
    private final BitSet defined;
    private final String postScriptName;

    public String getName() {
        return postScriptName;
    }

    public String getFamilyName() {
        return javaFont.getFamily();
    }

    public float getSize() {
        return size;
    }

    public Font getJavaFont() {
        return javaFont;
    }

    public int[][] getKernings() {
        ArrayList<int[]> kernings = new ArrayList<int[]>();
        for(Map.Entry<Integer, IntMap<Integer>> from : kerning) {
            for(Map.Entry<Integer, Integer> to : from.getValue()) {
                int value = convertUnitToEm(to.getValue());
                if(value != 0) {
                    kernings.add(new int[] { from.getKey(), to.getKey(), value});
                }
            }
        }
        return kernings.toArray(new int[kernings.size()][]);
    }

    public int getAdvance(char c) {
        return convertUnitToEm(charWidth.get(c));
    }

    public int getNextCodepoint(int codepoint) {
        return defined.nextSetBit(codepoint + 1);
    }

    public HashSet<Character.UnicodeBlock> getDefinedBlocks() {
        HashSet<Character.UnicodeBlock> result = new HashSet<Character.UnicodeBlock>();
        int codepoint = -1;
        while((codepoint=getNextCodepoint(codepoint)) >= 0) {
            Character.UnicodeBlock block = Character.UnicodeBlock.of(codepoint);
            if(block != null) {
                result.add(block);
            }
        }
        return result;
    }
    
    private FontData(byte[] data, float size) throws IOException {
        this.size = size;
        try {
            TTFFile rawFont = new TTFFile();
            if (!rawFont.readFont(new FontFileReader(data))) {
                throw new IOException("Invalid font file");
            }

            Font font = Font.createFont(Font.TRUETYPE_FONT, new ByteArrayInputStream(data));
            
            upem = rawFont.getUPEM();
            kerning = rawFont.getKerning();
            defined = rawFont.getDefinedUnicodePoints();
            charWidth = rawFont.getUnicodeWidth();
            postScriptName = rawFont.getPostScriptName();

            String name = getName();
            System.err.println("Loaded: " + name + " (" + data.length + ")");
            
            int style = 0;
            int comma = name.indexOf(',');
            if (comma >= 0) {
                name = name.substring(comma + 1);
                if (name.indexOf("Bold") >= 0) {
                    style |= Font.BOLD;
                }
                if (name.indexOf("Italic") >= 0) {
                    style |= Font.ITALIC;
                }
            }

            javaFont = font.deriveFont(style, size);
        } catch (FontFormatException e) {
            throw (IOException)(new IOException("Failed to read font").initCause(e));
        }
    }

    private FontData(FontData src, float size, int style) {
        this.size = size;
        this.javaFont = src.javaFont.deriveFont(style, size);
        this.upem = src.upem;
        this.kerning = src.kerning;
        this.defined = src.defined;
        this.charWidth = src.charWidth;
        this.postScriptName = src.postScriptName;
    }

    public FontData deriveFont(float size) {
        return deriveFont(size, javaFont.getStyle());
    }

    public FontData deriveFont(float size, int style) {
        return new FontData(this, size, style);
    }

    public static FontData create(InputStream is, float size) throws IOException {
        return new FontData(readInputStream(is), size);
    }

    public static FontData create(URL url, float size) throws IOException {
        byte[] data;
        InputStream is = url.openStream();
        try {
            data = readInputStream(is);
        } finally {
            is.close();
        }
        return new FontData(data, size);
    }
    
    private static byte[] readInputStream(InputStream is) throws IOException {
        byte[] buffer = new byte[16384];
        int size = 0;
        int read;
        while((read=is.read(buffer, size, buffer.length - size)) > 0) {
            size += read;
            if(size == buffer.length) {
                byte[] tmp = new byte[buffer.length * 2];
                System.arraycopy(buffer, 0, tmp, 0, size);
                buffer = tmp;
            }
        }

        byte[] data = new byte[size];
        System.arraycopy(buffer, 0, data, 0, size);
        return data;
    }
    
    private int convertUnitToEm(int units) {
        return Math.round((units * size) / upem);
    }
}
