/*
 * Copyright 2014 Colby Skeggs.
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
package ccre.supercanvas.components.channels;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;

import ccre.channel.BooleanInput;
import ccre.channel.BooleanOutput;
import ccre.supercanvas.BaseChannelComponent;
import ccre.supercanvas.Rendering;
import ccre.supercanvas.SuperCanvasPanel;

/**
 * A component allowing display of booleans.
 *
 * @author skeggsc
 */
public class BooleanDisplayComponent extends BaseChannelComponent<BooleanDisplayComponent.View> implements BooleanOutput {

    public static enum View {
        CONFIGURATION, RED_GREEN_LIGHT, WARNING_LIGHT, TEXTUAL
    }

    private static final long serialVersionUID = -5453098172677583207L;
    private boolean pressed;
    private boolean subscribed;
    private final BooleanInput inp;

    /**
     * Create a new BooleanDisplayComponent with a BooleanInput to read from.
     *
     * @param cx the X coordinate.
     * @param cy the Y coordinate.
     * @param name the name of the input.
     * @param inp the BooleanInput to read from.
     */
    public BooleanDisplayComponent(int cx, int cy, String name, BooleanInput inp) {
        super(cx, cy, name);
        this.inp = inp;
    }

    /**
     * Create a new BooleanDisplayComponent.
     *
     * @param cx the X coordinate.
     * @param cy the Y coordinate.
     * @param name the name of the input.
     */
    public BooleanDisplayComponent(int cx, int cy, String name) {
        this(cx, cy, name, null);
    }

    @Override
    public void channelRender(Graphics2D g, int screenWidth, int screenHeight, FontMetrics fontMetrics, int mouseX, int mouseY) {
        switch (activeView) {
        case RED_GREEN_LIGHT:
            int rad1 = Math.min(halfWidth / 3, halfHeight / 3);
            g.setColor(pressed ? Color.GREEN : Color.RED);
            g.fillOval(centerX - rad1, centerY - rad1, rad1 * 2, rad1 * 2);
            g.setColor(Color.BLACK);
            g.drawOval(centerX - rad1, centerY - rad1, rad1 * 2, rad1 * 2);
            break;
        case WARNING_LIGHT:
            int rad2 = Math.min(halfWidth / 2, halfHeight / 2);
            g.setColor(Color.BLACK);
            g.fillOval(centerX - rad2 - 2, centerY - rad2 - 2, rad2 * 2 + 4, rad2 * 2 + 4);
            g.setColor(pressed ? (System.currentTimeMillis() / 100) % 2 == 0 ? Color.RED : Color.BLACK : Color.GRAY);
            g.fillOval(centerX - rad2, centerY - rad2, rad2 * 2, rad2 * 2);
            break;
        case TEXTUAL:
            g.setFont(Rendering.labels);
            if (getPanel().editmode) {
                g.setColor(Color.BLACK);
            } else {
                g.setColor(pressed ? Color.GREEN : Color.RED);
            }
            String text = pressed ? "TRUE" : "FALSE";
            g.drawString(text, centerX - g.getFontMetrics().stringWidth(text) / 2, centerY + g.getFontMetrics().getAscent() / 2);
            break;
        case CONFIGURATION: // never called
        }
    }

    @Override
    public boolean onInteract(int x, int y) {
        return false;
    }

    @Override
    protected void onChangePanel(SuperCanvasPanel panel) {
        boolean hasPanel = panel != null;
        if (inp != null && hasPanel != subscribed) {
            if (hasPanel) {
                inp.send(this);
            } else {
                inp.unsend(this);
            }
            subscribed = hasPanel;
        }
    }

    @Override
    public void set(boolean value) {
        pressed = value;
    }

    @Override
    protected void setDefaultView() {
        activeView = View.RED_GREEN_LIGHT;
    }
}
