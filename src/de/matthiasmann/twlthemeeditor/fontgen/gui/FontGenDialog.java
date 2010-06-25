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

import de.matthiasmann.twl.BoxLayout;
import de.matthiasmann.twl.Button;
import de.matthiasmann.twl.ComboBox;
import de.matthiasmann.twl.DialogLayout;
import de.matthiasmann.twl.EditField;
import de.matthiasmann.twl.GUI;
import de.matthiasmann.twl.PopupWindow;
import de.matthiasmann.twl.ScrollPane;
import de.matthiasmann.twl.SplitPane;
import de.matthiasmann.twl.ToggleButton;
import de.matthiasmann.twl.ValueAdjusterInt;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.BooleanModel;
import de.matthiasmann.twl.model.HasCallback;
import de.matthiasmann.twl.model.SimpleChangableListModel;
import de.matthiasmann.twl.model.SimpleIntegerModel;
import de.matthiasmann.twlthemeeditor.fontgen.CharSet;
import de.matthiasmann.twlthemeeditor.fontgen.FontData;
import de.matthiasmann.twlthemeeditor.fontgen.Padding;
import de.matthiasmann.twlthemeeditor.fontgen.effects.BlurShadowEffect;
import de.matthiasmann.twlthemeeditor.fontgen.effects.GradientEffect;
import java.lang.Character.UnicodeBlock;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 *
 * @author Matthias Mann
 */
public final class FontGenDialog {

    private final CharSet charSet;
    private final DialogLayout layout;
    private final PopupWindow popupWindow;
    
    private final EditField fontPathEF;
    private final Button selectFontBtn;
    private final SimpleChangableListModel<Integer> textureSizesModel;
    private final ComboBox<Integer> textureSizeCB;
    private final ComboBox<FontDisplayBG> fontDisplayBgCB;
    private final BoxLayout unicodeBlocksBox;
    private final ScrollPane unicodeBlocksSP;
    private final SimpleChangableListModel<Integer> fontSizesModel;
    private final ComboBox<Integer> fontSizeCB;
    private final EffectsPanel effectsPanel;
    private final ScrollPane effectsPanelSP;
    private final FontDisplay fontDisplay;
    private final ScrollPane fontDisplaySP;
    private final Button saveFontButton;
    private final Button cancelButton;

    private final SimpleIntegerModel[] paddingModels;

    private String fontPath;
    private FontData fontData;
    
    public FontGenDialog(Widget owner) {
        this.charSet = new CharSet();

        effectsPanel = new EffectsPanel();

        effectsPanelSP = new ScrollPane(effectsPanel);
        effectsPanelSP.setTheme("effectsPanel");
        effectsPanelSP.setFixed(ScrollPane.Fixed.HORIZONTAL);
        effectsPanelSP.setExpandContentSize(true);

        unicodeBlocksBox = new BoxLayout(BoxLayout.Direction.VERTICAL);

        Runnable updateCharSetCB = new Runnable() {
            public void run() {
                updateCharset();
            }
        };
        for(Field f : Character.UnicodeBlock.class.getFields()) {
            if(Modifier.isStatic(f.getModifiers()) && f.getType() == Character.UnicodeBlock.class) {
                try {
                    Character.UnicodeBlock block = (UnicodeBlock) f.get(null);
                    CharSetBlockModel charSetBlockModel = new CharSetBlockModel(charSet, block);
                    charSetBlockModel.addCallback(updateCharSetCB);
                    ToggleButton tb = new ToggleButton(charSetBlockModel);
                    tb.setText(beautifyName(block.toString()));
                    tb.setTheme("checkbox");
                    unicodeBlocksBox.add(tb);
                } catch (Throwable ignored) {
                }
            }
        }

        fontPathEF = new EditField();
        fontPathEF.setReadOnly(true);

        selectFontBtn = new Button();
        selectFontBtn.setTheme("selectFontBtn");
        selectFontBtn.addCallback(new Runnable() {
            public void run() {
                selectFont();
            }
        });

        textureSizesModel = new SimpleChangableListModel<Integer>(256, 512, 1024, 2048, 4096);

        textureSizeCB = new ComboBox<Integer>(textureSizesModel);
        textureSizeCB.setSelected(2);
        textureSizeCB.addCallback(new Runnable() {
            public void run() {
                updateTextureSize();
            }
        });

        fontDisplayBgCB = new ComboBox<FontDisplayBG>(new SimpleChangableListModel<FontDisplayBG>(FontDisplayBG.values()));
        fontDisplayBgCB.setSelected(0);
        fontDisplayBgCB.addCallback(new Runnable() {
            public void run() {
                setFontDisplayTheme();
            }
        });

        unicodeBlocksSP = new ScrollPane(unicodeBlocksBox);
        unicodeBlocksSP.setTheme("unicodeBlocks");
        unicodeBlocksSP.setFixed(ScrollPane.Fixed.HORIZONTAL);

        fontSizesModel = new SimpleChangableListModel<Integer>();
        for(int size=8 ; size<24 ; size++) {
            fontSizesModel.addElement(size);
        }
        for(int size=24 ; size<64 ; size += 2) {
            fontSizesModel.addElement(size);
        }
        for(int size=64 ; size<=128 ; size += 4) {
            fontSizesModel.addElement(size);
        }
        fontSizeCB = new ComboBox<Integer>(fontSizesModel);
        fontSizeCB.setSelected(6);
        fontSizeCB.addCallback(new Runnable() {
            public void run() {
                updateFont();
            }
        });

        Runnable updatePaddingCB = new Runnable() {
            public void run() {
                updatePadding();
            }
        };

        paddingModels = new SimpleIntegerModel[5];
        ValueAdjusterInt[] paddingAdjuster = new ValueAdjusterInt[paddingModels.length];
        for(int i=0 ; i<paddingModels.length ; i++) {
            paddingModels[i] = new SimpleIntegerModel(0, 16, 0);
            paddingModels[i].addCallback(updatePaddingCB);
            paddingAdjuster[i] = new ValueAdjusterInt(paddingModels[i]);
        }

        paddingAdjuster[0].setDisplayPrefix("T: ");
        paddingAdjuster[1].setDisplayPrefix("L: ");
        paddingAdjuster[2].setDisplayPrefix("B: ");
        paddingAdjuster[3].setDisplayPrefix("R: ");
        paddingAdjuster[4].setDisplayPrefix("A: ");
        
        effectsPanel.addControl("TTF Font", fontPathEF, selectFontBtn);
        effectsPanel.addControl("Texture size", textureSizeCB);
        effectsPanel.addControl("Font size", fontSizeCB);
        effectsPanel.addControl("Padding", paddingAdjuster);
        effectsPanel.addControl("Preview BG", fontDisplayBgCB);
        effectsPanel.addCollapsible("Unicode blocks", unicodeBlocksSP, null).setExpanded(true);

        effectsPanel.addEffect("Shadow", new BlurShadowEffect());
        effectsPanel.addEffect("Gradient", new GradientEffect());
        effectsPanel.addCallback(new Runnable() {
            public void run() {
                updateEffects();
            }
        });
        
        Widget splitter = new Widget();
        splitter.setTheme("splitter");

        fontDisplay = new FontDisplay();
        fontDisplaySP = new ScrollPane(fontDisplay);
        fontDisplaySP.setTheme("fontDisplay");
        
        saveFontButton = new Button("Save Font");
        saveFontButton.addCallback(new Runnable() {
            public void run() {
            }
        });

        cancelButton = new Button("Cancel");
        cancelButton.addCallback(new Runnable() {
            public void run() {
                cancel();
            }
        });

        SplitPane splitPane = new SplitPane();
        splitPane.setDirection(SplitPane.Direction.HORIZONTAL);
        //splitPane.setReverseSplitPosition(true);
        splitPane.setSplitPosition(300);
        splitPane.add(effectsPanelSP);
        splitPane.add(fontDisplaySP);

        layout = new DialogLayout();
        layout.setTheme("fontgendialog");
        
        DialogLayout.Group hButtons = layout.createSequentialGroup()
                .addGap()
                .addWidget(saveFontButton)
                .addWidget(cancelButton);
        DialogLayout.Group vButtons = layout.createParallelGroup()
                .addWidget(saveFontButton)
                .addWidget(cancelButton);
        
        layout.setHorizontalGroup(layout.createParallelGroup()
                .addWidget(splitPane)
                .addGroup(hButtons));
        layout.setVerticalGroup(layout.createSequentialGroup()
                .addWidget(splitPane)
                .addGroup(vButtons));

        popupWindow = new PopupWindow(owner);
        popupWindow.setTheme("fontGenDialog-popup");
        popupWindow.add(layout);

        setFontDisplayTheme();
        updateTextureSize();
        updateCharset();
        updateFont();
        updatePadding();
        updateEffects();
    }

    public void openPopup() {
        if(popupWindow.openPopup()) {
            GUI gui = popupWindow.getGUI();
            popupWindow.setSize(gui.getWidth()*4/5, gui.getHeight()*4/5);
            popupWindow.setPosition(
                    (gui.getWidth() - popupWindow.getWidth())/2,
                    (gui.getHeight() - popupWindow.getHeight())/2);
        }
    }

    void cancel() {
        popupWindow.closePopup();
    }

    void setFontDisplayTheme() {
        FontDisplayBG fdbg = fontDisplayBgCB.getModel().getEntry(fontDisplayBgCB.getSelected());
        fontDisplay.setTheme(fdbg.theme);
        fontDisplay.reapplyTheme();
    }

    void selectFont() {
        FontSelectPopup fsd = new FontSelectPopup(selectFontBtn);
        fsd.addCallback(new FontSelectPopup.Callback() {
            public void fontSelected(String fontPath, FontData fontData) {
                FontGenDialog.this.fontSelected(fontPath, fontData);
            }
        });
        fsd.openPopup();
    }

    void fontSelected(String fontPath, FontData fontData) {
        this.fontPath = fontPath;
        this.fontData = fontData;

        fontPathEF.setText(fontPath);
        updateFont();
    }

    void updatePadding() {
        Padding padding = new Padding(
                paddingModels[0].getValue(),
                paddingModels[1].getValue(),
                paddingModels[2].getValue(),
                paddingModels[3].getValue(),
                paddingModels[4].getValue());
        fontDisplay.setPadding(padding);
    }

    void updateCharset() {
        fontDisplay.setCharSet(charSet);
    }

    void updateFont() {
        if(fontData != null) {
            int fontSize = fontSizesModel.getEntry(fontSizeCB.getSelected());
            fontDisplay.setFontData(fontData.deriveFont(fontSize));
        } else {
            fontDisplay.setFontData(null);
        }
    }

    void updateTextureSize() {
        int textureSize = textureSizesModel.getEntry(textureSizeCB.getSelected());
        fontDisplay.setTextureSize(textureSize);
    }

    void updateEffects() {
        fontDisplay.setEffects(effectsPanel.getActiveEffects());
    }

    private static String beautifyName(String name) {
        char[] cb = name.toCharArray();
        boolean toLower = false;
        for(int i=0 ; i<cb.length ; i++) {
            if(cb[i] == '_') {
                cb[i] = ' ';
                toLower = false;
            } else if(toLower) {
                cb[i] = Character.toLowerCase(cb[i]);
            } else {
                toLower = true;
            }
        }
        return new String(cb);
    }

    static class CharSetBlockModel extends HasCallback implements BooleanModel {
        private final CharSet charSet;
        private final Character.UnicodeBlock block;

        public CharSetBlockModel(CharSet charSet, UnicodeBlock block) {
            this.charSet = charSet;
            this.block = block;
        }

        public boolean getValue() {
            return charSet.getBlockEnabled(block);
        }

        public void setValue(boolean value) {
            charSet.setBlock(block, value);
            doCallback();
        }
    }

    static enum FontDisplayBG {
        BLACK("Black", "fontdisplay-black"),
        WHITE("White", "fontdisplay-white"),
        CHECKERBOARD("Checker board", "fontdisplay-checkerboard");

        final String text;
        final String theme;

        private FontDisplayBG(String text, String theme) {
            this.text = text;
            this.theme = theme;
        }

        @Override
        public String toString() {
            return text;
        }
    }
}
