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
    private final LinkedList<Device> devices = new LinkedList<Device>();
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
     * The relative position of the currently-dragged scrollbar, or null if not dragging.
     */
    private transient Float dragPosition;

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
            this.scrollMax = totalHeight;
            scrollPos = Math.max(Math.min(scrollPos, totalHeight - h), 0);
            renderScrollbar(g, w, SCROLLBAR_WIDTH);
            int yPosition = -scrollPos;
            for (Device comp : devices) {
                int deviceHeight = comp.getHeight();
                int bottom = yPosition + deviceHeight;
                if (yPosition >= -deviceHeight && bottom <= h + deviceHeight) {
                    g.setFont(Rendering.labels);
                    g.translate(0, yPosition);
                    Shape clip = g.getClip();
                    g.setClip(new Rectangle(0, 0, w, deviceHeight));
                    comp.render(g, w, deviceHeight, fontMetrics, mouseX, mouseY - yPosition);
                    g.setClip(clip);
                    g.translate(0, -yPosition);
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
                for (Device comp : devices) {
                    int deviceHeight = comp.getHeight();
                    if (yPosition >= 0 && yPosition < deviceHeight) {
                        comp.onPress(e.getX(), yPosition);
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
                for (Device dev : devices) {
                    int deviceHeight = dev.getHeight();
                    if (yPosition >= 0 && yPosition < deviceHeight) {
                        dev.onRelease(e.getX(), yPosition);
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
                boolean wasInSelectionArea = oldMouseX < getWidth() - SCROLLBAR_WIDTH;
                boolean isInSelectionArea = mouseX < getWidth() - SCROLLBAR_WIDTH;
                if (dragPosition != null) {
                    updateDragLocation(e.getY());
                    return;
                }
                int yPosition = scrollPos + mouseY;
                int oldYPosition = scrollPos + oldMouseY;
                for (Device dev : devices) {
                    int deviceHeight = dev.getHeight();
                    
                    boolean isIn = yPosition >= 0 && yPosition < deviceHeight && isInSelectionArea;
                    boolean wasIn = oldYPosition >= 0 && oldYPosition < deviceHeight && wasInSelectionArea;
                    //System.out.println("DEVICE " + dev + ": " + isIn + " / " + wasIn);
                    if (isIn) {
                        if (wasIn) {
                            dev.onMouseMove(e.getX(), yPosition);
                        } else {
                            dev.onMouseEnter(e.getX(), yPosition);
                        }
                    } else if (wasIn) {
                        dev.onMouseExit(e.getX(), yPosition);
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