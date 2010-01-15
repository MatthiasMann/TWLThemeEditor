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
import de.matthiasmann.twlthemeeditor.datamodel.operations.CreateNewSimple;
import de.matthiasmann.twlthemeeditor.datamodel.operations.CreateNewTexture;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import org.jdom.Element;

/**
 *
 * @author Matthias Mann
 */
public class Textures extends AbstractThemeTreeNode {

    private final URL textureURL;
    private final Dimension textureDimensions;
    private final Element element;
    
    Textures(TreeTableNode parent, Element element, ThemeFile themeFile) throws IOException {
        super(themeFile, parent);
        this.element = element;
        
        textureURL = themeFile.getURL(getFile());
        InputStream textureStream = textureURL.openStream();
        try {
            PNGDecoder decoder = new PNGDecoder(textureStream);
            textureDimensions = new Dimension(decoder.getWidth(), decoder.getHeight());
        } finally {
            textureStream.close();
        }

        themeFile.getEnv().registerFile(getFile(), textureURL);
    }

    public String getFile() {
        return element.getAttributeValue("file");
    }

    public String getFormat() {
        return element.getAttributeValue("format");
    }

    public URL getTextureURL() {
        return textureURL;
    }

    @Override
    public String toString() {
        return "[Textures file=\""+getFile()+"\"]";
    }

    @Override
    public String getName() {
        return getFile();
    }

    @Override
    protected String getType() {
        return "PNG";
    }

    public Dimension getTextureDimensions() {
        return textureDimensions;
    }

    public Element getDOMElement() {
        return element;
    }

    public void addChildren() throws IOException {
        Utils.addChildren(themeFile, this, element, Image.getImageDomWrapper(this));
    }

    @SuppressWarnings("unchecked")
    public void addToXPP(DomXPPParser xpp) {
        Utils.addToXPP(xpp, element.getName(), this, element.getAttributes());
    }

    public List<ThemeTreeOperation> getOperations() {
        List<ThemeTreeOperation> operations = AbstractThemeTreeNode.getDefaultOperations(element, this);
        addCreateImageOperations(operations, this);
        return operations;
    }

    public static void addCreateImageOperations(List<ThemeTreeOperation> operations, ThemeTreeNode parent) {
        operations.add(new CreateNewTexture(parent, "texture"));
        operations.add(new CreateNewTexture(parent, "hsplit", "hsplit", "0,0"));
        operations.add(new CreateNewTexture(parent, "vsplit", "vsplit", "0,0"));
        operations.add(new CreateNewTexture(parent, "hvsplit", "hsplit", "0,0", "vsplit", "0,0"));
        operations.add(new CreateNewSimple(parent, "select"));
        operations.add(new CreateNewSimple(parent, "composed"));
        operations.add(new CreateNewSimple(parent, "grid", "weightsX", "0,1,0", "weightsY", "0,1,0"));
        operations.add(new CreateNewSimple(parent, "animation", "timeSource", "hover"));
    }
}
