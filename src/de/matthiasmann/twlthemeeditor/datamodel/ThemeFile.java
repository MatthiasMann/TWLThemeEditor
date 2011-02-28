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
package de.matthiasmann.twlthemeeditor.datamodel;

import de.matthiasmann.twl.model.Property;
import de.matthiasmann.twl.model.TreeTableNode;
import de.matthiasmann.twlthemeeditor.TestEnv;
import de.matthiasmann.twlthemeeditor.VirtualFile;
import de.matthiasmann.twlthemeeditor.XMLWriter;
import de.matthiasmann.twlthemeeditor.datamodel.operations.CreateChildOperation;
import de.matthiasmann.twlthemeeditor.datamodel.operations.CreateNewFontDef;
import de.matthiasmann.twlthemeeditor.datamodel.operations.CreateNewImages;
import de.matthiasmann.twlthemeeditor.datamodel.operations.CreateNewInclude;
import de.matthiasmann.twlthemeeditor.datamodel.operations.CreateNewSimple;
import de.matthiasmann.twlthemeeditor.gui.MessageLog;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import org.jdom.Content;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import org.xmlpull.v1.XmlPullParser;

/**
 *
 * @author Matthias Mann
 */
public class ThemeFile implements VirtualFile {

    private final MessageLog messageLog;
    private final ThemeFile rootThemeFile;
    private final TestEnv env;
    private final URL url;
    private final Document document;
    private final Runnable xmlChangedCB;
    private final Runnable propertyChangedCB;

    private ThemeTreeNode treeNode;
    private boolean modified;
    private boolean elementsUpgraded;
    private boolean hadErrors;

    @SuppressWarnings("LeakingThisInConstructor")
    public ThemeFile(MessageLog messageLog, ThemeFile rootThemeFile, TestEnv env, URL url, Runnable xmlChangedCB) throws IOException {
        this.messageLog = messageLog;
        this.rootThemeFile = rootThemeFile;
        this.env = env;
        this.url = url;
        this.xmlChangedCB = xmlChangedCB;
        this.propertyChangedCB = new Runnable() {
            public void run() {
                setModified(true);
                ThemeFile.this.xmlChangedCB.run();
            }
        };
        
        document = Utils.loadDocument(url);
        document.setProperty(ThemeFile.class.getName(), this);
        env.registerFile(this);
    }

    public static ThemeFile getThemeFile(Content content) {
        Document document = content.getDocument();
        if(document != null) {
            return (ThemeFile)document.getProperty(ThemeFile.class.getName());
        }
        return null;
    }

    public boolean isModified() {
        return modified;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }
    
    public void writeTo(OutputStream out) throws IOException {
        Writer w = new XMLWriter(new OutputStreamWriter(out, "UTF8"));
        new XMLOutputter().output(document, w);
        w.flush();
    }

    public TestEnv getEnv() {
        return env;
    }

    public Element getRootElement() {
        return document.getRootElement();
    }

    public ThemeFile getRootThemeFile() {
        return rootThemeFile == null ? this : rootThemeFile;
    }

    public URL getURL() {
        return url;
    }
    
    public URL getURL(String file) throws MalformedURLException {
        return new URL(url, file);
    }

    public ThemeFile createThemeFile(String file) throws IOException {
        return new ThemeFile(messageLog, this, env, getURL(file), xmlChangedCB);
    }

    public URL getVirtualURL() throws MalformedURLException {
        return env.getURL(url.getFile());
    }

    public void log(MessageLog.Entry entry) {
        messageLog.add(entry);
    }

    private static final MessageLog.Category CAT_LOAD_ERROR =
            new MessageLog.Category("theme load error", MessageLog.CombineMode.NONE, DecoratedText.ERROR);

    public void logError(String msg, String detailMsg, Throwable ex) {
        hadErrors = true;
        messageLog.add(new MessageLog.Entry(CAT_LOAD_ERROR, msg, detailMsg, ex));
    }

    private static final MessageLog.Category CAT_UPGRADED =
            new MessageLog.Category("theme upgrades", MessageLog.CombineMode.NONE, DecoratedText.WARNING);

    public void elementUpgraded() {
        setModified(true);
        if(!elementsUpgraded) {
            elementsUpgraded = true;
            messageLog.add(new MessageLog.Entry(CAT_UPGRADED, "Elements have been upgraded",
                    "The theme file '" + url.getPath() + "' has been converted to a new format. " +
                    "Saving the theme will require a up to date TWL version to parse it.", null));
        }
    }

    public boolean isHadErrors() {
        return hadErrors;
    }
    
    protected void addChildren(ThemeTreeNode node) throws IOException {
        this.treeNode = node;
        node.addChildren(this, document.getRootElement(), new DomWrapper() {
            public TreeTableNode wrap(ThemeFile themeFile, ThemeTreeNode parent, Element element) throws IOException {
                String tagName = element.getName();

                if("images".equals(tagName)) {
                    return new Images(parent, element, themeFile);
                }
                if("textures".equals(tagName)) {
                    element.setName("images");
                    setModified(true);
                    return new Images(parent, element, themeFile);
                }
                if("include".equals(tagName)) {
                    return new Include(parent, element, themeFile);
                }
                if("fontDef".equals(tagName)) {
                    return new FontDef(themeFile, parent, element);
                }
                if("theme".equals(tagName)) {
                    return new Theme(themeFile, parent, element);
                }
                if("inputMapDef".equals(tagName)) {
                    return new InputMapDef(themeFile, parent, element);
                }
                return null;
            }
        });
    }

    protected void addCreateOperations(List<CreateChildOperation> operations, ThemeTreeNode node) {
        addCreateThemeOperation(operations, node, document.getRootElement());
        operations.add(new CreateNewSimple(node, document.getRootElement(), "inputMapDef", "name", "changeMe"));
        operations.add(new CreateNewImages(node, document.getRootElement()));
        operations.add(new CreateNewFontDef(node, document.getRootElement()));
        operations.add(new CreateNewInclude(node, document.getRootElement()));
    }

    static void addCreateThemeOperation(List<CreateChildOperation> operations, ThemeTreeNode node, Element parent) {
        operations.add(new CreateNewSimple(node, parent, "theme", "ref", "-defaults"));
    }

    void registerProperty(Property<?> property) {
        property.addValueChangedCallback(propertyChangedCB);
    }

    public String getVirtualFileName() {
        return url.getFile();
    }

    @SuppressWarnings("unchecked")
    public Object getContent(Class<?> type) throws IOException {
        if(type == XmlPullParser.class && treeNode != null) {
            DomXPPParser xpp = new DomXPPParser(getVirtualFileName());
            ThemeLoadErrorTracker.register(xpp);
            Element rootElement = document.getRootElement();
            Utils.addToXPP(xpp, rootElement.getName(), treeNode, rootElement.getAttributes());
            return xpp;
        }
        return null;
    }

    public InputStream openStream() throws IOException {
        throw new IOException("Call getContent().");
    }

}
