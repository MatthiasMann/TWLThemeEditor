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

import de.matthiasmann.twl.AnimationState;
import de.matthiasmann.twl.BoxLayout;
import de.matthiasmann.twl.Button;
import de.matthiasmann.twl.ComboBox;
import de.matthiasmann.twl.DialogLayout;
import de.matthiasmann.twl.EditField;
import de.matthiasmann.twl.GUI;
import de.matthiasmann.twl.Label;
import de.matthiasmann.twl.PopupWindow;
import de.matthiasmann.twl.ScrollPane;
import de.matthiasmann.twl.SplitPane;
import de.matthiasmann.twl.ToggleButton;
import de.matthiasmann.twl.ValueAdjusterInt;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.BooleanModel;
import de.matthiasmann.twl.model.HasCallback;
import de.matthiasmann.twl.model.SimpleBooleanModel;
import de.matthiasmann.twl.model.SimpleChangableListModel;
import de.matthiasmann.twl.model.SimpleIntegerModel;
import de.matthiasmann.twlthemeeditor.datamodel.DecoratedText;
import de.matthiasmann.twlthemeeditor.fontgen.CharSet;
import de.matthiasmann.twlthemeeditor.fontgen.FontData;
import de.matthiasmann.twlthemeeditor.fontgen.FontGenerator;
import de.matthiasmann.twlthemeeditor.fontgen.Padding;
import de.matthiasmann.twlthemeeditor.fontgen.effects.BlurShadowEffect;
import de.matthiasmann.twlthemeeditor.fontgen.effects.GradientEffect;
import de.matthiasmann.twlthemeeditor.fontgen.effects.OutlineEffect;
import de.matthiasmann.twlthemeeditor.gui.LoadFileSelector;
import de.matthiasmann.twlthemeeditor.gui.SaveFileSelector;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.Character.UnicodeBlock;
import java.util.HashSet;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

/**
 *
 * @author Matthias Mann
 */
public final class FontGenDialog {
    public static final String FONTGEN_FILE_SELECTOR_KEY = "fontgen_settings";

    private static final String SETTINGS_EXTENSION = ".twlfontgen";

    private final CharSet charSet;
    private final DialogLayout layout;
    private final PopupWindow popupWindow;
    
    private final EditField fontPathEF;
    private final Button selectFontBtn;
    private final SimpleChangableListModel<Integer> textureSizesModel;
    private final ComboBox<Integer> textureSizeCB;
    private final ComboBox<FontDisplayBG> fontDisplayBgCB;
    private final CharSetBlockCB[] unicodeBockCBs;
    private final BoxLayout unicodeBlocksBox;
    private final ScrollPane unicodeBlocksSP;
    private final SimpleIntegerModel fontSizeModel;
    private final ValueAdjusterInt fontSizeAdjuster;
    private final EffectsPanel effectsPanel;
    private final ScrollPane effectsPanelSP;
    private final FontDisplay fontDisplay;
    private final ScrollPane fontDisplaySP;
    private final Button loadSettingsButton;
    private final Button saveSettingsButton;
    private final Button saveFontButton;
    private final Button closeButton;
    private final Label statusBar;

    private final SimpleIntegerModel[] paddingModels;
    private final SimpleBooleanModel manualPaddingModel;

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
        UnicodeBlock[] supportedBlocks = CharSet.getSupportedBlocks();
        unicodeBockCBs = new CharSetBlockCB[supportedBlocks.length];
        for(int i=0,n=supportedBlocks.length ; i<n ; i++) {
            CharSetBlockModel charSetBlockModel = new CharSetBlockModel(charSet, supportedBlocks[i]);
            charSetBlockModel.addCallback(updateCharSetCB);
            unicodeBockCBs[i] = new CharSetBlockCB(charSetBlockModel);
            unicodeBlocksBox.add(unicodeBockCBs[i]);
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

        fontSizeModel = new SimpleIntegerModel(8, 128, 14);
        fontSizeModel.addCallback(new Runnable() {
            public void run() {
                updateFont();
            }
        });
        fontSizeAdjuster = new ValueAdjusterInt(fontSizeModel);

        Runnable updatePaddingCB = new Runnable() {
            public void run() {
                updatePadding();
            }
        };

        manualPaddingModel = new SimpleBooleanModel();
        manualPaddingModel.addCallback(updatePaddingCB);
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
        effectsPanel.addControl("Font size", fontSizeAdjuster);
        effectsPanel.addControl("Preview BG", fontDisplayBgCB);
        effectsPanel.addCollapsible("Unicode blocks", unicodeBlocksSP, null).setExpanded(true);
        effectsPanel.addCollapsible("Manual Padding", paddingAdjuster, manualPaddingModel);

        effectsPanel.addEffect("Shadow", new BlurShadowEffect());
        effectsPanel.addEffect("Gradient", new GradientEffect());
        effectsPanel.addEffect("Outline", new OutlineEffect());
        effectsPanel.addCallback(new Runnable() {
            public void run() {
                updateEffects();
            }
        });

        fontDisplay = new FontDisplay(new Runnable() {
            public void run() {
                updateStatusBar();
            }
        });
        fontDisplaySP = new ScrollPane(fontDisplay);
        fontDisplaySP.setTheme("fontDisplay");

        loadSettingsButton = new Button("Load Settings");
        loadSettingsButton.addCallback(new Runnable() {
            public void run() {
                loadSettings();
            }
        });

        saveSettingsButton = new Button("Save Settings");
        saveSettingsButton.addCallback(new Runnable() {
            public void run() {
                saveSettings();
            }
        });

        saveFontButton = new Button("Save Font");
        saveFontButton.addCallback(new Runnable() {
            public void run() {
                saveFont();
            }
        });

        closeButton = new Button("Close");
        closeButton.addCallback(new Runnable() {
            public void run() {
                close();
            }
        });

        statusBar = new Label();
        statusBar.setTheme("statusBar");

        SplitPane splitPane = new SplitPane();
        splitPane.setDirection(SplitPane.Direction.HORIZONTAL);
        splitPane.setSplitPosition(370);
        splitPane.add(effectsPanelSP);
        splitPane.add(fontDisplaySP);

        layout = new DialogLayout();
        layout.setTheme("fontgendialog");
        
        DialogLayout.Group hButtons = layout.createSequentialGroup()
                .addWidget(loadSettingsButton)
                .addWidget(saveSettingsButton)
                .addWidget(statusBar)
                .addWidget(saveFontButton)
                .addWidget(closeButton);
        DialogLayout.Group vButtons = layout.createParallelGroup()
                .addWidget(loadSettingsButton)
                .addWidget(saveSettingsButton)
                .addWidget(statusBar)
                .addWidget(saveFontButton)
                .addWidget(closeButton);
        
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
        updateStatusBar();
    }

    public void openPopup() {
        if(popupWindow.openPopup()) {
            GUI gui = popupWindow.getGUI();
            popupWindow.setSize(gui.getWidth()*7/8, gui.getHeight()*7/8);
            popupWindow.setPosition(
                    (gui.getWidth() - popupWindow.getWidth())/2,
                    (gui.getHeight() - popupWindow.getHeight())/2);
        }
    }

    void loadSettings() {
        LoadFileSelector lfs = new LoadFileSelector(loadSettingsButton,
                Preferences.userNodeForPackage(FontGenDialog.class), FONTGEN_FILE_SELECTOR_KEY,
                "TWL font generator settings", SETTINGS_EXTENSION, new LoadFileSelector.Callback() {
            public void canceled() {
            }
            public void fileSelected(File file) {
                loadSettings(file);
            }
        });
        lfs.openPopup();
    }

    void saveSettings() {
        SaveFileSelector sfs = new SaveFileSelector(saveSettingsButton,
                Preferences.userNodeForPackage(FontGenDialog.class), FONTGEN_FILE_SELECTOR_KEY,
                "TWL font generator settings", SETTINGS_EXTENSION, new SaveFileSelector.Callback() {
            public File[] getFilesCreatedForFileName(File file) {
                return new File[] { file };
            }
            public void fileNameSelected(File file) {
                saveSettings(file);
            }
            public void canceled() {
            }
        });
        sfs.openPopup();
    }

    private static final String KEY_FONTPATH = "fontPath";
    private static final String KEY_TEXTURESIZE = "textureSize";
    private static final String KEY_FONTSIZE = "fontSize";
    private static final String KEY_PADDING_AUTOMATIC = "padding.automatic";
    private static final String[] KEY_PADDING = {
        "padding.top",
        "padding.left",
        "padding.bottom",
        "padding.right",
        "padding.advance",
    };

    void loadSettings(File file) {
        Properties properties = new Properties();
        try {
            FileInputStream fis = new FileInputStream(file);
            try {
                InputStreamReader isr = new InputStreamReader(fis, "UTF8");
                properties.load(isr);
            } finally {
                fis.close();
            }
        } catch(IOException ex) {
            Logger.getLogger(FontGenDialog.class.getName()).log(Level.SEVERE, "Can't load settings file", ex);
            return;
        }

        fontPath = properties.getProperty(KEY_FONTPATH);
        fontData = null;
        fontPathEF.setText(fontPath);

        {
            int textureSizeEntry = -1;
            try {
                int textureSize = Integer.parseInt(properties.getProperty(KEY_TEXTURESIZE, "0"));
                textureSizeEntry = textureSizesModel.findElement(textureSize);
            } catch (IllegalArgumentException ignore) {
            }
            textureSizeCB.setSelected((textureSizeEntry < 0) ? 2 : textureSizeEntry);
        }

        {
            int fontSize = 14;
            try {
                fontSize = Integer.parseInt(properties.getProperty(KEY_FONTSIZE, "0"));
            } catch (IllegalArgumentException ignore) {
            }
            fontSizeModel.setValue((fontSize <= 0) ? 14 : fontSize);
        }

        manualPaddingModel.setValue(!Boolean.parseBoolean(properties.getProperty(KEY_PADDING_AUTOMATIC, "false")));
        for(int i=0 ; i<5 ; i++) {
            int padding = 0;
            try {
                padding = Integer.parseInt(properties.getProperty(KEY_PADDING[i], "0"));
            } catch (IllegalArgumentException ignore) {
            }
            paddingModels[i].setValue(padding);
        }

        charSet.load(properties);
        effectsPanel.load(properties);

        for(CharSetBlockCB cs : unicodeBockCBs) {
            cs.charSetModel.fireCallback();
        }
        
        if(fontPath != null) {
            try {
                FileInputStream fis = new FileInputStream(fontPath);
                try {
                    fontData = FontData.create(fis, 32);
                } finally {
                    fis.close();
                }
            } catch (Throwable ex) {
                Logger.getLogger(FontGenDialog.class.getName()).log(Level.SEVERE, "Can't load font", ex);
            }
        }

        updateCharset();
        updateFont();
    }

    void saveSettings(File file) {
        Properties properties = new Properties();
        properties.setProperty(KEY_FONTPATH, fontPath);
        properties.setProperty(KEY_TEXTURESIZE, Integer.toString(getTextureSize()));
        properties.setProperty(KEY_FONTSIZE, Integer.toString(fontSizeModel.getValue()));
        charSet.save(properties);
        properties.setProperty(KEY_PADDING_AUTOMATIC, Boolean.toString(!manualPaddingModel.getValue()));
        for(int i=0 ; i<5 ; i++) {
            properties.setProperty(KEY_PADDING[i], Integer.toString(paddingModels[i].getValue()));
        }
        effectsPanel.save(properties);

        try {
            FileOutputStream fos = new FileOutputStream(file);
            try {
                OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF8");
                properties.store(osw, "TWL theme editor font generator settings");
                osw.close();
            } finally {
                fos.close();
            }
        } catch(IOException ex) {
            Logger.getLogger(FontGenDialog.class.getName()).log(Level.SEVERE, "Can't write settings file", ex);
        }
    }

    void saveFont() {
        final FontGenerator fontGen = fontDisplay.getLastFontGen();
        if(fontGen == null) {
            return;
        }

        SaveFileSelector sfs = new SaveFileSelector(saveSettingsButton,
                Preferences.userNodeForPackage(FontGenDialog.class), FONTGEN_FILE_SELECTOR_KEY,
                "BMFont files", ".fnt", new SaveFileSelector.Callback() {
            public File[] getFilesCreatedForFileName(File file) {
                return fontGen.getFilesCreatedForName(file);
            }
            public void fileNameSelected(File file) {
                try {
                    fontGen.write(file);
                } catch(IOException ex) {
                    Logger.getLogger(FontGenDialog.class.getName()).log(Level.SEVERE, "Cound not save font", ex);
                }
            }
            public void canceled() {
            }
        });
        sfs.openPopup();
    }

    void close() {
        popupWindow.closePopup();
    }

    void updateStatusBar() {
        FontGenerator fontGen = fontDisplay.getLastFontGen();
        if(fontGen == null) {
            setStatusBar("Select a font", DecoratedText.ERROR);
            return;
        }
        int usedTextureHeight = fontGen.getUsedTextureHeight();
        if(usedTextureHeight == 0) {
            setStatusBar("Select unicode blocks to include", DecoratedText.ERROR);
            return;
        }
        Integer textureSize = getTextureSize();
        if(usedTextureHeight > textureSize) {
            setStatusBar("Not all characters could fit onto the selected texture size (need "
                    + (usedTextureHeight - textureSize) + " lines more)", DecoratedText.ERROR);
            return;
        }
        setStatusBar("Used " + usedTextureHeight + " of " + textureSize + " lines", 0);
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
        if(manualPaddingModel.getValue()) {
            Padding padding = new Padding(
                    paddingModels[0].getValue(),
                    paddingModels[1].getValue(),
                    paddingModels[2].getValue(),
                    paddingModels[3].getValue(),
                    paddingModels[4].getValue());
            fontDisplay.setPaddingManual(padding);
        } else {
            fontDisplay.setPaddingAutomatic();
        }
    }

    void updateCharset() {
        fontDisplay.setCharSet(charSet);
    }

    void updateFont() {
        if(fontData != null) {
            HashSet<UnicodeBlock> definedBlocks = fontData.getDefinedBlocks();
            for(CharSetBlockCB cb : unicodeBockCBs) {
                cb.setEnabled(definedBlocks.contains(cb.charSetModel.block));
            }
            fontDisplay.setFontData(fontData.deriveFont(fontSizeModel.getValue()));
        } else {
            fontDisplay.setFontData(null);
        }
    }

    void updateTextureSize() {
        fontDisplay.setTextureSize(getTextureSize());
    }

    private Integer getTextureSize() {
        return textureSizesModel.getEntry(textureSizeCB.getSelected());
    }

    void updateEffects() {
        fontDisplay.setEffects(effectsPanel.getActiveEffects());
    }

    private void setStatusBar(String text, int flags) {
        statusBar.setText(text);
        AnimationState animState = statusBar.getAnimationState();
        animState.setAnimationState("error", (flags & DecoratedText.ERROR) != 0);
        animState.setAnimationState("warning", (flags & DecoratedText.WARNING) != 0);
    }

    static class CharSetBlockModel extends HasCallback implements BooleanModel {
        final CharSet charSet;
        final Character.UnicodeBlock block;

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

        void fireCallback() {
            super.doCallback();
        }
    }

    static class CharSetBlockCB extends ToggleButton {
        final CharSetBlockModel charSetModel;

        public CharSetBlockCB(CharSetBlockModel charSetModel) {
            super(charSetModel);
            this.charSetModel = charSetModel;
            setText(getName(charSetModel.block));
            setTheme("checkbox");
        }

        public CharSetBlockModel getCharSetModel() {
            return charSetModel;
        }

        private static String getName(Character.UnicodeBlock block) {
            char[] cb = block.toString().toCharArray();
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
