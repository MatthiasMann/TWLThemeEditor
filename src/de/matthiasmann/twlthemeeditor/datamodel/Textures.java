/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.matthiasmann.twlthemeeditor.datamodel;

import de.matthiasmann.twl.Dimension;
import java.util.ArrayList;
import org.jdom.Element;

/**
 *
 * @author MannMat
 */
public class Textures extends NodeWrapper {

    private Dimension textureDimensions;
    
    Textures(ThemeFile themeFile, Element root) {
        super(themeFile, root);
    }

    public String getFile() {
        return getAttribute("file");
    }

    public String getFormat() {
        return getAttribute("format");
    }

    @Override
    public String toString() {
        return "[Textures file=\""+getFile()+"\"]";
    }

    public Dimension getTextureDimensions() {
        return textureDimensions;
    }

    public void setTextureDimensions(Dimension textureDimensions) {
        this.textureDimensions = textureDimensions;
    }

    public Image[] getImages() {
        ArrayList<Image> result = new ArrayList<Image>();
        for(Object node : root.getChildren()) {
            if(node instanceof Element) {
                Element element = (Element)node;
                String tagName = element.getName();
                Image image = null;

                if("texture".equals(tagName)) {
                    image = new Image.Texture(this, element);
                } else if("alias".equals(tagName)) {
                    image = new Image.Alias(themeFile, element);
                }

                if(image != null) {
                    result.add(image);
                }
            }
        }
        return result.toArray(new Image[result.size()]);
    }

}
