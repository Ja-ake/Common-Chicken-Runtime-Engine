/*
 * Copyright 2013-2014 Colby Skeggs, Gregor Peach (Added Folders)
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
package intelligence;

import ccre.channel.EventOutput;
import ccre.cluck.Cluck;
import ccre.ctrl.ExpirationTimer;
import ccre.ctrl.PauseTimer;
import ccre.log.FileLogger;
import ccre.log.Logger;
import ccre.log.NetworkAutologger;
import ccre.net.CountingNetworkProvider;
import intelligence.monitor.IPhidgetMonitor;
import intelligence.monitor.NonexistentPhidgetMonitor;
import intelligence.monitor.PhidgetMonitor;
import intelligence.monitor.VirtualPhidgetMonitor;
import java.util.Arrays;
import java.util.Date;

/**
 * The main frame for the Poultry Inspector.
 *
 * @author skeggsc
 */
public class PoultryInspectorFrame extends javax.swing.JFrame {

    /**
     * The main entry point for the program. Sets up various subsystems and then
     * starts the GUI.
     *
     * @param args The program arguments.
     */
    public static void main(String[] args) {
        CountingNetworkProvider.register();
        NetworkAutologger.register();
        FileLogger.register();
        IPProvider.init();
        new PoultryInspectorFrame().start(args);
    }
    private IPhidgetMonitor monitor;

    /**
     * Create a new PoultryInspectorFrame.
     */
    public PoultryInspectorFrame() {
        initComponents();
    }

    private void start(String[] args) {
        this.setVisible(true);

        ListModelLogger.register(loggingEntries, loggingList, rescroller);
        Logger.info("Started Poultry Inspector at " + System.currentTimeMillis());

        monitor = new NonexistentPhidgetMonitor();
        processArguments(args);
        monitor.share();

        setupTimeNotifier();
        setupWatchdog(monitor);
        IPProvider.connect();

        intelligenceMain.start();
    }

    private void processArguments(String[] iargs) {
        String[] args = iargs;
        if (args.length == 0) {
            return;
        }
        if (args[0].equals("-virtual")) {
            monitor = new VirtualPhidgetMonitor();
            System.out.println("Current argument count: " + args.length);
            args = Arrays.copyOfRange(args, 1, args.length);
        } else if (args[0].equals("-phidget")) {
            monitor = new PhidgetMonitor();
            args = Arrays.copyOfRange(args, 1, args.length);
        }
        if (args.length >= 2) {
            try {
                this.setSize(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
            } catch (NumberFormatException ex) {
                Logger.warning("Bad window size!", ex);
            }
        }
        if (args.length >= 4) {
            try {
                this.setLocation(Integer.parseInt(args[2]), Integer.parseInt(args[3]));
            } catch (NumberFormatException ex) {
                Logger.warning("Bad window position!", ex);
            }
        }
    }

    private static void setupWatchdog(final IPhidgetMonitor monitor) {
        final ExpirationTimer watchdog = new ExpirationTimer();
        watchdog.schedule(500, Cluck.subscribeEO("robot/phidget/WatchDog"));
        watchdog.schedule(2000, new EventOutput() {
            @Override
            public void event() {
                monitor.connectionDown();
            }
        });
        watchdog.schedule(3000, watchdog.getFeedEvent());
        watchdog.start();
        Cluck.publish("WatchDog", new EventOutput() {
            @Override
            public void event() {
                monitor.connectionUp();
                watchdog.feed();
            }
        });
    }

    private void setupTimeNotifier() {
        final PauseTimer timer = new PauseTimer(5000);
        timer.triggerAtEnd(new EventOutput() {
            @Override
            public void event() {
                Logger.info("Current time: " + new Date());
            }
        });
        Cluck.getNode().subscribeToStructureNotifications("notify-fetcher-virt", timer);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        loggingEntries = new javax.swing.DefaultListModel();
        rescroller = new intelligence.RescrollingThread();
        jSplitPane1 = new javax.swing.JSplitPane();
        intelligenceMain = new intelligence.IntelligenceMain();
        jPanel1 = new javax.swing.JPanel();
        loggingScroller = new javax.swing.JScrollPane();
        loggingList = new javax.swing.JList();
        btnClear = new javax.swing.JButton();
        btnRefresh = new javax.swing.JButton();
        btnReconnect = new javax.swing.JButton();
        textAddress = new javax.swing.JTextField();
        btnSetAddress = new javax.swing.JButton();

        rescroller.setScrollBar(loggingScroller.getVerticalScrollBar());

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Poultry Inspector");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        jSplitPane1.setDividerLocation(320);
        jSplitPane1.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        jSplitPane1.setResizeWeight(0.7);

        javax.swing.GroupLayout intelligenceMainLayout = new javax.swing.GroupLayout(intelligenceMain);
        intelligenceMain.setLayout(intelligenceMainLayout);
        intelligenceMainLayout.setHorizontalGroup(
            intelligenceMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 638, Short.MAX_VALUE)
        );
        intelligenceMainLayout.setVerticalGroup(
            intelligenceMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 282, Short.MAX_VALUE)
        );

        jSplitPane1.setLeftComponent(intelligenceMain);

        loggingList.setModel(loggingEntries);
        loggingScroller.setViewportView(loggingList);

        btnClear.setText("Clear");
        btnClear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnClearActionPerformed(evt);
            }
        });

        btnRefresh.setText("Refresh");
        btnRefresh.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRefreshActionPerformed(evt);
            }
        });

        btnReconnect.setText("Reconnect");
        btnReconnect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnReconnectActionPerformed(evt);
            }
        });

        textAddress.setFont(new java.awt.Font("Monospaced", 0, 12)); // NOI18N
        textAddress.setText("*");

        btnSetAddress.setText("Set Address");
        btnSetAddress.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSetAddressActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(loggingScroller)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(btnClear)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnRefresh)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnReconnect)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(textAddress, javax.swing.GroupLayout.DEFAULT_SIZE, 312, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnSetAddress))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(loggingScroller, javax.swing.GroupLayout.DEFAULT_SIZE, 129, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnClear)
                    .addComponent(btnRefresh)
                    .addComponent(btnReconnect)
                    .addComponent(textAddress, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnSetAddress)))
        );

        jSplitPane1.setRightComponent(jPanel1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane1, javax.swing.GroupLayout.Alignment.TRAILING)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane1, javax.swing.GroupLayout.Alignment.TRAILING)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        monitor.displayClosing();
        System.exit(0);
    }//GEN-LAST:event_formWindowClosing

    private void btnClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnClearActionPerformed
        loggingEntries.clear();
    }//GEN-LAST:event_btnClearActionPerformed

    private void btnSetAddressActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSetAddressActionPerformed
        IPProvider.setForcedAddress(textAddress.getText());
    }//GEN-LAST:event_btnSetAddressActionPerformed

    private void btnRefreshActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRefreshActionPerformed
        intelligenceMain.triggerResearch();
    }//GEN-LAST:event_btnRefreshActionPerformed

    private void btnReconnectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnReconnectActionPerformed
        intelligenceMain.triggerDiscover();
    }//GEN-LAST:event_btnReconnectActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnClear;
    private javax.swing.JButton btnReconnect;
    private javax.swing.JButton btnRefresh;
    private javax.swing.JButton btnSetAddress;
    private intelligence.IntelligenceMain intelligenceMain;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.DefaultListModel loggingEntries;
    private javax.swing.JList loggingList;
    private javax.swing.JScrollPane loggingScroller;
    private intelligence.RescrollingThread rescroller;
    private javax.swing.JTextField textAddress;
    // End of variables declaration//GEN-END:variables
}
