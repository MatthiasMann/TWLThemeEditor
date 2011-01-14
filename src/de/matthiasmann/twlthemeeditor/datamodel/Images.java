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
package de.matthiasmann.twlthemeeditor.datamodel;

import de.matthiasmann.twl.Dimension;
import de.matthiasmann.twl.model.TreeTableNode;
import de.matthiasmann.twl.renderer.lwjgl.PNGDecoder;
import de.matthiasmann.twlthemeeditor.TestEnv;
import de.matthiasmann.twlthemeeditor.VirtualFile;
import de.matthiasmann.twlthemeeditor.datamodel.operations.CreateChildOperation;
import de.matthiasmann.twlthemeeditor.datamodel.operations.CreateNewSimple;
import de.matthiasmann.twlthemeeditor.datamodel.operations.CreateNewArea;
import de.matthiasmann.twlthemeeditor.properties.AttributeProperty;
import de.matthiasmann.twlthemeeditor.properties.HasProperties;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jdom.Element;

/**
 *
 * @author Matthias Mann
 */
public class Images extends ThemeTreeNode implements HasProperties {

    private Dimension textureDimensions;
    private AttributeProperty commentProperty;

    protected VirtualFile textureVirtualFile;
    
    Images(TreeTableNode parent, Element element, ThemeFile themeFile) throws IOException {
        super(themeFile, parent, element);
        
        registerTextureURL();
    }

    final void registerTextureURL() throws IOException {
        TestEnv env = themeFile.getEnv();
        if(textureVirtualFile != null) {
            env.unregisterFile(textureVirtualFile);
            textureVirtualFile = null;
        }

        URL textureURL = getTextureURL();
        if(textureURL != null) {
            textureVirtualFile = env.registerFile(textureURL);

            InputStream textureStream = textureURL.openStream();
            try {
                PNGDecoder decoder = new PNGDecoder(textureStream);
                textureDimensions = new Dimension(decoder.getWidth(), decoder.getHeight());
            } finally {
                textureStream.close();
            }
        }

        commentProperty = new AttributeProperty(element, "comment", "Comment", true);
        addProperty(commentProperty);
    }

    public URL getTextureURL() throws MalformedURLException {
        String file = getFile();
        if(file != null) {
            return themeFile.getURL(file);
        } else {
            return null;
        }
    }

    public void updateTextureDimension(URL url, int width, int height) {
        if(width != textureDimensions.getX() && height != textureDimensions.getY()) {
            try {
                if(url.equals(getTextureURL())) {
                    textureDimensions = new Dimension(width, height);
                }
            } catch(MalformedURLException ex) {
                Logger.getLogger(Images.class.getName()).log(Level.SEVERE, "Could not compare URLs", ex);
            }
        }
    }

    public String getFile() {
        return element.getAttributeValue("file");
    }

    public String getFormat() {
        return element.getAttributeValue("format");
    }

    @Override
    public String toString() {
        return "[Images file=\""+getFile()+"\"]";
    }

    @Override
    public String getName() {
        return getFile();
    }

    @Override
    protected String getIcon() {
        return "images";
    }

    @Override
    public String getDisplayName() {
        String comment = commentProperty.getPropertyValue();
        String name = getName();

        if(comment != null) {
            comment = comment.trim();
        }

        if(comment != null && comment.length() > 0) {
            if(name != null) {
                return comment + " (" + name + ")";
            }
            return comment;
        } else if(name != null) {
            return name;
        }
        
        return "<add comment>";
    }

    public Kind getKind() {
        return Kind.NONE;
    }

    @Override
    protected String getType() {
        return "PNG";
    }

    public Dimension getTextureDimensions() {
        return textureDimensions;
    }


    private static final String[] ALLOWED_CHILDREN = {
        "area", "select", "composed", "grid", "animation", "alias"
    };
    public static boolean isAllowedChildImage(String tag) {
        for(String allowed : ALLOWED_CHILDREN) {
            if(allowed.equals(tag)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canPasteElement(Element element) {
        String tag = element.getName();
        return isAllowedChildImage(tag) || "cursor".equals(tag);
    }

    @Override
    public boolean childrenNeedName() {
        return true;
    }

    public void addChildren() throws IOException {
        addChildren(themeFile, element, Image.getImageDomWrapper(this));
    }

    @SuppressWarnings("unchecked")
    public void addToXPP(DomXPPParser xpp) {
        Utils.addToXPP(xpp, element.getName(), this, element.getAttributes());
    }

    @Override
    public List<CreateChildOperation> getCreateChildOperations() {
        List<CreateChildOperation> operations = super.getCreateChildOperations();
        addCreateImageOperations(operations, this);
        return operations;
    }

    public static void addCreateImageOperations(List<CreateChildOperation> operations, ThemeTreeNode parent) {
        operations.add(new CreateNewArea(parent, parent.getDOMElement(), "area"));
        operations.add(new CreateNewSimple(parent, parent.getDOMElement(), "select"));
        operations.add(new CreateNewSimple(parent, parent.getDOMElement(), "composed"));
        operations.add(new CreateNewSimple(parent, parent.getDOMElement(), "grid", "weightsX", "0,1,0", "weightsY", "0,1,0"));
        operations.add(new CreateNewSimple(parent, parent.getDOMElement(), "animation", "timeSource", "hover"));
        operations.add(new CreateNewSimple(parent, parent.getDOMElement(), "alias", "ref", "none"));
        operations.add(new CreateNewArea(parent, parent.getDOMElement(), "cursor", "hotSpotX", "0", "hotSpotY", "0"));
        operations.add(new CreateNewSimple("opNewNodeCursorAlias", parent, parent.getDOMElement(), "cursor", "ref", "none"));
    }
}
