/*
 * Copyright (c) 2008-2011, Matthias Mann
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

import de.matthiasmann.twl.Button;
import de.matthiasmann.twl.Color;
import de.matthiasmann.twl.GUI;
import de.matthiasmann.twl.ThemeInfo;
import de.matthiasmann.twl.model.ColorModel;
import de.matthiasmann.twl.renderer.Image;

/**
 *
 * @author Matthias Mann
 */
public class ColorButton extends Button {
    
    private Image white;
    private Image colored;
    private Color color = Color.WHITE;
    private String colorName;
    private ColorModel model;
    private Runnable modelCallback;
    private String userText;

    public void setColor(Color color, String colorName) {
        this.color = color;
        this.colorName = colorName;
        updateColored();
    }

    public ColorModel getColorModel() {
        return model;
    }

    public void setColorModel(ColorModel model) {
        if(this.model != model) {
            removeModelCallback();
            this.model = model;
            if(model != null) {
                addModelCallback();
                modelValueChanged();
            }
        }
    }

    @Override
    public String getText() {
        return userText;
    }

    @Override
    public void setText(String text) {
        this.userText = text;
        updateText();
    }

    @Override
    protected void applyTheme(ThemeInfo themeInfo) {
        super.applyTheme(themeInfo);
        white = themeInfo.getImage("white");
        updateColored();
    }

    @Override
    protected void paintWidget(GUI gui) {
        if(colored != null) {
            colored.draw(getAnimationState(), getX(), getY(), getWidth(), getHeight());
        }
        super.paintWidget(gui);
    }

    private void updateColored() {
        if(white != null) {
            colored = white.createTintedVersion(color);
        }
        updateText();
    }
    
    private void updateText() {
        String text = color.toString();
        if(userText != null) {
            if(colorName != null) {
                text = userText + " (" + colorName + ": " + text + ")";
            } else {
                text = userText + " (" + text + ")";
            }
        } else if(colorName != null) {
            text = colorName + " (" + text + ")";
        }
        super.setText(text);
    }

    @Override
    protected void afterAddToGUI(GUI gui) {
        super.afterAddToGUI(gui);
        addModelCallback();
    }

    @Override
    protected void beforeRemoveFromGUI(GUI gui) {
        removeModelCallback();
        super.beforeRemoveFromGUI(gui);
    }
    
    private void removeModelCallback() {
        if(model != null) {
            model.removeCallback(modelCallback);
        }
    }
    
    private void addModelCallback() {
        if(model != null && getGUI() != null) {
            if(modelCallback == null) {
                modelCallback = new Runnable() {
                    public void run() {
                        modelValueChanged();
                    }
                };
            }
            model.addCallback(modelCallback);
        }
    }
    
    void modelValueChanged() {
        if(model != null) {
            setColor(model.getValue(), null);
        }
    }
}
