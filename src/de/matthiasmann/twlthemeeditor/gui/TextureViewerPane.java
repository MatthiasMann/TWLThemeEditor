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
package de.matthiasmann.twlthemeeditor.gui;

import de.matthiasmann.twl.Color;
import de.matthiasmann.twl.DialogLayout;
import de.matthiasmann.twl.Label;
import de.matthiasmann.twl.Rect;
import de.matthiasmann.twl.ScrollPane;
import de.matthiasmann.twl.ToggleButton;
import de.matthiasmann.twl.ValueAdjusterFloat;
import de.matthiasmann.twl.model.FloatModel;
import de.matthiasmann.twl.model.SimpleBooleanModel;
import de.matthiasmann.twl.model.SimpleFloatModel;
import java.net.URL;

/**
 *
 * @author Matthias Mann
 */
public class TextureViewerPane extends DialogLayout {

    private final TextureViewer textureViewer;
    private final ScrollPane scrollPane;
    private final Label labelErrorDisplay;
    private final ToggleButton btnShowCompleteTexture;
    
    private final SimpleFloatModel zoomFactorX;
    private final SimpleFloatModel zoomFactorY;
    private final SimpleBooleanModel linkZoomFactors;
    private final SimpleBooleanModel showCompleteTexture;

    private Rect rect;

    public TextureViewerPane() {
        this.textureViewer = new TextureViewer();
        this.scrollPane = new ScrollPane(textureViewer);
        this.labelErrorDisplay = new Label();
        
        this.zoomFactorX = new SimpleFloatModel(0.1f, 10.0f, 1.0f);
        this.zoomFactorY = new SimpleFloatModel(0.1f, 10.0f, 1.0f);
        this.linkZoomFactors = new SimpleBooleanModel(true);
        this.showCompleteTexture = new SimpleBooleanModel(false);

        ValueAdjusterFloat adjusterZoomX = new ZoomAdjuster(zoomFactorX);
        adjusterZoomX.setDisplayPrefix("X: ");
        adjusterZoomX.setFormat("%2.1f");
        ValueAdjusterFloat adjusterZoomY = new ZoomAdjuster(zoomFactorY);
        adjusterZoomY.setDisplayPrefix("Y: ");
        adjusterZoomY.setFormat("%2.1f");

        ToggleButton btnLinkZoomFactors = new ToggleButton(linkZoomFactors);
        btnLinkZoomFactors.setTheme("linkZoomFactors");

        btnShowCompleteTexture = new ToggleButton(showCompleteTexture);
        btnShowCompleteTexture.setTheme("showCompleteTexture");

        Group horzCtrls = createSequentialGroup()
                .addGap()
                .addWidgets(adjusterZoomX, btnLinkZoomFactors, adjusterZoomY)
                .addGap()
                .addWidget(btnShowCompleteTexture)
                .addGap();
        Group vertCtrls = createParallelGroup(adjusterZoomX, btnLinkZoomFactors,
                adjusterZoomY, btnShowCompleteTexture);

        setHorizontalGroup(createParallelGroup()
                .addWidget(scrollPane)
                .addGroup(horzCtrls)
                .addWidget(labelErrorDisplay));
        setVerticalGroup(createSequentialGroup()
                .addWidget(scrollPane)
                .addGroup(vertCtrls)
                .addWidget(labelErrorDisplay));

        zoomFactorX.addCallback(new Runnable() {
            public void run() {
                if(linkZoomFactors.getValue()) {
                    zoomFactorY.setValue(zoomFactorX.getValue());
                }
                updateZoom();
            }
        });
        zoomFactorY.addCallback(new Runnable() {
            public void run() {
                if(linkZoomFactors.getValue()) {
                    zoomFactorX.setValue(zoomFactorY.getValue());
                }
                updateZoom();
            }
        });
        showCompleteTexture.addCallback(new Runnable() {
            public void run() {
                updateRect();
            }
        });
        textureViewer.addCallback(new Runnable() {
            public void run() {
                Throwable ex = textureViewer.getLoadException();
                labelErrorDisplay.setText((ex != null) ? ex.getMessage() : "");
                labelErrorDisplay.getAnimationState().setAnimationState("error", ex != null);
            }
        });

        updateZoom();
    }

    public void setUrl(URL url) {
        textureViewer.setUrl(url);
    }

    public void setRect(Rect rect) {
        this.rect = rect;
        btnShowCompleteTexture.setEnabled(rect != null);
        updateRect();
    }

    public void setTintColor(Color tintColor) {
        textureViewer.setTintColor(tintColor);
    }

    void updateZoom() {
        textureViewer.setZoom(zoomFactorX.getValue(), zoomFactorY.getValue());
    }

    void updateRect() {
        textureViewer.setRect(showCompleteTexture.getValue() ? null : rect);
    }

    static class ZoomAdjuster extends ValueAdjusterFloat {
        public ZoomAdjuster(FloatModel model) {
            super(model);
        }

        @Override
        protected void doDecrement() {
            float value = getValue();
            float stepSize = (value > 1f) ? 1f : 0.1f;

            float floor = (float)Math.floor(value / stepSize) * stepSize;
            setValue((floor < value) ? floor : floor - stepSize);
        }

        @Override
        protected void doIncrement() {
            float value = getValue();
            float stepSize = (value < 1f) ? 0.1f : 1f;

            float ceil = (float)Math.ceil(value / stepSize) * stepSize;
            setValue((ceil > value) ? ceil : ceil + stepSize);
        }
    }
}
