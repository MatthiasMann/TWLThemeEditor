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

import de.matthiasmann.twl.GUI;
import de.matthiasmann.twl.utils.CallbackSupport;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

/**
 *
 * @author Matthias Mann
 */
public class MessageLog {

    public enum CombineMode {
        NONE,
        REPLACE
    }

    public static class Category {
        private final String name;
        private final CombineMode combineMode;
        private final int flags;

        public Category(String name, CombineMode combineMode, int flags) {
            this.name = name;
            this.combineMode = combineMode;
            this.flags = flags;
        }

        @Override
        public String toString() {
            return name;
        }

        public CombineMode getCombineMode() {
            return combineMode;
        }

        public int getFlags() {
            return flags;
        }
    }

    public static abstract class EntryAction implements Runnable {
        private final String name;
        public EntryAction(String name) {
            this.name = name;
        }
        public String getName() {
            return name;
        }
    }

    public static class Entry {
        private final long time;
        private final Category category;
        private final String msg;
        private final String detailText;
        private final Throwable detailException;
        private final EntryAction[] actions;

        public Entry(Category category, String msg, String detailText, Throwable detailException, EntryAction ... actions) {
            if(category == null) {
                throw new NullPointerException("category");
            }
            if(msg == null) {
                throw new NullPointerException("msg");
            }
            this.time = System.currentTimeMillis();
            this.category = category;
            this.msg = msg;
            this.detailText = detailText;
            this.detailException = detailException;
            this.actions = actions;
        }

        public EntryAction[] getActions() {
            return actions.clone();
        }

        public Category getCategory() {
            return category;
        }

        public Throwable getDetailException() {
            return detailException;
        }

        public String getDetailText() {
            return detailText;
        }

        public String getMessage() {
            return msg;
        }

        public Date getTime() {
            return new Date(time);
        }
    }

    private static final int MAX_LOG_SIZE = 100;

    private final ArrayList<Entry> entries;
    private GUI gui;
    private Runnable[] callbacks;
    private boolean callbackPending;

    public MessageLog() {
        this.entries = new ArrayList<MessageLog.Entry>();
    }

    public synchronized void add(Entry entry) {
        switch (entry.getCategory().getCombineMode()) {
            case REPLACE:
                removeAllImpl(entry.getCategory());
                break;
        }
        if(entries.size() >= MAX_LOG_SIZE-1) {
            entries.remove(0);
        }
        entries.add(entry);
        fireCallback();
    }

    public synchronized void remove(Entry entry) {
        if(entries.remove(entry)) {
            fireCallback();
        }
    }

    public synchronized void removeAll(Category category) {
        if(removeAllImpl(category)) {
            fireCallback();
        }
    }
    
    public synchronized Entry[] getEntries() {
        return entries.toArray(new Entry[entries.size()]);
    }

    public synchronized Entry getLatestMessage() {
        if(entries.isEmpty()) {
            return null;
        } else {
            return entries.get(entries.size()-1);
        }
    }

    public synchronized void setGUI(GUI gui) {
        this.gui = gui;
        if(callbackPending) {
            fireCallback();
        }
    }

    public void addCallback(Runnable cb) {
        callbacks = CallbackSupport.addCallbackToList(callbacks, cb, Runnable.class);
    }

    public void removeCallback(Runnable cb) {
        callbacks = CallbackSupport.removeCallbackFromList(callbacks, cb);
    }

    private boolean removeAllImpl(Category category) {
        boolean removed = false;
        Iterator<Entry> iter = entries.iterator();
        while(iter.hasNext()) {
            Entry e = iter.next();
            if(e.getCategory() == category) {
                iter.remove();
                removed = true;
            }
        }
        return removed;
    }

    private void fireCallback() {
        if(!callbackPending) {
            callbackPending = true;
            if(gui != null) {
                gui.invokeLater(new Runnable() {
                    public void run() {
                        executeCallbacks();
                    }
                });
            }
        }
    }

    void executeCallbacks() {
        synchronized(this) {
            callbackPending = false;
        }
        CallbackSupport.fireCallbacks(callbacks);
    }
    
}
