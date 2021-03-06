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

import de.matthiasmann.twl.DialogLayout;
import de.matthiasmann.twl.GUI;
import de.matthiasmann.twl.Label;
import de.matthiasmann.twl.PopupWindow;
import de.matthiasmann.twl.ProgressBar;
import de.matthiasmann.twl.Widget;

/**
 *
 * @author Matthias Mann
 */
public class ProgressDialog extends PopupWindow {

    final Label titleLabel;
    final Label messageLabel;
    final ProgressBar progressBar;

    public ProgressDialog(Widget owner) {
        super(owner);

        titleLabel = new Label(" ");
        titleLabel.setTheme("title");

        messageLabel = new Label(" ");
        messageLabel.setTheme("message");

        progressBar = new ProgressBar();
        progressBar.setClip(true);

        DialogLayout l = new DialogLayout();
        l.setHorizontalGroup(l.createParallelGroup(titleLabel, messageLabel, progressBar));
        l.setVerticalGroup(l.createSequentialGroup()
                .addWidget(titleLabel)
                .addGap("title-message")
                .addWidget(messageLabel)
                .addGap("message-progressbar")
                .addWidget(progressBar));

        add(l);
        setCloseOnEscape(false);
        setCloseOnClickedOutside(false);
    }

    public void setTitle(String title) {
        titleLabel.setText(title);
    }

    public void setMessage(final String msg) {
        invokeLater(new Runnable() {
            public void run() {
                messageLabel.setText(msg);
            }
        });
    }

    public void setIndeterminate(final String text) {
        invokeLater(new Runnable() {
            public void run() {
                progressBar.setText(text);
                progressBar.setIndeterminate();
            }
        });
    }

    public void setProgress(final String text, final float value) {
        invokeLater(new Runnable() {
            public void run() {
                progressBar.setText(text);
                progressBar.setValue(value);
            }
        });
    }

    private void invokeLater(Runnable r) {
        GUI g = getGUI();
        if(g != null) {
            g.invokeLater(r);
        }
    }
}
