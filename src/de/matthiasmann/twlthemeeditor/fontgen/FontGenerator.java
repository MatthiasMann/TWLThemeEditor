/*
 * Copyright (ch) 2008-2010, Matthias Mann
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

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphMetrics;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

/**
 *
 * @author Matthias Mann
 */
public class FontGenerator {

    private final FontData fontData;

    private Padding padding;
    private BufferedImage image;
    private GlyphRect[] rects;
    private int ascent;
    private int descent;
    private int leading;
    private int usedTextureHeight;
    
    public FontGenerator(FontData fontData) {
        this.fontData = fontData;
    }

    public void generate(int width, int height, CharSet set, Padding padding, Effect[] effects) {
        this.padding = padding;
        
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);

        Font font = fontData.getJavaFont();
        g.setFont(font);

        FontRenderContext fontRenderContext = g.getFontRenderContext();

        ascent = g.getFontMetrics().getMaxAscent();
        descent = g.getFontMetrics().getMaxDescent();
        leading = g.getFontMetrics().getLeading();
        int maxHeight = ascent;

        //data = new DataSet(font.getName(), (int) font.getSize(), lineHeight, width, height, set.getName(), "font.png");

        ArrayList<GlyphRect> rectList = new ArrayList<GlyphRect>();
        char[] chBuffer = new char[1];

        int codepoint = -1;
        while((codepoint=fontData.getNextCodepoint(codepoint)) >= 0) {
            if (!set.isIncluded(codepoint)) {
                continue;
            }

            chBuffer[0] = (char)codepoint;

            final GlyphVector vector = font.layoutGlyphVector(fontRenderContext, chBuffer, 0, 1, Font.LAYOUT_LEFT_TO_RIGHT);
            final GlyphMetrics metrics = vector.getGlyphMetrics(0);
            
            int xoffset = 0;
            int lsb = (int)metrics.getLSB();
            int rsb = (int)metrics.getRSB();
            int advance = (int)metrics.getAdvanceX();
            int glyphWidth = advance + padding.left + padding.right + 1;
            int glyphHeight = vector.getGlyphVisualBounds(0).getBounds().height + 2 + padding.top + padding.bottom;
            int yoffset = vector.getPixelBounds(fontRenderContext, 0, 0).y - 1;

            if (lsb < 0) {
                xoffset = 1 - lsb;
                glyphWidth += xoffset;
            }
            if (rsb < 0) {
                glyphWidth -= rsb - 1;
            }

            GlyphRect rect = new GlyphRect(chBuffer[0],
                    glyphWidth, glyphHeight + 1,
                    advance + padding.advance, yoffset,
                    xoffset + padding.left + 1, padding.top,
                    vector.getGlyphOutline(0));

            maxHeight = Math.max(glyphHeight, maxHeight);

            rectList.add(rect);
        }

        FontInfo fontInfo = new FontInfo(maxHeight, descent);
        
        // sorting of arrays is more efficient then sorting of collections
        final int numGlyphs = rectList.size();

        rects = rectList.toArray(new GlyphRect[numGlyphs]);
        Arrays.sort(rects, new Comparator<GlyphRect>() {
            public int compare(GlyphRect a, GlyphRect b) {
                return b.height - a.height;
            }
        });

        for(Effect effect : effects) {
            effect.prePageRender(g, fontInfo);
        }

        g.setColor(Color.white);

        int xp = 0;
        int dir = 1;
        int[] usedY = new int[width];
        usedTextureHeight = 0;

        for (int i=0 ; i < numGlyphs ; i++) {
            final GlyphRect rect = rects[i];

            if (dir > 0) {
                if (xp + rect.width > width) {
                    xp = width - rect.width;
                    dir = -1;
                }
            } else {
                xp -= rect.width;
                if (xp < 0) {
                    xp = 0;
                    dir = 1;
                }
            }

            int yp = 0;
            for(int x=0 ; x<rect.width ; x++) {
                yp = Math.max(yp, usedY[xp + x]);
            }

            rect.x = xp;
            rect.y = yp;

            //System.out.println("xp="+xp+" yp="+yp+" w="+rect.width+" h="+rect.height+" adv="+rect.advance);
            
            Graphics2D gGlyph = (Graphics2D) g.create(xp, yp, rect.width, rect.height);
            try {
                for(Effect effect : effects) {
                    effect.preGlyphRender(gGlyph, fontInfo, rect);
                }
                rect.drawGlyph(gGlyph);
                for(Effect effect : effects) {
                    effect.postGlyphRender(gGlyph, fontInfo, rect);
                }
            } finally {
                gGlyph.dispose();
            }

            yp += rect.height + 1;
            for(int x=0 ; x<rect.width ; x++) {
                usedY[xp + x] = yp;
            }

            if(yp > usedTextureHeight) {
                usedTextureHeight = yp;
            }
            
            if(dir > 0) {
                xp += rect.width + 1;
            } else {
                xp -= 1;
            }
        }

        for(Effect effect : effects) {
            effect.postPageRender(g, fontInfo);
        }

        Arrays.sort(rects, new Comparator<GlyphRect>() {
            public int compare(GlyphRect a, GlyphRect b) {
                return a.ch - b.ch;
            }
        });
    }

    public int getUsedTextureHeight() {
        return usedTextureHeight;
    }

    public boolean getTextureData(IntBuffer ib) {
        if(image != null) {
            ib.put(((DataBufferInt)image.getRaster().getDataBuffer()).getData());
            return true;
        }
        return false;
    }

    public void write(File file) throws IOException {
        File dir = file.getParentFile();
        String baseName = getBaseName(file);
        
        PNGWriter.write(new File(dir, baseName.concat("_00.png")), image, usedTextureHeight);
        OutputStream os = new FileOutputStream(file);
        try {
            writeXML(os, baseName);
        } finally {
            os.close();
        }
    }

    public File[] getFilesCreatedForName(File file) {
        File dir = file.getParentFile();
        String baseName = getBaseName(file);

        return new File[] {
            file,
            new File(dir, baseName.concat("_00.png"))
        };
    }

    private void writeXML(OutputStream os, String basename) throws IOException {
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(false);
            XmlSerializer xs = factory.newSerializer();
            xs.setOutput(os, "UTF8");
            xs.startDocument("UTF8", true);
            xs.text("\n");
            xs.startTag(null, "font");
            xs.text("\n  ");
            xs.startTag(null, "info");
            xs.attribute(null, "face", fontData.getName());
            xs.attribute(null, "size", Integer.toString((int)fontData.getSize()));
            xs.attribute(null, "bold", fontData.getJavaFont().isBold() ? "1" : "0");
            xs.attribute(null, "italic", fontData.getJavaFont().isItalic() ? "1" : "0");
            xs.attribute(null, "charset", "");
            xs.attribute(null, "unicode", "1");
            xs.attribute(null, "stretchH", "100");
            xs.attribute(null, "smooth", "0");
            xs.attribute(null, "aa", "1");
            xs.attribute(null, "padding", padding.top+","+padding.left+","+padding.bottom+","+padding.right);   // order?
            xs.attribute(null, "spacing", "1,1");
            xs.endTag(null, "info");
            xs.text("\n  ");
            xs.startTag(null, "common");
            int lineHeight = descent + ascent + leading + padding.bottom + padding.top;
            xs.attribute(null, "lineHeight", Integer.toString(lineHeight));
            xs.attribute(null, "base", Integer.toString(ascent));
            xs.attribute(null, "scaleW", Integer.toString(image.getWidth()));
            xs.attribute(null, "scaleH", Integer.toString(image.getHeight()));
            xs.attribute(null, "pages", "1");
            xs.attribute(null, "packed", "0");
            xs.endTag(null, "common");
            xs.text("\n  ");
            xs.startTag(null, "pages");
            xs.text("\n    ");
            xs.startTag(null, "page");
            xs.attribute(null, "id", "0");
            xs.attribute(null, "file", basename.concat("_00.png"));
            xs.endTag(null, "page");
            xs.text("\n  ");
            xs.endTag(null, "pages");
            xs.text("\n  ");
            xs.startTag(null, "chars");
            xs.attribute(null, "count", Integer.toString(rects.length));
            for(GlyphRect rect : rects) {
                xs.text("\n    ");
                xs.startTag(null, "char");
                xs.attribute(null, "id", Integer.toString(rect.ch));
                xs.attribute(null, "x", Integer.toString(rect.x));
                xs.attribute(null, "y", Integer.toString(rect.y));
                xs.attribute(null, "width", Integer.toString(rect.width));
                xs.attribute(null, "height", Integer.toString(rect.height));
                xs.attribute(null, "xoffset", "0");
                xs.attribute(null, "yoffset", Integer.toString(ascent + rect.yoffset));
                xs.attribute(null, "xadvance", Integer.toString(rect.advance));
                xs.attribute(null, "page", "0");
                xs.attribute(null, "chnl", "0");
                xs.endTag(null, "char");
            }
            xs.text("\n  ");
            xs.endTag(null, "chars");
            xs.text("\n  ");
            xs.startTag(null, "kernings");
            int[][] kerinings = fontData.getKernings();
            xs.attribute(null, "count", Integer.toString(kerinings.length));
            for(int[] kerning : kerinings) {
                xs.text("\n    ");
                xs.startTag(null, "kerning");
                xs.attribute(null, "first", Integer.toString(kerning[0]));
                xs.attribute(null, "second", Integer.toString(kerning[1]));
                xs.attribute(null, "amount", Integer.toString(kerning[2]));
                xs.endTag(null, "kerning");
                xs.comment(" '" + ch2str(kerning[0]) + "' to '" + ch2str(kerning[1]) + "' ");
            }
            xs.text("\n  ");
            xs.endTag(null, "kernings");
            xs.text("\n");
            xs.endTag(null, "font");
            xs.endDocument();
        } catch (XmlPullParserException ex) {
            throw (IOException)(new IOException().initCause(ex));
        }
    }

    private String ch2str(int ch) {
        if(Character.isISOControl(ch)) {
            return String.format("\\u%04X", ch);
        } else {
            return Character.toString((char)ch);
        }
    }

    private String getBaseName(File file) {
        String baseName = file.getName();
        int idx = baseName.lastIndexOf('.');
        if(idx > 0) {
            baseName = baseName.substring(0, idx);
        }
        return baseName;
    }
}
