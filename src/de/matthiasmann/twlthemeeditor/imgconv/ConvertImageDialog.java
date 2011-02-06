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

import de.matthiasmann.twl.Button;
import de.matthiasmann.twl.ComboBox;
import de.matthiasmann.twl.DialogLayout;
import de.matthiasmann.twl.EditField;
import de.matthiasmann.twl.Label;
import de.matthiasmann.twl.PopupWindow;
import de.matthiasmann.twl.ScrollPane;
import de.matthiasmann.twl.SplitPane;
import de.matthiasmann.twl.ToggleButton;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.SimpleBooleanModel;
import de.matthiasmann.twl.model.SimpleChangableListModel;
import de.matthiasmann.twlthemeeditor.datamodel.DecoratedText;
import de.matthiasmann.twlthemeeditor.fontgen.gui.EffectsPanel;
import de.matthiasmann.twlthemeeditor.fontgen.gui.FontGenDialog;
import de.matthiasmann.twlthemeeditor.gui.DecoratedTextRenderer;
import de.matthiasmann.twlthemeeditor.gui.SaveFileSelector;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

/**
 *
 * @author Matthias Mann
 */
public final class ConvertImageDialog extends PopupWindow {

    private static final String IMAGECONV_FILE_SELECTOR_KEY = "saveImage";

    private final EditField imagePathEF;
    private final Button selectImageBtn;
    private final SimpleBooleanModel excludeZeroDelayFramesModel;
    private final ToggleButton excludeZeroDelayFramesBtn;
    private final ComboBox<ImageDisplayBG> imageDisplayBgCB;
    private final ImageDisplay imageDisplay;
    private final Label statusBar;
    private final Button saveImageButton;
    private final Button closeButton;

    private String imagePath;
    private ImageData imageData;
    
    public ConvertImageDialog(Widget owner) {
        super(owner);

        imagePathEF = new EditField();
        imagePathEF.setReadOnly(true);

        selectImageBtn = new Button();
        selectImageBtn.setTheme("selectImageBtn");
        selectImageBtn.addCallback(new Runnable() {
            public void run() {
                selectImage();
            }
        });

        excludeZeroDelayFramesModel = new SimpleBooleanModel(true);
        excludeZeroDelayFramesModel.addCallback(new Runnable() {
            public void run() {
                updateExcludeZeroDelayFrames();
            }
        });

        excludeZeroDelayFramesBtn = new ToggleButton(excludeZeroDelayFramesModel);
        excludeZeroDelayFramesBtn.setTheme("checkbox");
        excludeZeroDelayFramesBtn.setText("Exclude frames with 0 delay");

        imageDisplayBgCB = new ComboBox<ImageDisplayBG>(new SimpleChangableListModel<ImageDisplayBG>(ImageDisplayBG.values()));
        imageDisplayBgCB.setSelected(0);
        imageDisplayBgCB.addCallback(new Runnable() {
            public void run() {
                setImageDisplayTheme();
            }
        });

        EffectsPanel effectsPanel = new EffectsPanel();
        ScrollPane effectsPanelSP = new ScrollPane(effectsPanel);
        effectsPanelSP.setTheme("effectsPanel");
        effectsPanelSP.setFixed(ScrollPane.Fixed.HORIZONTAL);
        effectsPanelSP.setExpandContentSize(true);
        
        effectsPanel.addControl("Image", imagePathEF, selectImageBtn);
        effectsPanel.addControl(excludeZeroDelayFramesBtn);

        imageDisplay = new ImageDisplay(new Runnable() {
            public void run() {
                updateStatusBar();
            }
        });

        ScrollPane imageDisplaySP = new ScrollPane(imageDisplay);
        imageDisplaySP.setTheme("imageDisplay");

        saveImageButton = new Button("Save Image");
        saveImageButton.addCallback(new Runnable() {
            public void run() {
                saveImage();
            }
        });

        closeButton = new Button("Close");
        closeButton.addCallback(new Runnable() {
            public void run() {
                closePopup();
            }
        });

        statusBar = new Label();
        statusBar.setTheme("statusBar");

        SplitPane splitPane = new SplitPane();
        splitPane.setDirection(SplitPane.Direction.HORIZONTAL);
        splitPane.setSplitPosition(370);
        splitPane.add(effectsPanelSP);
        splitPane.add(imageDisplaySP);
        effectsPanel.addControl("Preview BG", imageDisplayBgCB);
        
        DialogLayout layout = new DialogLayout();

        DialogLayout.Group hButtons = layout.createSequentialGroup()
                .addWidget(statusBar)
                .addWidget(saveImageButton)
                .addWidget(closeButton);
        DialogLayout.Group vButtons = layout.createParallelGroup()
                .addWidget(statusBar)
                .addWidget(saveImageButton)
                .addWidget(closeButton);

        layout.setHorizontalGroup(layout.createParallelGroup()
                .addWidget(splitPane)
                .addGroup(hButtons));
        layout.setVerticalGroup(layout.createSequentialGroup()
                .addWidget(splitPane)
                .addGroup(vButtons));

        add(layout);
        setCloseOnClickedOutside(false);

        setImageDisplayTheme();
        updateExcludeZeroDelayFrames();
        updateStatusBar();
    }
    
    @Override
    public boolean openPopup() {
        if(super.openPopup()) {
            int width = getGUI().getInnerWidth();
            int height = getGUI().getInnerHeight();
            setSize(width*4/5, height*4/5);
            setPosition(
                    width/2 - getWidth()/2,
                    height/2 - getHeight()/2);
            return true;
        }
        return false;
    }

    void setImageDisplayTheme() {
        ImageDisplayBG idbg = imageDisplayBgCB.getModel().getEntry(imageDisplayBgCB.getSelected());
        imageDisplay.setTheme(idbg.theme);
        imageDisplay.reapplyTheme();
    }

    void selectImage() {
        ImageSelectPopup isp = new ImageSelectPopup(selectImageBtn);
        isp.addCallback(new ImageSelectPopup.Callback() {
            public void imageSelected(String imagePath, ImageData imageData) {
                ConvertImageDialog.this.imageSelected(imagePath, imageData);
            }
        });
        isp.openPopup();
    }

    void imageSelected(String imagePath, ImageData imageData) {
        this.imagePath = imagePath;
        this.imageData = imageData;

        imagePathEF.setText(imagePath);
        imageDisplay.setImageData(imageData);
    }

    void updateExcludeZeroDelayFrames() {
        imageDisplay.setSkipZeroDelayFrames(excludeZeroDelayFramesModel.getValue());
    }

    void saveImage() {
        final ImageGenerator imageGen = imageDisplay.getLastImageGen();
        if(imageGen == null) {
            return;
        }

        SaveFileSelector sfs = new SaveFileSelector(saveImageButton,
                Preferences.userNodeForPackage(ConvertImageDialog.class), IMAGECONV_FILE_SELECTOR_KEY,
                "PNG files", ".png", new SaveFileSelector.Callback() {
            public File[] getFilesCreatedForFileName(File file) {
                return imageGen.getFilesCreatedForName(file);
            }
            public void fileNameSelected(File file) {
                try {
                    imageGen.write(file);
                } catch(IOException ex) {
                    Logger.getLogger(FontGenDialog.class.getName()).log(Level.SEVERE, "Cound not save image", ex);
                }
            }
            public void canceled() {
            }
        });
        sfs.openPopup();
    }

    void updateStatusBar() {
        ImageGenerator imageGen = imageDisplay.getLastImageGen();
        if(imageGen == null) {
            setStatusBar("Select an image", DecoratedText.ERROR);
            return;
        }
        if(imageGen.getNumFrames() == 0) {
            setStatusBar("No frames found in selected image", DecoratedText.ERROR);
            return;
        }
        if(imageGen.isCutoff()) {
            setStatusBar("Not all frames could fit onto the maximum texture size of "
                    + ImageGenerator.MAX_TEXTURE_SIZE, DecoratedText.WARNING);
        }
        setStatusBar("Used texture size is " + imageGen.getWidth() + "x" + imageGen.getHeight(), 0);
    }

    private void setStatusBar(String text, int flags) {
        statusBar.setText(text);
        DecoratedTextRenderer.setAnimationState(statusBar.getAnimationState(), flags);
        saveImageButton.setEnabled((flags & DecoratedText.ERROR) == 0);
    }

    static enum ImageDisplayBG {
        BLACK("Black", "imagedisplay-black"),
        WHITE("White", "imagedisplay-white"),
        CHECKERBOARD("Checker board", "imagedisplay-checkerboard");

        final String text;
        final String theme;

        private ImageDisplayBG(String text, String theme) {
            this.text = text;
            this.theme = theme;
        }

        @Override
        public String toString() {
            return text;
        }
    }
}
