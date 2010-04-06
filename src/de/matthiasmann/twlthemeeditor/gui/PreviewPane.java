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

import de.matthiasmann.twl.renderer.Image;
import de.matthiasmann.twl.Button;
import de.matthiasmann.twl.DialogLayout;
import de.matthiasmann.twl.GUI;
import de.matthiasmann.twl.Label;
import de.matthiasmann.twl.PopupWindow;
import de.matthiasmann.twl.ScrollPane;
import de.matthiasmann.twl.TextArea;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.SimpleTextAreaModel;
import de.matthiasmann.twl.model.TextAreaModel;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;

/**
 *
 * @author Matthias Mann
 */
public class PreviewPane extends DialogLayout {

    private final PreviewWidget previewWidget;
    private final Label labelErrorDisplay;
    private final Button btnClearStackTrace;
    private final Button btnShowStackTrace;

    public PreviewPane() {
        this.previewWidget = new PreviewWidget();
        this.labelErrorDisplay = new Label() {
            @Override
            public int getMinWidth() {
                return 0;
            }
        };
        this.btnClearStackTrace = new Button("Clear");
        this.btnShowStackTrace = new Button("Stack Trace");

        labelErrorDisplay.setTheme("errorDisplay");
        labelErrorDisplay.setClip(true);
        btnClearStackTrace.setEnabled(false);
        btnShowStackTrace.setEnabled(false);

        setHorizontalGroup(createParallelGroup()
                .addWidget(previewWidget)
                .addGroup(createSequentialGroup(labelErrorDisplay, btnClearStackTrace, btnShowStackTrace)));
        setVerticalGroup(createSequentialGroup()
                .addWidget(previewWidget)
                .addGroup(createParallelGroup(labelErrorDisplay, btnClearStackTrace, btnShowStackTrace)));

        previewWidget.getExceptionHolder().addCallback(new Runnable() {
            public void run() {
                updateException();
            }
        });
        btnClearStackTrace.addCallback(new Runnable() {
            public void run() {
                clearException();
            }
        });
        btnShowStackTrace.addCallback(new Runnable() {
            public void run() {
                showStackTrace();
            }
        });
    }

    public void setURL(URL url) {
        previewWidget.setURL(url);
    }

    public void reloadTheme() {
        previewWidget.reloadTheme();
    }

    public Image getImage(String name) {
        return previewWidget.getImage(name);
    }

    public Object getThemeLoadErrorLocation() {
        return previewWidget.getThemeLoadErrorLocation();
    }

    public void removeCallback(Runnable cb) {
        previewWidget.getExceptionHolder().removeCallback(cb);
    }

    public void addCallback(Runnable cb) {
        previewWidget.getExceptionHolder().addCallback(cb);
    }

    public void setTestWidgetChangedCB(Runnable testWidgetChangedCB) {
        previewWidget.setTestWidgetChangedCB(testWidgetChangedCB);
    }

    public Widget getTestWidget() {
        return previewWidget.getTestWidget();
    }
    
    void updateException() {
        int nr = selectException();
        if(nr >= 0) {
            ExceptionHolder exceptionHolder = previewWidget.getExceptionHolder();
            Throwable ex = exceptionHolder.getException(nr);
            labelErrorDisplay.setText(exceptionHolder.getExceptionName(nr) + " exception: " + ex.getMessage());
            btnClearStackTrace.setEnabled(true);
            btnShowStackTrace.setEnabled(true);
        } else {
            labelErrorDisplay.setText("");
            btnClearStackTrace.setEnabled(false);
            btnShowStackTrace.setEnabled(false);
        }
    }

    void clearException() {
        clearException(selectException());
    }

    void clearException(int nr) {
        if(nr >= 0) {
            previewWidget.clearException(nr);
        }
    }

    void showStackTrace() {
        final int nr = selectException();
        if(nr < 0) {
            return;
        }
        
        final Throwable ex = previewWidget.getExceptionHolder().getException(nr);

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        pw.flush();

        TextAreaModel model = new SimpleTextAreaModel(sw.toString());

        TextArea textArea = new TextArea(model);
        ScrollPane scrollPane = new ScrollPane(textArea);
        scrollPane.setFixed(ScrollPane.Fixed.HORIZONTAL);

        Button btnClear = new Button("Clear Exception and Close");
        Button btnClose = new Button("Close");

        DialogLayout layout = new DialogLayout();
        layout.setHorizontalGroup(layout.createParallelGroup()
                .addWidget(scrollPane)
                .addGroup(layout.createSequentialGroup().addGap().addWidgets(btnClear, btnClose)));
        layout.setVerticalGroup(layout.createSequentialGroup()
                .addWidget(scrollPane)
                .addGroup(layout.createParallelGroup().addWidgets(btnClear, btnClose)));

        final PopupWindow popupWindow = new PopupWindow(this);
        popupWindow.setTheme("previewExceptionPopup");
        popupWindow.setCloseOnClickedOutside(false);
        popupWindow.setCloseOnEscape(true);
        popupWindow.add(layout);

        btnClear.addCallback(new Runnable() {
            public void run() {
                clearException(nr);
                popupWindow.closePopup();
            }
        });
        btnClose.addCallback(new Runnable() {
            public void run() {
                popupWindow.closePopup();
            }
        });

        GUI gui = getGUI();
        popupWindow.setSize(gui.getWidth()*4/5, gui.getHeight()*4/5);
        popupWindow.setPosition(
                (gui.getWidth() - popupWindow.getWidth())/2,
                (gui.getHeight() - popupWindow.getHeight())/2);
        popupWindow.openPopup();
    }

    private int selectException() {
        return previewWidget.getExceptionHolder().getHighestPriority();
    }

    public void setWidgetFactory(TestWidgetFactory factory) {
        previewWidget.setWidgetFactory(factory);
    }

}
