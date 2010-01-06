/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.matthiasmann.twlthemeeditor.gui;

import de.matthiasmann.twl.DialogLayout;
import de.matthiasmann.twl.ToggleButton;
import de.matthiasmann.twlthemeeditor.datamodel.Optional;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.TreeMap;

/**
 *
 * @author MannMat
 */
public class PropertyPanel extends DialogLayout {

    protected static final int NUM_COLUMNS = 2;

    protected final Context ctx;
    protected final Group horzActive;
    protected final Group horzColumns[];
    protected final Group horzComplete;

    public PropertyPanel(Context ctx, Object obj) throws IntrospectionException {
        this.ctx = ctx;
        
        horzComplete = createParallelGroup();
        horzActive = createParallelGroup();
        horzColumns = new Group[NUM_COLUMNS];

        for(int i=0 ; i<NUM_COLUMNS ; i++) {
            horzColumns[i] = createParallelGroup();
        }

        horzComplete.addGroup(createSequentialGroup(horzColumns));
        
        setHorizontalGroup(createSequentialGroup()
                .addGroup(horzActive)
                .addGroup(horzComplete));
        setVerticalGroup(createSequentialGroup());

        BeanInfo beanInfo = Introspector.getBeanInfo(obj.getClass());
        TreeMap<String, PropertyDescriptor> properties = new TreeMap<String, PropertyDescriptor>();
        for(PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
            if(pd.getWriteMethod() != null && pd.getReadMethod() != null) {
                properties.put(pd.getName(), pd);
            }
        }

        for(String property : ctx.getPropertyOrder()) {
            PropertyDescriptor pd = properties.remove(property);
            if(pd != null) {
                addProperty(obj, pd);
            }
        }

        for(PropertyDescriptor pd : properties.values()) {
            addProperty(obj, pd);
        }
    }

    protected void addProperty(Object obj, PropertyDescriptor pd) {
        boolean optional = pd.getReadMethod().isAnnotationPresent(Optional.class);
        Class<?> type = pd.getPropertyType();

        PropertyEditorFactory factory = ctx.getFactory(type);
        if(factory != null) {
            Group vert = createParallelGroup();
            ToggleButton btnActive = null;

            if(optional) {
                btnActive = new ToggleButton();
                btnActive.setTheme("active");
                horzActive.addWidget(btnActive);
                vert.addGroup(createSequentialGroup().addWidget(btnActive).addGap());
            }

            factory.create(this, vert, obj, pd, btnActive);

            getVerticalGroup().addGroup(vert);
        } else {
            System.out.println("No factory for property " + pd.getName() + " type " + type);
        }
    }
}
