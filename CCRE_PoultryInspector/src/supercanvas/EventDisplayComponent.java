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
package supercanvas;

import ccre.channel.EventInput;
import ccre.channel.EventOutput;
import intelligence.Rendering;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;

/**
 * A component allowing display of events.
 *
 * @author skeggsc
 */
public class EventDisplayComponent extends BaseChannelComponent implements EventOutput {

    private transient long countStart;
    private boolean subscribed;
    private final EventInput inp;

    /**
     * Create a new EventDisplayComponent with a EventInput to read from.
     *
     * @param cx the X coordinate.
     * @param cy the Y coordinate.
     * @param name the name of the input.
     * @param inp the EventInput to read from.
     */
    public EventDisplayComponent(int cx, int cy, String name, EventInput inp) {
        super(cx, cy, name);
        this.inp = inp;
    }

    /**
     * Create a new EventDisplayComponent.
     *
     * @param cx the X coordinate.
     * @param cy the Y coordinate.
     * @param name the name of the input.
     */
    public EventDisplayComponent(int cx, int cy, String name) {
        this(cx, cy, name, null);
    }

    @Override
    public void channelRender(Graphics2D g, int screenWidth, int screenHeight, FontMetrics fontMetrics, int mouseX, int mouseY) {
        long count = (System.currentTimeMillis() - countStart);
        g.setColor(Rendering.blend(Color.green, Color.orange, count / 500.0f));
        int rad = Math.min(halfWidth / 3, halfHeight / 3);
        g.fillOval(centerX - rad, centerY - rad, rad * 2, rad * 2);
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
    public void event() {
        countStart = System.currentTimeMillis();
    }
}