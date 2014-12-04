/*
 * Copyright 2014 Colby Skeggs
 * 
 * This file is part of the CCRE, the Common Chicken Runtime Engine.
 * 
 * The CCRE is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 * 
 * The CCRE is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the CCRE.  If not, see <http://www.gnu.org/licenses/>.
 */
package ccre.igneous;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.LinkedList;

import javax.swing.JPanel;

import ccre.channel.EventOutput;
import ccre.concurrency.ConcurrentDispatchArray;
import ccre.ctrl.ExpirationTimer;
import ccre.log.Logger;

/**
 * A base display panel used in device tree panels.
 *
 * @author skeggsc
 */
public final class DeviceListPanel extends JPanel {

    private static final long serialVersionUID = 3194911460808795658L;
    /**
     * The width of the embedded scrollbar.
     */
    private static final int SCROLLBAR_WIDTH = 20;
    /**
     * The width of the embedded scrollbar's padding.
     */
    private static final int SCROLLBAR_PADDING = 2;
    /**
     * The currently visible list of devices.
     */
    private final ConcurrentDispatchArray<Device> devices = new ConcurrentDispatchArray<Device>();
    /**
     * The most recent position of the mouse.
     */
    private transient int mouseX, mouseY;
    /**
     * The current scrolling position. Larger means further down the list.
     */
    private transient int scrollPos, scrollMax;
    /**
     * An expiration timer to repaint the pane when appropriate.
     */
    private transient ExpirationTimer painter;
    /**
     * The relative position of the currently-dragged scrollbar, or null if not
     * dragging.
     */
    private transient Float dragPosition;
    /**
     * The number of devices that appear in the first column.
     */
    private transient int devicesInFirstColumn;

    /**
     * Add the specified device to this panel.
     *
     * @param comp The device to add.
     * @return the added device.
     */
    public synchronized <E extends Device> E add(E comp) {
        comp.setParent(this);
        devices.add(comp);
        repaint();
        return comp;
    }

    /**
     * Remove the specified device from this panel.
     *
     * @param comp The device to remove.
     */
    public synchronized void remove(Device comp) {
        if (devices.remove(comp)) {
            comp.setParent(null);
            repaint();
        }
    }

    /**
     * Start the IntelligenceMain instance so that it runs.
     */
    public void start() {
        MouseAdapter listener = new SuperCanvasMouseAdapter();
        this.addMouseWheelListener(listener);
        this.addMouseListener(listener);
        this.addMouseMotionListener(listener);
        painter = new ExpirationTimer();
        painter.schedule(100, new EventOutput() {
            @Override
            public void event() {
                repaint();
            }
        });
        painter.start();
    }

    @Override
    public void paint(Graphics go) {
        try {
            boolean splitColumns = shouldColumnsSplit();
            Graphics2D g = (Graphics2D) go;
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
            int w = getWidth() - SCROLLBAR_WIDTH;
            int h = getHeight();
            g.setFont(Rendering.labels);
            FontMetrics fontMetrics = g.getFontMetrics();
            renderBackground(g, w, h, fontMetrics);
            int totalHeight = 0;
            for (Device comp : devices) {
                totalHeight += comp.getHeight();
            }
            int calcDevicesInFirstColumn = 0;
            if (splitColumns) {
                int runningHeight = 0;
                for (Device comp : devices) {
                    runningHeight += comp.getHeight();
                    calcDevicesInFirstColumn++;
                    if (runningHeight > totalHeight / 2) {
                        break;
                    }
                }
                int maxColumnDevices = calcDevicesInFirstColumn;
                int columnA = 0, columnB = 0;
                for (Device comp : devices) {
                    if (maxColumnDevices-- <= 0) {
                        columnB += comp.getHeight();
                    } else {
                        columnA += comp.getHeight();
                    }
                }
                this.scrollMax = Math.max(columnA, columnB);
            } else {
                this.scrollMax = totalHeight;
            }
            this.devicesInFirstColumn = calcDevicesInFirstColumn;
            scrollPos = Math.max(Math.min(scrollPos, scrollMax - h), 0);
            renderScrollbar(g, w, SCROLLBAR_WIDTH);
            int yPosition = -scrollPos, xPosition = 0;
            int maxColumnDevices = calcDevicesInFirstColumn;
            for (Device comp : devices) {
                if (splitColumns && maxColumnDevices-- == 0) {
                    yPosition = -scrollPos; // new column, reset y position.
                    xPosition = w / 2;
                }
                int deviceHeight = comp.getHeight();
                int bottom = yPosition + deviceHeight;
                if (yPosition >= -deviceHeight && bottom <= h + deviceHeight) {
                    g.setFont(Rendering.labels);
                    g.translate(xPosition, yPosition);
                    Shape clip = g.getClip();
                    g.setClip(new Rectangle(0, 0, w, deviceHeight));
                    comp.render(g, splitColumns ? w / 2 : w, deviceHeight, fontMetrics, mouseX - xPosition, mouseY - yPosition);
                    g.setClip(clip);
                    g.translate(-xPosition, -yPosition);
                }
                yPosition = bottom;
            }
            if (painter != null) {
                painter.feed();
            } else {
                String navail = "Panel Not Started";
                g.setColor(Color.BLACK);
                g.drawString(navail, w / 2 - fontMetrics.stringWidth(navail) / 2, h / 2 - fontMetrics.getHeight() / 2);
            }
        } catch (Throwable thr) {
            Logger.severe("Exception while handling paint event", thr);
        }
    }

    private boolean shouldColumnsSplit() {
        return getWidth() > 1400;
    }

    private int scrollbarRange() {
        return getHeight() - SCROLLBAR_PADDING * 2;
    }

    private float positionToScrollbarPosition(float y) {
        return SCROLLBAR_PADDING + scrollbarRange() * y / scrollMax;
    }

    private float scrollbarPositionToPosition(float y) {
        return (y - SCROLLBAR_PADDING) * scrollMax / scrollbarRange();
    }

    private void renderScrollbar(Graphics2D g, int x, int width) {
        int height = getHeight();
        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(x, 0, width, height);
        g.setColor(Color.WHITE);
        g.drawRect(x, 0, width - 1, height - 1);
        int scrollbarHeight = Math.min(Math.round(positionToScrollbarPosition(height) - SCROLLBAR_PADDING), getHeight() - SCROLLBAR_PADDING * 2);
        int position = Math.round(positionToScrollbarPosition(scrollPos));
        g.setColor(Color.BLACK);
        g.fillRect(x + SCROLLBAR_PADDING, position, width - SCROLLBAR_PADDING * 2, scrollbarHeight);
        g.setColor(Color.GRAY);
        g.drawRect(x + SCROLLBAR_PADDING, position, width - SCROLLBAR_PADDING * 2 - 1, scrollbarHeight - 1);
    }

    private void renderBackground(Graphics2D g, int w, int h, FontMetrics fontMetrics) {
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
    }

    private class SuperCanvasMouseAdapter extends MouseAdapter {

        SuperCanvasMouseAdapter() {
        }

        @Override
        public void mousePressed(MouseEvent e) {
            try {
                if (e.getX() >= getWidth() - SCROLLBAR_WIDTH) {
                    // It's on the scrollbar.
                    dragPosition = scrollbarPositionToPosition(e.getY()) - scrollPos;
                    repaint();
                    return;
                }
                int yPosition = scrollPos + e.getY();
                int columnWidth = (getWidth() - SCROLLBAR_WIDTH) / 2;
                boolean inSecondColumn = shouldColumnsSplit() && e.getX() > columnWidth;
                int xPosition = inSecondColumn ? e.getX() - columnWidth : e.getX();
                int devicesRemainingToSkip = inSecondColumn ? devicesInFirstColumn : 0;
                for (Device dev : devices) {
                    if (devicesRemainingToSkip-- > 0) {
                        continue;
                    }
                    int deviceHeight = dev.getHeight();
                    if (yPosition >= 0 && yPosition < deviceHeight) {
                        dev.onPress(xPosition, yPosition);
                        repaint();
                    }
                    yPosition -= deviceHeight;
                }
            } catch (Throwable thr) {
                Logger.severe("Exception while handling mouse press", thr);
            }
        }

        private void updateDragLocation(int newY) {
            scrollPos = Math.round(scrollbarPositionToPosition(newY) - dragPosition);
            repaint();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            try {
                if (dragPosition != null) {
                    updateDragLocation(e.getY());
                    dragPosition = null;
                    return;
                }
                int yPosition = scrollPos + e.getY();
                int columnWidth = (getWidth() - SCROLLBAR_WIDTH) / 2;
                boolean inSecondColumn = shouldColumnsSplit() && e.getX() > columnWidth;
                int xPosition = inSecondColumn ? e.getX() - columnWidth : e.getX();
                int devicesRemainingToSkip = inSecondColumn ? devicesInFirstColumn : 0;
                for (Device dev : devices) {
                    if (devicesRemainingToSkip-- > 0) {
                        continue;
                    }
                    int deviceHeight = dev.getHeight();
                    if (yPosition >= 0 && yPosition < deviceHeight) {
                        dev.onRelease(xPosition, yPosition);
                        repaint();
                    }
                    yPosition -= deviceHeight;
                }
            } catch (Throwable thr) {
                Logger.severe("Exception while handling mouse release", thr);
            }
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            try {
                scrollPos += e.getWheelRotation();
                repaint();
            } catch (Throwable thr) {
                Logger.severe("Exception while handling mouse wheel", thr);
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            mouseMoved(e);
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            try {
                int oldMouseX = mouseX;
                int oldMouseY = mouseY;
                mouseX = e.getX();
                mouseY = e.getY();
                int columnWidth = (getWidth() - SCROLLBAR_WIDTH) / 2;
                boolean inSecondColumn = shouldColumnsSplit() && e.getX() > columnWidth;
                int oldXPosition = inSecondColumn ? oldMouseX - columnWidth : oldMouseX;
                int xPosition = inSecondColumn ? mouseX - columnWidth : mouseX;
                boolean wasInSelectionArea = oldXPosition >= 0 && oldXPosition < columnWidth;
                boolean isInSelectionArea = xPosition >= 0 && xPosition < columnWidth;
                if (dragPosition != null) {
                    updateDragLocation(e.getY());
                    return;
                }
                int yPosition = scrollPos + mouseY;
                int oldYPosition = scrollPos + oldMouseY;
                int devicesRemainingToSkip = inSecondColumn ? devicesInFirstColumn : 0;
                for (Device dev : devices) {
                    if (devicesRemainingToSkip-- > 0) {
                        continue;
                    }
                    int deviceHeight = dev.getHeight();

                    boolean isIn = yPosition >= 0 && yPosition < deviceHeight && isInSelectionArea;
                    boolean wasIn = oldYPosition >= 0 && oldYPosition < deviceHeight && wasInSelectionArea;
                    if (isIn) {
                        if (wasIn) {
                            dev.onMouseMove(xPosition, yPosition);
                        } else {
                            dev.onMouseEnter(xPosition, yPosition);
                        }
                    } else if (wasIn) {
                        dev.onMouseExit(xPosition, yPosition);
                    }

                    yPosition -= deviceHeight;
                    oldYPosition -= deviceHeight;
                }
            } catch (Throwable thr) {
                Logger.severe("Exception while handling mouse move", thr);
            }
        }
    }
}
