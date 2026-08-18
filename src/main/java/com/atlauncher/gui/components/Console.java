/*
 * ATLauncher - https://github.com/ATLauncher/ATLauncher
 * Copyright (C) 2013-2022 ATLauncher
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.atlauncher.gui.components;

import java.awt.Color;
import java.awt.Container;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.BoxView;
import javax.swing.text.ComponentView;
import javax.swing.text.Element;
import javax.swing.text.IconView;
import javax.swing.text.LabelView;
import javax.swing.text.ParagraphView;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.evnt.LogEvent.LogType;
import com.atlauncher.themes.md3.token.MD3Color;
import com.atlauncher.themes.md3.token.MD3Type;

/**
 * The launcher's log, as a list of entries that happens to be rendered into a text pane.
 *
 * <p>
 * It used to be only the text pane: {@code setColor().setBold().write()} appended styled runs to a
 * document and that was the whole of it. Which meant there was nothing to filter - a document is
 * flat, and a line you have already written cannot be asked what level it was. So the console could
 * not hide debug output, could not search, and grew without bound for as long as the launcher ran.
 *
 * <p>
 * Keeping the entries means all three fall out: {@link #setLevelVisible} and {@link #setQuery} decide
 * which are drawn, {@link #getLog()} answers with all of them however the view is filtered - a log
 * uploaded to a bug report must be the whole log - and the oldest are dropped once there are
 * {@value #MAX_ENTRIES} of them.
 *
 * <p>
 * <b>Threading:</b> {@link #append} is called from the single logging thread, everything else from
 * the event thread. Entries are guarded; the <em>document</em> is only ever touched on the event
 * thread, which the previous version was not careful about and now has to be, because a filter change
 * rewrites the document from underneath whatever the logging thread is appending.
 */
public final class Console extends JTextPane {
    /** How many lines are kept. Past this the oldest go, which is what stops a long play session
     * from turning the console into a memory leak with a scrollbar. */
    private static final int MAX_ENTRIES = 5000;

    /** Dropping one line per new line would rewrite the document constantly, so they go in blocks. */
    private static final int TRIM_BLOCK = 500;

    /** Level tags are padded to this, so the message column lines up in a monospaced face. */
    private static final int LEVEL_WIDTH = 5;

    /** Characters before the message: the time, the tag, and the gaps around them. */
    private static final int MESSAGE_COLUMN = 8 + 2 + LEVEL_WIDTH + 2;

    private final Object lock = new Object();
    private final ArrayDeque<Entry> entries = new ArrayDeque<>();
    private final ArrayDeque<Entry> pending = new ArrayDeque<>();
    private final Set<LogType> levels = EnumSet.allOf(LogType.class);

    private String query = "";
    private String queryRaw = "";
    private Runnable onContentChanged;

    public Console() {
        setEditable(false);
        setEditorKit(new WrapEditorKit());
        refreshAppearance();
    }

    /**
     * Puts the console back on JetBrains Mono and the theme's chrome. Called after a theme or
     * language change: those walk the tree and would otherwise restyle this pane to the UI face,
     * which is proportional and cannot keep the columns lined up.
     */
    public void refreshAppearance() {
        setFont(ConsoleFonts.latin());
        putClientProperty(ConsoleFonts.TYPE_ROLE_KEY, Boolean.TRUE);

        setBackground(MD3Color.surfaceContainerLowest());
        setForeground(MD3Color.onSurface());
        setCaretColor(MD3Color.primary());
        setSelectionColor(MD3Color.primaryContainer());
        setSelectedTextColor(MD3Color.onPrimaryContainer());
        setDisabledTextColor(MD3Color.onSurfaceVariant());

        if (!entries.isEmpty()) {
            rebuild();
        }
    }

    @Override
    public void updateUI() {
        super.updateUI();

        // JTextPane.updateUI runs from the super constructor, before the entries deque exists
        if (entries != null) {
            refreshAppearance();
        }
    }

    /**
     * Called whenever what is on screen changes, so the window can say how much of the log it is
     * showing.
     */
    public void setOnContentChanged(Runnable onContentChanged) {
        this.onContentChanged = onContentChanged;
    }

    /**
     * Records a line. Safe from the logging thread; the drawing of it happens on the event thread.
     */
    public void append(LogType type, String time, String body) {
        Entry entry = new Entry(type, time, body);

        synchronized (lock) {
            entries.add(entry);
            pending.add(entry);
        }

        SwingUtilities.invokeLater(this::drain);
    }

    /**
     * Writes whatever has arrived since the last time round. Several {@code append}s in a row leave
     * several of these queued and the first one to run empties the backlog, so a burst of logging
     * costs one pass rather than one per line.
     */
    private void drain() {
        List<Entry> arrived;

        synchronized (lock) {
            if (pending.isEmpty()) {
                return;
            }

            arrived = new ArrayList<>(pending);
            pending.clear();
        }

        boolean tailing = isScrolledToBottom() && getSelectionStart() == getSelectionEnd();

        for (Entry entry : arrived) {
            if (matches(entry)) {
                render(entry);
            }
        }

        trim();

        if (tailing) {
            setCaretPosition(getDocument().getLength());
        }

        contentChanged();
    }

    /**
     * @return whether the view is following the end of the log. A console that scrolls to the bottom
     *         on every line cannot be read while Minecraft is running, which is precisely when there
     *         is something in it worth reading
     */
    private boolean isScrolledToBottom() {
        JScrollPane scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, this);

        if (scrollPane == null) {
            return true;
        }

        JScrollBar bar = scrollPane.getVerticalScrollBar();

        // a couple of lines of slack, so a view that is as good as at the bottom counts as at it
        return bar.getValue() + bar.getVisibleAmount() >= bar.getMaximum() - bar.getUnitIncrement() * 2;
    }

    private void trim() {
        List<Entry> dropped = new ArrayList<>();

        synchronized (lock) {
            if (entries.size() <= MAX_ENTRIES) {
                return;
            }

            for (int i = 0; i < TRIM_BLOCK && !entries.isEmpty(); i++) {
                dropped.add(entries.removeFirst());
            }
        }

        int length = 0;

        for (Entry entry : dropped) {
            length += entry.renderedLength;
        }

        if (length <= 0) {
            return;
        }

        try {
            getDocument().remove(0, length);
        } catch (BadLocationException e) {
            // the document and the entries disagree about what is in it, which nothing here can put
            // right - so start over from what the entries say, which is the record that matters
            rebuild();
        }
    }

    private boolean matches(Entry entry) {
        if (!levels.contains(entry.type)) {
            return false;
        }

        if (query.isEmpty()) {
            return true;
        }

        // time and level are searched too, so "ERROR" or a timestamp finds the line. The raw
        // query is kept so a Chinese search is not lost to a case fold that does not apply to it
        String haystack = entry.time + " " + entry.type.name() + " " + entry.body;

        return haystack.toLowerCase(Locale.ROOT).contains(query) || haystack.contains(queryRaw);
    }

    /**
     * Draws one entry: the time in the colour of chrome, the level in its own, the message in
     * whatever the level makes it. The old console coloured the <em>timestamp</em> by level and left
     * the level itself unwritten, which put the loudest thing on the line on the least interesting
     * part of it and gave you no way to tell a warning from a note.
     */
    private void render(Entry entry) {
        StyledDocument document = getStyledDocument();
        int start = document.getLength();

        try {
            ConsoleFonts.insert(document, entry.time + "  ", timeStyle());
            ConsoleFonts.insert(document, pad(entry.type.name()) + "  ", levelStyle(entry.type));
            ConsoleFonts.insert(document, entry.body, bodyStyle(entry.type));
        } catch (BadLocationException e) {
            return;
        }

        entry.renderedLength = document.getLength() - start;

        // a wrapped line hangs under the message rather than starting back at the margin, so a long
        // line reads as one line and not as an entry that lost its timestamp
        document.setParagraphAttributes(start, entry.renderedLength, hangingIndent(), false);
    }

    private static String pad(String level) {
        StringBuilder padded = new StringBuilder(level);

        while (padded.length() < LEVEL_WIDTH) {
            padded.append(' ');
        }

        return padded.toString();
    }

    private SimpleAttributeSet hangingIndent() {
        FontMetrics metrics = getFontMetrics(getFont());
        float indent = metrics.charWidth('0') * (float) MESSAGE_COLUMN;

        SimpleAttributeSet attributes = new SimpleAttributeSet();
        StyleConstants.setLeftIndent(attributes, indent);
        StyleConstants.setFirstLineIndent(attributes, -indent);
        StyleConstants.setLineSpacing(attributes, 0.18f);

        return attributes;
    }

    private static SimpleAttributeSet timeStyle() {
        SimpleAttributeSet attributes = new SimpleAttributeSet();
        StyleConstants.setForeground(attributes, MD3Color.onSurfaceVariant());

        return attributes;
    }

    private static SimpleAttributeSet levelStyle(LogType type) {
        SimpleAttributeSet attributes = new SimpleAttributeSet();
        StyleConstants.setForeground(attributes, type.color());
        StyleConstants.setBold(attributes, true);

        return attributes;
    }

    /**
     * Errors carry their colour through the message. Everything else is read, not scanned for, and a
     * page of coloured text has nothing left to emphasise with.
     */
    private static SimpleAttributeSet bodyStyle(LogType type) {
        Color foreground = type == LogType.ERROR ? type.color() : MD3Color.onSurface();

        SimpleAttributeSet attributes = new SimpleAttributeSet();
        StyleConstants.setForeground(attributes, foreground);

        return attributes;
    }

    public void setLevelVisible(LogType type, boolean visible) {
        boolean changed = visible ? levels.add(type) : levels.remove(type);

        if (changed) {
            rebuild();
        }
    }

    public boolean isLevelVisible(LogType type) {
        return levels.contains(type);
    }

    public void setQuery(String query) {
        String raw = query == null ? "" : query.trim();
        String normalised = raw.toLowerCase(Locale.ROOT);

        if (!normalised.equals(this.query) || !raw.equals(this.queryRaw)) {
            this.query = normalised;
            this.queryRaw = raw;

            rebuild();
        }
    }

    /**
     * Redraws every entry that passes the filter. Only ever from a filter or search change - a line
     * arriving is appended, not re-rendered, or the console would cost more the longer it ran.
     */
    private void rebuild() {
        List<Entry> snapshot;

        synchronized (lock) {
            pending.clear();
            snapshot = new ArrayList<>(entries);
        }

        setText("");

        for (Entry entry : snapshot) {
            entry.renderedLength = 0;

            if (matches(entry)) {
                render(entry);
            }
        }

        setCaretPosition(getDocument().getLength());
        contentChanged();
    }

    public void clear() {
        synchronized (lock) {
            entries.clear();
            pending.clear();
        }

        setText("");
        contentChanged();
    }

    /**
     * @return every line ever logged this session, whatever the view is filtered to. What Copy Log
     *         and Upload Log want: a log trimmed to what someone happened to be looking at is worse
     *         than no log, because nothing says it was trimmed
     */
    public String getLog() {
        List<Entry> snapshot;

        synchronized (lock) {
            snapshot = new ArrayList<>(entries);
        }

        StringBuilder log = new StringBuilder();

        for (Entry entry : snapshot) {
            log.append(entry.time).append(" [").append(entry.type.name()).append("] ").append(entry.body);
        }

        return log.toString();
    }

    public int getTotalCount() {
        synchronized (lock) {
            return entries.size();
        }
    }

    public int getShownCount() {
        List<Entry> snapshot;

        synchronized (lock) {
            snapshot = new ArrayList<>(entries);
        }

        int shown = 0;

        for (Entry entry : snapshot) {
            if (matches(entry)) {
                shown++;
            }
        }

        return shown;
    }

    private void contentChanged() {
        if (onContentChanged != null) {
            onContentChanged.run();
        }
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    /**
     * Fills the viewport when the log is shorter than it, so the empty state below has somewhere to
     * be drawn. A text pane is otherwise exactly as tall as its text, which for an empty one is
     * nothing at all.
     *
     * <p>
     * Measured through the UI rather than with {@code getPreferredSize}, which a
     * {@link javax.swing.JEditorPane} answers by asking this - and the two then call each other until
     * the stack runs out.
     */
    @Override
    public boolean getScrollableTracksViewportHeight() {
        Container parent = getParent();

        return parent instanceof JViewport && parent.getHeight() > getUI().getPreferredSize(this).height;
    }

    /**
     * Says why the console is empty when a filter is the reason. Filtering every level off leaves a
     * blank rectangle, which looks exactly like a console that has stopped working.
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (getDocument().getLength() > 0) {
            return;
        }

        String message = getTotalCount() == 0
                ? GetText.tr("No log output yet")
                : GetText.tr("No lines match the filter");

        Graphics2D g2 = (Graphics2D) g.create();

        try {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(MD3Type.font(MD3Type.BODY_MEDIUM, message));
            g2.setColor(MD3Color.onSurfaceVariant());

            FontMetrics metrics = g2.getFontMetrics();
            g2.drawString(message, (getWidth() - metrics.stringWidth(message)) / 2,
                    getHeight() / 2 + metrics.getAscent() / 2);
        } finally {
            g2.dispose();
        }
    }

    /** One logged line, and how much of the document it occupies while it is drawn. */
    private static final class Entry {
        final LogType type;
        final String time;
        final String body;

        int renderedLength;

        Entry(LogType type, String time, String body) {
            this.type = type;
            this.time = time;
            this.body = body;
        }
    }
}

// https://stackoverflow.com/a/13375811
class WrapEditorKit extends StyledEditorKit {
    ViewFactory defaultFactory = new WrapColumnFactory();

    @Override
    public ViewFactory getViewFactory() {
        return defaultFactory;
    }

}

class WrapColumnFactory implements ViewFactory {
    @Override
    public View create(Element elem) {
        String kind = elem.getName();
        if (kind != null) {
            switch (kind) {
                case AbstractDocument.ContentElementName:
                    return new WrapLabelView(elem);
                case AbstractDocument.ParagraphElementName:
                    return new ParagraphView(elem);
                case AbstractDocument.SectionElementName:
                    return new BoxView(elem, View.Y_AXIS);
                case StyleConstants.ComponentElementName:
                    return new ComponentView(elem);
                case StyleConstants.IconElementName:
                    return new IconView(elem);
            }
        }

        // default to text display
        return new LabelView(elem);
    }
}

class WrapLabelView extends LabelView {
    public WrapLabelView(Element elem) {
        super(elem);
    }

    @Override
    public float getMinimumSpan(int axis) {
        switch (axis) {
            case View.X_AXIS:
                return 0;
            case View.Y_AXIS:
                return super.getMinimumSpan(axis);
            default:
                throw new IllegalArgumentException("Invalid axis: " + axis);
        }
    }

}
