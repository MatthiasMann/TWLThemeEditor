/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.matthiasmann.twlthemeeditor.gui;

import de.matthiasmann.twl.Color;
import de.matthiasmann.twl.Rect;
import de.matthiasmann.twl.model.ListModel;
import de.matthiasmann.twl.utils.TypeMapping;
import de.matthiasmann.twlthemeeditor.datamodel.Image;
import de.matthiasmann.twlthemeeditor.datamodel.ImageReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author MannMat
 */
public class Context {

    private final ListModel<Image> images;
    private final TypeMapping<PropertyEditorFactory> factories;
    private final ArrayList<String> propertyOrder;

    public Context(ListModel<Image> images) {
        this.images = images;
        
        factories = new TypeMapping<PropertyEditorFactory>();
        factories.put(Integer.class, new IntegerEditor());
        factories.put(int.class, new IntegerEditor());
        factories.put(boolean.class, new BooleanEditor());
        factories.put(Color.class, new ColorEditor());
        factories.put(Rect.class, new RectEditor());
        factories.put(ImageReference.class, new ImageRefEditor(this));

        this.propertyOrder = new ArrayList<String>();
    }

    public ListModel<Image> getImages() {
        return images;
    }

    public PropertyEditorFactory getFactory(Class<?> clazz) {
        return factories.get(clazz);
    }

    public int findImage(ImageReference ref) {
        for(int i=0,n=images.getNumEntries() ; i<n ; i++) {
            if(ref.getName().equals(images.getEntry(i).getName())) {
                return i;
            }
        }
        return -1;
    }

    public List<String> getPropertyOrder() {
        return Collections.unmodifiableList(propertyOrder);
    }

    public void setPropertyOrder(String ... order) {
        propertyOrder.clear();
        propertyOrder.addAll(Arrays.asList(order));
    }
}
