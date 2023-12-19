package org.example.server;

import org.example.server.controller.ServerController;

public class ServerMain {
    public static void main(String[] args) {
        new ServerController().run();
    }
}
