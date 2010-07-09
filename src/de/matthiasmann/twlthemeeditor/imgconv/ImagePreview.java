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

import de.matthiasmann.twl.GUI;
import de.matthiasmann.twl.Timer;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.renderer.DynamicImage;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

/**
 *
 * @author Matthias Mann
 */
public class ImagePreview extends Widget {

    private static final int WIDTH = 256;
    private static final int HEIGHT = 256;

    private final BufferedImage bi;
    private final ByteBuffer bb;
    private final IntBuffer ib;

    private boolean imageDirty = true;
    private ImageData imgData;
    private DynamicImage image;
    private Timer timer;
    private int curFrame;

    public ImagePreview() {
        this.bi = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        this.bb = ByteBuffer.allocateDirect(WIDTH * HEIGHT * 4);
        this.ib = bb.order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();

        setClip(true);
    }

    public void setImageData(ImageData imgData) {
        this.imgData = imgData;
        this.curFrame = 0;
        this.imageDirty = true;
        if(timer != null) {
            timer.stop();
        }
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
            imageDirty = false;
            updateImage(gui);
        }
        
        if(image != null){
            image.draw(getAnimationState(), getInnerX(), getInnerY());
        }
    }

    @Override
    protected void afterAddToGUI(GUI gui) {
        super.afterAddToGUI(gui);
        timer = gui.createTimer();
        timer.setDelay(100);
        timer.setCallback(new Runnable() {
            public void run() {
                nextFrame();
            }
        });
        startTimer();
    }

    @Override
    protected void beforeRemoveFromGUI(GUI gui) {
        if(timer != null) {
            timer.stop();
            timer = null;
        }
        super.beforeRemoveFromGUI(gui);
    }

    void nextFrame() {
        if(imgData != null) {
            curFrame = (curFrame + 1) % imgData.getNumImages();
            imageDirty = true;
        }
    }

    private void updateImage(GUI gui) {
        if(image == null) {
            image = gui.getRenderer().createDynamicImage(WIDTH, HEIGHT);
        }

        if(image != null) {
            Graphics2D g = bi.createGraphics();
            try {
                g.setComposite(AlphaComposite.Src);
                g.setColor(new Color(0,0,0,0));
                g.fillRect(0, 0, WIDTH, HEIGHT);

                if(imgData != null) {
                    BufferedImage img = imgData.getImage(curFrame);
                    int width = img.getWidth();
                    int height = img.getHeight();

                    if(width > WIDTH || height > HEIGHT) {
                        int w = width * HEIGHT / height;
                        int h = height * WIDTH / width;
                        if(w > WIDTH) {
                            width = WIDTH;
                            height = h;
                        } else {
                            width = w;
                            height = HEIGHT;
                        }
                    }

                    g.drawImage(img, 0, 0, width, height, null);
                }
            } finally {
                g.dispose();
            }

            ib.clear();
            ib.put(((DataBufferInt)bi.getRaster().getDataBuffer()).getData());
            image.update(bb, DynamicImage.Format.BGRA);
        }

        startTimer();
    }

    private void startTimer() {
        if(imgData != null && imgData.getNumImages() > 1 && timer != null) {
            int delay = Math.max(10, imgData.getDelayMS(curFrame));
            timer.setDelay(delay);
            timer.start();
        }
    }
}
