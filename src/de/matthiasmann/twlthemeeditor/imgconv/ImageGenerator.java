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
package de.matthiasmann.twlthemeeditor.imgconv;

import de.matthiasmann.twlthemeeditor.fontgen.PNGWriter;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.IntBuffer;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

/**
 *
 * @author Matthias Mann
 */
public class ImageGenerator {

    public static final int MAX_TEXTURE_SIZE = 4096;
    
    private final BufferedImage texture;
    private final Frame[] frames;
    private final boolean cutoff;

    public ImageGenerator(ImageData imageData, boolean skipZeroDelayFrames) {
        int numFrames = imageData.getNumImages();

        if(skipZeroDelayFrames) {
            for(int i=numFrames ; i-->0 ;) {
                if(imageData.getDelayMS(i) == 0) {
                    numFrames--;
                }
            }
        }

        BufferedImage image0 = imageData.getImage(0);

        // 1 pixel padding
        int imgWidth = image0.getWidth() + 1;
        int imgHeight = image0.getHeight() + 1;

        int textureSize = nextPOT(imgWidth);
        int numFramesX;
        int numFramesY;

        for(;;) {
            numFramesX = (textureSize + 1) / imgWidth;
            numFramesY = (textureSize + 1) / imgHeight;
            if(numFramesX * numFramesY >= numFrames) {
                break;
            }
            if(textureSize == MAX_TEXTURE_SIZE) {
                break;
            }
            textureSize <<= 1;
        }

        numFramesY = Math.min(numFramesY, (numFrames + numFramesX - 1) / numFramesX);

        int textureWidth  = numFramesX * imgWidth - 1;   // no padding on the right
        int textureHeight = numFramesY * imgHeight - 1;   // no padding on the bottom

        frames = new Frame[Math.min(numFrames, numFramesX * numFramesY)];
        cutoff = frames.length < numFrames;

        texture = new BufferedImage(textureWidth, textureHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = texture.createGraphics();
        try {
            g.setComposite(AlphaComposite.Src);

            for(int imgNr=0,frameNr=0,ix=0,iy=0 ; imgNr<imageData.getNumImages() && iy < numFramesY ; imgNr++) {
                int delayMS = imageData.getDelayMS(imgNr);
                if(skipZeroDelayFrames && delayMS == 0) {
                    continue;
                }

                Frame frame = new Frame(ix*imgWidth, iy*imgHeight, imgWidth-1, imgHeight-1, delayMS);   // exclude padding
                frames[frameNr++] = frame;
                
                g.drawImage(imageData.getImage(imgNr), frame.x, frame.y, null);

                if(++ix == numFramesX) {
                    ix = 0;
                    ++iy;
                }
            }
        } finally {
            g.dispose();
        }
    }

    public boolean isCutoff() {
        return cutoff;
    }

    public int getNumFrames() {
        return frames.length;
    }

    public int getWidth() {
        return texture.getWidth();
    }

    public int getHeight() {
        return texture.getHeight();
    }

    public boolean getTextureData(IntBuffer ib) {
        if(texture != null) {
            ib.put(((DataBufferInt)texture.getRaster().getDataBuffer()).getData());
            return true;
        }
        return false;
    }

    public File[] getFilesCreatedForName(File file) {
        File dir = file.getParentFile();
        String baseName = getBaseName(file);

        return new File[] {
            file,
            new File(dir, baseName.concat(".xml"))
        };
    }

    public void write(File file) throws IOException {
        writePNG(file);
        writeXML(file);
    }
    
    private void writePNG(File pngFile) throws IOException {
        PNGWriter.write(pngFile, texture, texture.getHeight());
    }

    private void writeXML(File pngFile) throws IOException {
        File dir = pngFile.getParentFile();
        String baseName = getBaseName(pngFile);
        File xmlFile = new File(dir, baseName.concat(".xml"));

        OutputStream os = new FileOutputStream(xmlFile);
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(false);
            XmlSerializer xs = factory.newSerializer();
            xs.setOutput(os, "UTF8");
            xs.startDocument("UTF8", true);
            xs.text("\n");
            xs.startTag(null, "themes");
            xs.text("\n  ");
            xs.startTag(null, "images");
            xs.attribute(null, "file", pngFile.getName());
            xs.attribute(null, "format", "COLOR");

            if(frames.length > 1) {
                for(int frameNr=0 ; frameNr<frames.length ; frameNr++) {
                    xs.text("\n    ");
                    xs.startTag(null, "area");
                    xs.attribute(null, "name", baseName+"-"+frameNr);
                    xs.attribute(null, "xywh", frames[frameNr].getXYWH());
                    xs.endTag(null, "area");
                }
                xs.text("\n    \n    ");
                xs.startTag(null, "animation");
                xs.attribute(null, "name", baseName);
                xs.attribute(null, "timeSource", "hover");
                for(int frameNr=0 ; frameNr<frames.length ; frameNr++) {
                    xs.text("\n      ");
                    xs.startTag(null, "frame");
                    xs.attribute(null, "duration", Integer.toString(frames[frameNr].delayMS));
                    xs.attribute(null, "ref", baseName+"-"+frameNr);
                    xs.endTag(null, "frame");
                }
                xs.text("\n    ");
                xs.endTag(null, "animation");
            } else if(frames.length == 1) {
                xs.text("\n    ");
                xs.startTag(null, "area");
                xs.attribute(null, "name", baseName);
                xs.attribute(null, "xywh", frames[0].getXYWH());
                xs.endTag(null, "area");
            }
            
            xs.text("\n  ");
            xs.endTag(null, "images");
            xs.text("\n");
            xs.endTag(null, "themes");
            xs.endDocument();
        } catch (XmlPullParserException ex) {
            throw (IOException)(new IOException().initCause(ex));
        } finally {
            os.close();
        }
    }

    private static String getBaseName(File file) {
        String baseName = file.getName();
        int idx = baseName.lastIndexOf('.');
        if(idx > 0) {
            baseName = baseName.substring(0, idx);
        }
        return baseName;
    }

    private static int nextPOT(int value) {
        return 1 << (32 - Integer.numberOfLeadingZeros(value - 1));
    }

    public static class Frame {
        public final int x;
        public final int y;
        public final int width;
        public final int height;
        public final int delayMS;

        public Frame(int x, int y, int width, int height, int delayMS) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.delayMS = delayMS;
        }

        public String getXYWH() {
            return x+","+y+","+width+","+height;
        }
    }
}
