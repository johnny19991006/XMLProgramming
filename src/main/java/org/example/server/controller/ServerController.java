package org.example.server.controller;

import org.example.server.domain.MessengerServer;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class ServerController {
    public void run() {
        MessengerServer ms = new MessengerServer();

        ms.addWindowListener(
                new WindowAdapter() {
                    public void windowClosing( WindowEvent e ) {
                        System.exit( 0 );
                    }
                }
        );

        ms.runServer();
    }
}
