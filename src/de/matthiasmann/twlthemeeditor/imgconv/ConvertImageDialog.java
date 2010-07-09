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
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.SimpleChangableListModel;
import de.matthiasmann.twlthemeeditor.fontgen.gui.EffectsPanel;

/**
 *
 * @author Matthias Mann
 */
public class ConvertImageDialog extends PopupWindow {

    private final EditField imagePathEF;
    private final Button selectImageBtn;
    private final SimpleChangableListModel<Integer> textureSizesModel;
    private final ComboBox<Integer> textureSizeCB;
    private final Label statusBar;
    private final Button saveImageButton;
    private final Button closeButton;

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

        textureSizesModel = new SimpleChangableListModel<Integer>(256, 512, 1024, 2048, 4096);

        textureSizeCB = new ComboBox<Integer>(textureSizesModel);
        textureSizeCB.setSelected(2);
        textureSizeCB.addCallback(new Runnable() {
            public void run() {
                updateTextureSize();
            }
        });

        EffectsPanel effectsPanel = new EffectsPanel();
        ScrollPane effectsPanelSP = new ScrollPane(effectsPanel);
        effectsPanelSP.setTheme("effectsPanel");
        effectsPanelSP.setFixed(ScrollPane.Fixed.HORIZONTAL);
        effectsPanelSP.setExpandContentSize(true);
        
        effectsPanel.addControl("Image", imagePathEF, selectImageBtn);
        effectsPanel.addControl("Texture size", textureSizeCB);

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
        //splitPane.add(imageDisplaySP);
        
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
    }

    void selectImage() {
        ImageSelectPopup isp = new ImageSelectPopup(selectImageBtn);
        isp.addCallback(new ImageSelectPopup.Callback() {
            public void imageSelected(String imagePath, ImageData imageData) {
            }
        });
        isp.openPopup();
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

    void saveImage() {
        
    }

    void updateTextureSize() {
        //fontDisplay.setTextureSize(getTextureSize());
    }

    private Integer getTextureSize() {
        return textureSizesModel.getEntry(textureSizeCB.getSelected());
    }

}
