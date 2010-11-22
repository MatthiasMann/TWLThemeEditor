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

import com.sun.jna.Platform;
import de.matthiasmann.javafreetype.FreeTypeCodePointIterator;
import de.matthiasmann.javafreetype.FreeTypeFont;
import de.matthiasmann.javafreetype.FreeTypeGlyphInfo;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphMetrics;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

/**
 *
 * @author Matthias Mann
 */
public class FontGenerator {

    public enum ExportFormat {
        XML,
        TEXT
    };

    public enum GeneratorMethod {
        AWT_VECTOR(true, true, true),
        AWT_DRAWSTRING(true, true, false),
        FREETYPE2(isFreeTypeAvailable(), false, false);

        public final boolean isAvailable;
        public final boolean supportsAAflag;
        public final boolean supportsEffects;

        private GeneratorMethod(boolean isAvailable, boolean supportsAAflag, boolean supportsEffects) {
            this.isAvailable = isAvailable;
            this.supportsAAflag = supportsAAflag;
            this.supportsEffects = supportsEffects;
        }
    }
    
    private final FontData fontData;
    private final GeneratorMethod generatorMethod;

    private Padding padding;
    private BufferedImage image;
    private GlyphRect[] rects;
    private int[][] kernings;
    private int ascent;
    private int descent;
    private int lineHeight;
    private int usedTextureHeight;

    public FontGenerator(FontData fontData, GeneratorMethod generatorMethod) {
        this.fontData = fontData;
        this.generatorMethod = generatorMethod;
    }
    
    public void generate(int width, int height, CharSet set, Padding padding, Effect.Renderer[] effects, boolean useAA) throws IOException {
        if(generatorMethod == GeneratorMethod.FREETYPE2) {
            generateFT2(width, height, set, padding);
        } else {
            generateAWT(width, height, set, padding, effects, useAA, generatorMethod == GeneratorMethod.AWT_DRAWSTRING);
        }
    }

    static class FT2Glyph implements Comparable<FT2Glyph> {
        final FreeTypeGlyphInfo info;
        final int glyphIndex;
        int x;
        int y;

        public FT2Glyph(FreeTypeGlyphInfo info, int glyphIndex) {
            this.info = info;
            this.glyphIndex = glyphIndex;
        }

        public int compareTo(FT2Glyph o) {
            int diff = o.info.getHeight() - info.getHeight();
            if(diff == 0) {
                diff = o.info.getWidth() - info.getWidth();
            }
            return diff;
        }
    }

    private void generateFT2(int width, int height, CharSet set, Padding padding) throws IOException {
        this.padding = new Padding(0, 0, 0, 0, padding.advance);
        this.image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        FreeTypeFont font = FreeTypeFont.create(fontData.getFontFile());
        try {
            font.setCharSize(0, fontData.getSize(), 72, 72);
            
            ascent = font.getAscent();
            descent = font.getDescent();
            lineHeight = font.getLineHeight();
            int maxHeight = ascent;

            IntMap<FT2Glyph> glyphMap = new IntMap<FT2Glyph>();
            int numCodePoints = 0;
            int numGlyphs = 0;

            FreeTypeCodePointIterator iter = font.iterateCodePoints();
            while(iter.nextCodePoint()) {
                int codepoint = iter.getCodePoint();
                int glyphIndex = iter.getGlyphIndex();

                if (!set.isIncluded(codepoint)) {
                    continue;
                }

                FT2Glyph glyph = glyphMap.get(glyphIndex);
                if(glyph == null) {
                    try {
                        glyph = new FT2Glyph(font.loadGlyph(glyphIndex), glyphIndex);
                        glyphMap.put(glyphIndex, glyph);

                        numGlyphs++;
                        maxHeight = Math.max(glyph.info.getHeight(), maxHeight);
                    } catch (IOException ex) {
                        // ignore
                        /*
                        Logger.getLogger(FontGenerator.class.getName()).log(Level.SEVERE,
                                "Can't retrieve glyph " + glyphIndex + " codepoint " + codepoint +
                                " (" + new String(Character.toChars(codepoint)) + ")", ex);
                         */
                    }
                }

                if(glyph != null) {
                    numCodePoints++;
                }
            }

            FT2Glyph[] glyphs = new FT2Glyph[numGlyphs];
            Iterator<IntMap.Entry<FT2Glyph>> glyphIter = glyphMap.iterator();
            for(int idx=0 ; glyphIter.hasNext() ; idx++) {
                glyphs[idx] = glyphIter.next().value;
            }
            
            Arrays.sort(glyphs);

            int xp = 0;
            int dir = 1;
            int[] usedY = new int[width];
            usedTextureHeight = 0;

            for (int glyphNr=0 ; glyphNr<numGlyphs ; glyphNr++) {
                final FT2Glyph glyph = glyphs[glyphNr];
                final int glyphWidth = glyph.info.getWidth();

                if (dir > 0) {
                    if (xp + glyphWidth > width) {
                        xp = width - glyphWidth;
                        dir = -1;
                    }
                } else {
                    xp -= glyphWidth;
                    if (xp < 0) {
                        xp = 0;
                        dir = 1;
                    }
                }

                int yp = 0;
                for(int x=0 ; x<glyphWidth ; x++) {
                    yp = Math.max(yp, usedY[xp + x]);
                }

                glyph.x = xp;
                glyph.y = yp;

                //System.out.println("xp="+xp+" yp="+yp+" w="+rect.width+" h="+rect.height+" adv="+rect.advance);

                font.loadGlyph(glyph.glyphIndex);
                font.copyGlpyhToBufferedImage(image, xp, yp, Color.WHITE);

                yp += glyph.info.getHeight() + 1;
                for(int x=0 ; x<glyphWidth ; x++) {
                    usedY[xp + x] = yp;
                }

                if(yp > usedTextureHeight) {
                    usedTextureHeight = yp;
                }

                if(dir > 0) {
                    xp += glyphWidth + 1;
                } else {
                    xp -= 1;
                }
            }
            
            rects = new GlyphRect[numCodePoints];
            iter = font.iterateCodePoints();
            for(int rectNr=0 ; iter.nextCodePoint() ;) {
                int codepoint = iter.getCodePoint();
                int glyphIndex = iter.getGlyphIndex();

                if (!set.isIncluded(codepoint)) {
                    continue;
                }

                FT2Glyph glyph = glyphMap.get(glyphIndex);
                if(glyph != null) {
                    GlyphRect rect = new GlyphRect((char)codepoint,
                            glyph.info.getWidth(), glyph.info.getHeight(),
                            glyph.info.getAdvanceX(), -glyph.info.getOffsetY(),
                            -glyph.info.getOffsetX(), 0, null);
                    rect.x = glyph.x;
                    rect.y = glyph.y;
                    rects[rectNr++] = rect;
                }
            }

            if(font.hasKerning()) {
                ArrayList<int[]> kerns = new ArrayList<int[]>();
                for(IntMap.Entry<IntMap<Integer>> from : fontData.getRawKerning()) {
                    if(set.isIncluded(from.key)) {
                        int leftGlyphIdx = font.getGlyphForCodePoint(from.key);
                        if(leftGlyphIdx > 0) {
                            for(IntMap.Entry<Integer> to : from.value) {
                                if(set.isIncluded(to.key)) {
                                    int rightGlyphIdx = font.getGlyphForCodePoint(to.key);
                                    if(rightGlyphIdx != 0) {
                                        int value = font.getKerning(leftGlyphIdx, rightGlyphIdx).x;
                                        if(value != 0) {
                                            kerns.add(new int[]{ from.key, to.key, value });
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                this.kernings = kerns.toArray(new int[kerns.size()][]);
            } else {
                this.kernings = new int[0][];
            }
        } finally {
            font.close();
        }
    }

    private void generateAWT(int width, int height, CharSet set, Padding padding, Effect.Renderer[] effects, boolean useAA, boolean useDrawString) {
        this.padding = padding;
        
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, useAA ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, useAA ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON : RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);

        Font font = fontData.getJavaFont();
        g.setFont(font);

        FontRenderContext fontRenderContext = g.getFontRenderContext();

        kernings = fontData.getKernings(set);
        ascent = g.getFontMetrics().getMaxAscent();
        descent = g.getFontMetrics().getMaxDescent();
        lineHeight = g.getFontMetrics().getLeading() + ascent + descent;
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
            final Rectangle bounds = metrics.getBounds2D().getBounds();
            
            int advance = Math.round(metrics.getAdvanceX());
            int glyphWidth = bounds.width + 1 + padding.left + padding.right;
            int glyphHeight = bounds.height + 1 + padding.top + padding.bottom;
            int xoffset = 1 - bounds.x;
            int yoffset = bounds.y - 1;

            GlyphRect rect = new GlyphRect(chBuffer[0],
                    glyphWidth, glyphHeight,
                    advance + padding.advance, yoffset,
                    xoffset + padding.left, padding.top,
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
                int diff = b.height - a.height;
                if(diff == 0) {
                    diff = b.width - a.width;
                }
                return diff;
            }
        });

        for(Effect.Renderer effect : effects) {
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
                for(Effect.Renderer effect : effects) {
                    effect.preGlyphRender(gGlyph, fontInfo, rect);
                }
                rect.drawGlyph(gGlyph, useDrawString);
                for(Effect.Renderer effect : effects) {
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

        for(Effect.Renderer effect : effects) {
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

    public int getAscent() {
        return ascent;
    }

    public int getDescent() {
        return descent;
    }

    public int getLineHeight() {
        return lineHeight;
    }

    public int getImageType() {
        if(image != null) {
            return image.getType();
        }
        return -1;
    }

    public boolean getTextureData(IntBuffer ib) {
        if(image != null) {
            ib.put(((DataBufferInt)image.getRaster().getDataBuffer()).getData());
            return true;
        }
        return false;
    }

    public boolean getTextureData(ByteBuffer bb) {
        if(image != null) {
            bb.put(((DataBufferByte)image.getRaster().getDataBuffer()).getData());
            return true;
        }
        return false;
    }

    public void write(File file, ExportFormat format) throws IOException {
        File dir = file.getParentFile();
        String baseName = getBaseName(file);
        
        PNGWriter.write(new File(dir, baseName.concat("_00.png")), image, usedTextureHeight);
        OutputStream os = new FileOutputStream(file);
        try {
            switch(format) {
                case XML:
                    writeXML(os, baseName);
                    break;
                case TEXT:
                    writeText(os, baseName);
                    break;
                default:
                    throw new AssertionError();
            }
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
            xs.comment("Created by TWL Theme Editor");
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
            xs.attribute(null, "lineHeight", Integer.toString(lineHeight + padding.top + padding.bottom));
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
                xs.attribute(null, "xoffset", Integer.toString(-rect.xDrawOffset));
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
            xs.attribute(null, "count", Integer.toString(kernings.length));
            for(int[] kerning : kernings) {
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

    public void writeText(OutputStream os, String basename) {
        PrintWriter pw = new PrintWriter(os);
        
        pw.printf("info face=%s size=%d bold=%d italic=%d charset=\"\" unicode=1 stretchH=100 smooth=0 aa=1 padding=%d,%d,%d,%d spacing=1,1\n",
                fontData.getName(), (int)fontData.getSize(),
                fontData.getJavaFont().isBold() ? 1 : 0,
                fontData.getJavaFont().isItalic() ? 1 : 0,
                padding.top, padding.left, padding.bottom, padding.right);

        pw.printf("common lineHeight=%d base=%s scaleW=%s scaleH=%d pages=1 packed=0\n",
                lineHeight + padding.bottom + padding.top, ascent, image.getWidth(), image.getHeight());

        pw.printf("page id=0 file=%s_00.png\n", basename);
        pw.printf("chars count=%d\n", rects.length);

        for(GlyphRect rect : rects) {
            pw.printf("char id=%d x=%d y=%d width=%d height=%d xoffset=%d yoffset=%d xadvance=%d page=0 chnl=0\n",
                    (int)rect.ch, rect.x, rect.y, rect.width, rect.height,
                    -rect.xDrawOffset, ascent+rect.yoffset, rect.advance);
        }
        
        pw.printf("kernings count=%d\n", kernings.length);
        for(int[] kerning : kernings){
            pw.printf("kerning first=%d second=%d amount=%d\n",
                    kerning[0], kerning[1], kerning[2]);
        }

        pw.close();
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

    static boolean isFreeTypeAvailable() {
        if(Platform.isWindows()) {
            FreeTypeFont.setNativeLibraryName(Platform.is64Bit()
                    ? "freetype6_amd64" : "freetype6_x86");
        }
        return FreeTypeFont.isAvailable();
    }
}
