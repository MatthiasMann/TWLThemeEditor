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
package de.matthiasmann.twlthemeeditor.imgconv;

import de.matthiasmann.twl.FileSelector;
import de.matthiasmann.twl.FileTable;
import de.matthiasmann.twl.PopupWindow;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.FileSystemModel;
import de.matthiasmann.twl.model.FileSystemModel.FileFilter;
import de.matthiasmann.twl.model.JavaFileSystemModel;
import de.matthiasmann.twl.utils.CallbackSupport;
import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.prefs.Preferences;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;

/**
 *
 * @author Matthias Mann
 */
public class ImageSelectPopup extends PopupWindow {

    public interface Callback {
        public void imageSelected(String imagePath, ImageData imageData);
    }

    private static final String PREF_FS = "imageSelector";

    private final FileSelector fileSelector;
    private final ImagePreview preview;

    private String imagePath;
    private ImageData imageData;
    private Callback[] callbacks;

    public ImageSelectPopup(Widget owner) {
        super(owner);

        this.fileSelector = new FileSelector(Preferences.userNodeForPackage(ImageSelectPopup.class), PREF_FS);
        this.preview = new ImagePreview();

        HashSet<ImageReaderSpi> irSPIs = new HashSet<ImageReaderSpi>();
        for(String format : ImageIO.getReaderFormatNames()) {
            Iterator<ImageReader> iri = ImageIO.getImageReadersByFormatName(format);
            while(iri.hasNext()) {
                irSPIs.add(iri.next().getOriginatingProvider());
            }
        }

        MultiExtFilter all = new MultiExtFilter();
        fileSelector.addFileFilter(new FileSelector.NamedFileFilter("All image formats", all));

        for(ImageReaderSpi irs : irSPIs) {
            MultiExtFilter filter = new MultiExtFilter();
            for(String suffix : irs.getFileSuffixes()) {
                suffix = suffix.toLowerCase();
                filter.addExtension(suffix);
                all.addExtension(suffix);
            }
            if(!filter.isEmpty()) {
                fileSelector.addFileFilter(new FileSelector.NamedFileFilter(
                        irs.getDescription(Locale.getDefault()), filter));
            }
        }

        fileSelector.setFileSystemModel(JavaFileSystemModel.getInstance());
        fileSelector.setAllowMultiSelection(false);
        fileSelector.setUserWidgetRight(preview);
        fileSelector.getFileTable().addCallback(new FileTable.Callback() {
            public void selectionChanged() {
                updatePreview();
            }
            public void sortingChanged() {
            }
        });
        fileSelector.addCallback(new FileSelector.Callback() {
            public void filesSelected(Object[] files) {
                fireAcceptImage();
            }
            public void canceled() {
                closePopup();
            }
        });

        add(fileSelector);
        setCloseOnClickedOutside(false);
    }

    public void addCallback(Callback cb) {
        callbacks = CallbackSupport.addCallbackToList(callbacks, cb, Callback.class);
    }

    public void removeCallback(Callback cb) {
        callbacks = CallbackSupport.removeCallbackFromList(callbacks, cb);
    }

    @Override
    public boolean openPopup() {
        if(super.openPopup()) {
            int width = getGUI().getInnerWidth();
            int height = getGUI().getInnerHeight();
            setSize(width*4/5, height*4/5);
            setPosition(
                    width/2 - getWidth()/2,
                    height/2 - getHeight()/2);
            return true;
        }
        return false;
    }

    void updatePreview() {
        imagePath = null;
        imageData = null;
        FileTable.Entry[] selection = fileSelector.getFileTable().getSelection();
        if(selection.length >= 0) {
            FileTable.Entry entry = selection[0];
            if(!entry.isFolder && (entry.obj instanceof File)) {
                imageData = ImageData.create((File)entry.obj);
                imagePath = entry.getPath();
            }
        }

        if(imageData != null) {
            fileSelector.setOkButtonEnabled(true);
            preview.setImageData(imageData);
            preview.setTooltipContent(imagePath);
        } else {
            fileSelector.setOkButtonEnabled(false);
            preview.setImageData(null);
            preview.setTooltipContent(null);
        }
    }

    void fireAcceptImage() {
        if(imageData != null) {
            if(callbacks != null) {
                for(Callback cb : callbacks) {
                    cb.imageSelected(imagePath, imageData);
                }
            }
            closePopup();
        }
    }

    static class MultiExtFilter implements FileFilter {
        private final HashSet<String> extensions;

        public MultiExtFilter() {
            this.extensions = new HashSet<String>();
        }

        public boolean addExtension(String e) {
            return extensions.add(e);
        }

        public boolean isEmpty() {
            return extensions.isEmpty();
        }

        public boolean accept(FileSystemModel fsm, Object file) {
            String name = fsm.getName(file).toLowerCase();
            for(String extension : extensions) {
                if(name.endsWith(extension)) {
                    return true;
                }
            }
            return false;
        }

    }
}
