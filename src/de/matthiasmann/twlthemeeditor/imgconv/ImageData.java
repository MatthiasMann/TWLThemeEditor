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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Matthias Mann
 */
public class ImageData {

    private static final Color TRANSPARENT = new Color(0, 0, 0, 0);
    
    private final ImageReader imageReader;
    private final int numImages;
    private final BufferedImage[] images;
    private final GIFImageMetadata[] gifImagesMetadata;

    private int backgroundColorIndex;
    private Node globalColorTable;
    private BufferedImage backgroundForNext;
    private Rectangle eraseForNext;
    private int lastImgNr;

    private ImageData(ImageReader imageReader, int numImages) throws IOException {
        this.imageReader = imageReader;
        this.numImages = numImages;

        this.images = new BufferedImage[numImages];

        IIOMetadata iiom = imageReader.getStreamMetadata();
        Node gifMetadata = getMetaDataTree(iiom, "javax_imageio_gif_stream_1.0");
        if(gifMetadata != null) {
            this.gifImagesMetadata = new GIFImageMetadata[numImages];

            Node logicalScreenDescriptor = getChildNode(gifMetadata, "LogicalScreenDescriptor");
            int logicalScreenWidth = getInt(logicalScreenDescriptor, "logicalScreenWidth", 0);
            int logicalScreenHeight = getInt(logicalScreenDescriptor, "logicalScreenHeight", 0);

            if(logicalScreenWidth <= 0 || logicalScreenHeight <= 0) {
                for(int i=0 ; i<numImages ; i++) {
                    GIFImageMetadata gifim = getGIFImageMetadata(i);
                    logicalScreenWidth  = Math.max(logicalScreenWidth,  gifim.imageLeftPosition + gifim.imageWidth);
                    logicalScreenHeight = Math.max(logicalScreenHeight, gifim.imageTopPosition + gifim.imageHeight);
                }
            }

            backgroundForNext = new BufferedImage(logicalScreenWidth, logicalScreenHeight, BufferedImage.TYPE_INT_ARGB);

            globalColorTable = getChildNode(gifMetadata, "GlobalColorTable");
            backgroundColorIndex  = getInt(globalColorTable, "backgroundColorIndex", -1);
            eraseForNext = new Rectangle(0, 0, logicalScreenWidth, logicalScreenHeight);
        } else {
            gifImagesMetadata = null;
        }
    }

    public int getNumImages() {
        return numImages;
    }

    public BufferedImage getImage(int imageIndex) {
        decodeTo(imageIndex);
        return images[imageIndex];
    }

    public int getDelayMS(int imageIndex) {
        if(gifImagesMetadata != null) {
            GIFImageMetadata gifim = getGIFImageMetadata(imageIndex);
            return gifim.delayTimeMS;
        } else {
            return 100; // assume 10 Hz
        }
    }
    
    public static ImageData create(File file) {
        try {
            ImageInputStream iis = ImageIO.createImageInputStream(file);
            if(iis != null) {
                Iterator<ImageReader> iri = ImageIO.getImageReaders(iis);
                while(iri.hasNext()) {
                    ImageReader ir = iri.next();
                    ir.setInput(iis);

                    try {
                        int num = ir.getNumImages(true);
                        if(num >= 1) {
                            return new ImageData(ir, num);
                        }
                    } catch (IOException ex) {
                    }
                }
            }
        } catch (IOException ex) {
        }
        return null;
    }

    private GIFImageMetadata getGIFImageMetadata(int imageIndex) {
        GIFImageMetadata gifim = gifImagesMetadata[imageIndex];
        if(gifim == null) {
            Node gifMetadata;
            try {
                IIOMetadata iiom = imageReader.getImageMetadata(imageIndex);
                gifMetadata = getMetaDataTree(iiom, "javax_imageio_gif_image_1.0");
            } catch (IOException ignore) {
                gifMetadata = null;
            }
            
            gifim = new GIFImageMetadata(gifMetadata);
            gifImagesMetadata[imageIndex] = gifim;
        }
        return gifim;
    }
    
    private void decodeTo(int imageIndex) {
        while(lastImgNr <= imageIndex) {
            decodeNextImage();
        }
    }

    private void decodeNextImage() {
        BufferedImage img;
        try {
            img = imageReader.read(lastImgNr);

            if(gifImagesMetadata != null) {
                img = postProcessGIF(lastImgNr, img);
            }
        } catch (IOException ex) {
            img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        }
        images[lastImgNr++] = img;
    }

    private BufferedImage postProcessGIF(int imageIndex, BufferedImage img) {
        GIFImageMetadata gifim = getGIFImageMetadata(imageIndex);

        BufferedImage tmp = new BufferedImage(backgroundForNext.getWidth(), backgroundForNext.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = tmp.createGraphics();
        try {
            g.setComposite(AlphaComposite.Src);
            g.drawImage(backgroundForNext, 0, 0, null);
            if(eraseForNext != null) {
                Color backgroundColor;
                if(gifim.transparentColorFlag && gifim.transparentColorIndex == backgroundColorIndex) {
                    backgroundColor = TRANSPARENT;
                } else {
                    Node colorEntry = getChildNodeWithIndex(gifim.localColorTable, "ColorTableEntry", backgroundColorIndex);
                    if(colorEntry == null) {
                        colorEntry = getChildNodeWithIndex(globalColorTable, "ColorTableEntry", backgroundColorIndex);
                    }
                    int red   = getInt(colorEntry, "red", 0);
                    int green = getInt(colorEntry, "green", 0);
                    int blue  = getInt(colorEntry, "blue", 0);
                    backgroundColor = new Color(red, green, blue);
                }

                g.setColor(backgroundColor);
                g.fillRect(eraseForNext.x, eraseForNext.y, eraseForNext.width, eraseForNext.height);
            }
            g.setComposite(AlphaComposite.SrcOver);
            g.drawImage(img, gifim.imageLeftPosition, gifim.imageTopPosition, null);
        } finally {
            g.dispose();
        }
        img = tmp;

        eraseForNext = null;

        if("doNotDispose".equals(gifim.disposalMethod) || "none".equals(gifim.disposalMethod)) {
            backgroundForNext = img;
        } else if("restoreToBackgroundColor".equals(gifim.disposalMethod)) {
            eraseForNext = new Rectangle(gifim.imageLeftPosition, gifim.imageTopPosition, gifim.imageWidth, gifim.imageHeight);
        } else {
            // assume "restoreToPrevious" - so nothing to change
        }

        return img;
    }

    static Node getMetaDataTree(IIOMetadata iiom, String format) {
        if(iiom != null) {
            try {
                return iiom.getAsTree(format);
            } catch (IllegalArgumentException ignore) {
            }
        }
        return null;
    }
    
    static Node getChildNode(Node node, String name) {
        if(node != null) {
            NodeList children = node.getChildNodes();
            for(int i=0 ; i<children.getLength() ; i++) {
                Node child = children.item(i);
                if(name.equals(child.getNodeName())) {
                    return child;
                }
            }
        }
        return null;
    }

    static Node getChildNodeWithIndex(Node node, String name, int index) {
        if(node != null) {
            NodeList children = node.getChildNodes();
            for(int i=0 ; i<children.getLength() ; i++) {
                Node child = children.item(i);
                if(name.equals(child.getNodeName())) {
                    int childIndex = getInt(child, "index", -1);
                    if(childIndex == index) {
                        return child;
                    }
                }
            }
        }
        return null;
    }

    static String getStr(Node node, String attribute, String defaultValue) {
        if(node != null) {
            Node attribNode = node.getAttributes().getNamedItem(attribute);
            if(attribNode != null) {
                return attribNode.getNodeValue();
            }
        }
        return defaultValue;
    }
    
    static int getInt(Node node, String attribute, int defaultValue) {
        String value = getStr(node, attribute, null);
        if(value != null) {
            try {
                return Integer.parseInt(value);
            } catch (IllegalArgumentException ignore) {
            }
        }
        return defaultValue;
    }

    static class GIFImageMetadata {
        final int imageLeftPosition;
        final int imageTopPosition;
        final int imageWidth;
        final int imageHeight;
        final int delayTimeMS;
        final String disposalMethod;
        final int transparentColorIndex;
        final boolean transparentColorFlag;
        final Node localColorTable;

        public GIFImageMetadata(Node gifMetadata) {
            Node imageDescriptor = getChildNode(gifMetadata, "ImageDescriptor");
            imageLeftPosition = getInt(imageDescriptor, "imageLeftPosition", 0);
            imageTopPosition  = getInt(imageDescriptor, "imageTopPosition", 0);
            imageWidth        = getInt(imageDescriptor, "imageWidth", 1);
            imageHeight       = getInt(imageDescriptor, "imageHeight", 1);

            Node graphicControlExtension = getChildNode(gifMetadata, "GraphicControlExtension");
            delayTimeMS           = getInt(graphicControlExtension, "delayTime", 10) * 10; // in 10 ms steps
            disposalMethod        = getStr(graphicControlExtension, "disposalMethod", "none");
            transparentColorIndex = getInt(graphicControlExtension, "transparentColorIndex", 255);
            transparentColorFlag  = Boolean.parseBoolean(getStr(graphicControlExtension, "transparentColorFlag", "false"));

            localColorTable = getChildNode(gifMetadata, "LocalColorTable");
        }
    }
}
