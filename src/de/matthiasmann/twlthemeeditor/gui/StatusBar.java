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

import de.matthiasmann.twl.AnimationState;
import de.matthiasmann.twl.Button;
import de.matthiasmann.twl.DialogLayout;
import de.matthiasmann.twl.GUI;
import de.matthiasmann.twl.Label;
import de.matthiasmann.twl.PopupWindow;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twlthemeeditor.datamodel.DecoratedText;
import java.text.DateFormat;

/**
 *
 * @author Matthias Mann
 */
public class StatusBar extends DialogLayout {

    static final DateFormat TIME_FORMAT = DateFormat.getTimeInstance();

    private final MessageLog messageLog;
    private final Button btnDiscard;
    private final Button btnDetails;
    private final Label lTime;
    private final Label lMessage;

    private MessageLog.Entry displayedEntry;

    public StatusBar(MessageLog messageLog) {
        this.messageLog = messageLog;

        messageLog.addCallback(new Runnable() {
            public void run() {
                newMessages();
            }
        });

        btnDiscard = new Button("Discard");
        btnDiscard.addCallback(new Runnable() {
            public void run() {
                discardMessage();
            }
        });

        btnDetails = new Button("Details");
        btnDetails.addCallback(new Runnable() {
            public void run() {
                openDetailsDialog();
            }
        });

        lTime = new Label();
        lTime.setTheme("time");

        lMessage = new Label();
        lMessage.setTheme("message");

        displayedEntry = messageLog.getLatestMessage();
        updateStatusBar();
    }

    void newMessages() {
        final MessageLog.Entry latestMessage = messageLog.getLatestMessage();
        if(displayedEntry != latestMessage) {
            displayedEntry = latestMessage;
            updateStatusBar();
        }
    }

    void discardMessage() {
        messageLog.remove(displayedEntry);
    }
    
    private void updateStatusBar() {
        setVerticalGroup(null); // stop layout
        removeAllChildren();

        Group horz = createSequentialGroup();
        Group vert = createParallelGroup();

        if(displayedEntry != null) {
            lTime.setText(TIME_FORMAT.format(displayedEntry.getTime()));
            lMessage.setText(displayedEntry.getMessage());

            setAnimState(lTime, displayedEntry.getCategory().getFlags());
            setAnimState(lMessage, displayedEntry.getCategory().getFlags());

            horz.addWidget(lTime).addWidget(lMessage).addGap().addWidget(btnDiscard).addWidget(btnDetails);
            vert.addWidget(lTime).addWidget(lMessage).addWidget(btnDiscard).addWidget(btnDetails);
        }

        setHorizontalGroup(horz);
        setVerticalGroup(vert);
    }

    private void setAnimState(Widget w, int flags) {
        AnimationState as = w.getAnimationState();
        as.setAnimationState("error", (flags & DecoratedText.ERROR) != 0);
        as.setAnimationState("warning", (flags & DecoratedText.WARNING) != 0);
    }
    
    public void openDetailsDialog() {
        MessageDialog msgDlg = new MessageDialog(messageLog, displayedEntry);

        final PopupWindow popupWindow = new PopupWindow(this);
        popupWindow.setTheme("messagePopup");
        popupWindow.setCloseOnClickedOutside(false);
        popupWindow.setCloseOnEscape(true);
        popupWindow.add(msgDlg);

        msgDlg.addCloseCallback(new Runnable() {
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

}
