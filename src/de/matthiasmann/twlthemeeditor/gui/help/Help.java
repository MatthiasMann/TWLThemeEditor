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
package de.matthiasmann.twlthemeeditor.gui.help;

import de.matthiasmann.twl.Button;
import de.matthiasmann.twl.DialogLayout;
import de.matthiasmann.twl.PopupWindow;
import de.matthiasmann.twl.Rect;
import de.matthiasmann.twl.ScrollPane;
import de.matthiasmann.twl.TextArea;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.textarea.HTMLTextAreaModel;
import de.matthiasmann.twl.textarea.StyleSheet;
import de.matthiasmann.twl.textarea.TextAreaModel;
import de.matthiasmann.twlthemeeditor.datamodel.DecoratedText;
import de.matthiasmann.twlthemeeditor.gui.MessageLog;
import de.matthiasmann.twlthemeeditor.gui.MessageLog.CombineMode;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import org.lwjgl.Sys;

/**
 *
 * @author matthias
 */
public class Help extends PopupWindow {
    
    private static final MessageLog.Category CAT_HELP = new MessageLog.Category("Help", CombineMode.NONE, DecoratedText.ERROR);
    
    private final MessageLog messageLog;
    private final HTMLTextAreaModel model;
    private final TextArea textArea;
    private final ScrollPane scrollPane;
    private final Button btnClose;
    
    private URL currentURL;

    public Help(Widget owner, MessageLog messageLog) {
        super(owner);
        setTheme("helpDialog");
        
        this.messageLog = messageLog;
        
        currentURL = Help.class.getResource("mainui.html");
        
        model = new HTMLTextAreaModel();
        loadURL(currentURL);
        
        textArea = new TextArea(model);
        textArea.addCallback(new TextArea.Callback() {
            public void handleLinkClicked(String href) {
                Help.this.handleLinkClicked(href);
            }
        });
        
        StyleSheet styleSheet = new StyleSheet();
        try {
            styleSheet.parse(Help.class.getResource("help.css"));
            textArea.setStyleClassResolver(styleSheet);
        } catch(IOException ex) {
            messageLog.add(new MessageLog.Entry(CAT_HELP, "Can't load help style sheet", null, ex));
            textArea.setDefaultStyleSheet();
        }
        
        scrollPane = new ScrollPane(textArea);
        scrollPane.setFixed(ScrollPane.Fixed.HORIZONTAL);
        
        btnClose = new Button();
        btnClose.setTheme("closebutton");
        btnClose.addCallback(new Runnable() {
            public void run() {
                closePopup();
            }
        });
        
        DialogLayout l = new DialogLayout();
        l.setHorizontalGroup(l.createParallelGroup()
                .addWidget(scrollPane)
                .addGroup(l.createSequentialGroup().addGap().addWidget(btnClose).addGap()));
        l.setVerticalGroup(l.createSequentialGroup()
                .addWidget(scrollPane)
                .addGap("scrollpane-closebutton")
                .addWidget(btnClose));
        
        add(l);
    }
    
    public void show() {
        openPopupCentered(
                getOwner().getInnerWidth()*7/8,
                getOwner().getInnerHeight()*7/8);
    }
    
    void handleLinkClicked(String href) {
        System.out.println(href);
        if(href.startsWith("javascript:")) {
            String key = href.substring(11);
            if("copyToClipboard()".equals(key)) {
                //Clipboard.setClipboard(aboutHTML);
            }
        } else if(href.startsWith("http://")) {
            Sys.openURL(href);
        } else if(href.startsWith("#")) {
            TextAreaModel.Element ankor = model.getElementById(href.substring(1));
            if(ankor != null) {
                Rect rect = textArea.getElementRect(ankor);
                if(rect != null) {
                    scrollPane.setScrollPositionY(rect.getY());
                }
            }
        } else {
            gotoURL(href);
        }
    }
    
    private void gotoURL(String href) {
        try {
            URL url = new URL(currentURL, href);
            loadURL(url);
        } catch (MalformedURLException ex) {
            messageLog.add(new MessageLog.Entry(CAT_HELP, "Could not construct URL", href, ex));
        }
    }
    
    private void loadURL(URL url) {
        try {
            model.readHTMLFromURL(url);
            currentURL = url;
        } catch (IOException ex) {
            messageLog.add(new MessageLog.Entry(CAT_HELP, "Could not open page", url.toString(), ex));
        }
    }
    
}
