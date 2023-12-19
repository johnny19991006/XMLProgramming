package org.example.client.controller;

import org.example.client.domain.MessengerClient;

public class ClientController {
public void run() {
    MessengerClient cm = new MessengerClient();

    cm.runMessengerClient();
    }
}
