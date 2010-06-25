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
package de.matthiasmann.twlthemeeditor.fontgen.gui;

import de.matthiasmann.twl.GUI;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.renderer.DynamicImage;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

/**
 *
 * @author Matthias Mann
 */
public class FontPreview extends Widget {

    private static final int WIDTH = 1024;
    private static final int HEIGHT = 64;

    private final BufferedImage bi;
    private final ByteBuffer bb;
    private final IntBuffer ib;

    private String text;
    private Font font;
    private DynamicImage image;
    private boolean imageDirty;

    public FontPreview() {
        this.bi = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        this.text = "The quick brown fox jumps over the lazy dog.";
        this.imageDirty = true;

        this.bb = ByteBuffer.allocateDirect(WIDTH * HEIGHT * 4);
        this.ib = bb.order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();

        setClip(true);
    }

    public Font getFont() {
        return font;
    }

    public void setFont(Font font) {
        this.font = font;
        this.imageDirty = true;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
        this.imageDirty = true;
    }

    @Override
    public int getPreferredInnerWidth() {
        return WIDTH;
    }

    @Override
    public int getPreferredInnerHeight() {
        return HEIGHT;
    }

    @Override
    public void destroy() {
        super.destroy();
        if(image != null) {
            image.destroy();
            image = null;
        }
    }

    @Override
    protected void paintWidget(GUI gui) {
        if(imageDirty) {
            updateImage(gui);
        }

        if(image != null){
            image.draw(getAnimationState(), getInnerX(), getInnerY());
        }
    }

    private void updateImage(GUI gui) {
        imageDirty = false;

        if(image == null) {
            image = gui.getRenderer().createDynamicImage(WIDTH, HEIGHT);
        }

        if(image != null) {
            Graphics2D g = bi.createGraphics();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, WIDTH, HEIGHT);

                if(font != null && text != null) {
                    g.setColor(Color.BLACK);
                    g.setFont(font);
                    FontMetrics fm = g.getFontMetrics();
                    g.drawString(text, 10, fm.getAscent() + fm.getLeading());
                }
            } finally {
                g.dispose();
            }

            ib.clear();
            ib.put(((DataBufferInt)bi.getRaster().getDataBuffer()).getData());
            image.update(bb, DynamicImage.Format.BGRA);
        }
    }

}
