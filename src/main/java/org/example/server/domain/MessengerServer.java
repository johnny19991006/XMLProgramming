package org.example.server.domain;

import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.io.*;
import javax.swing.*;
import java.util.*;

import org.example.server.repository.DatabaseManager;
import org.w3c.dom.*;

import javax.xml.parsers.*;
//import com.sun.xml.tree.XmlDocument;

public class MessengerServer extends JFrame {
    private JLabel status;
    private JTextArea display;
    private Vector<UserThread> onlineUsers;
    private DocumentBuilderFactory factory;
    private DocumentBuilder builder;
    private Document users;

    private DatabaseManager databaseManager;
    public MessengerServer()
    {
        // create GUI
        super ( "Messenger Server" );

        try {

            // obtain the default parser
            factory = DocumentBuilderFactory.newInstance();

            // get DocumentBuilder
            builder = factory.newDocumentBuilder();
        }
        catch ( ParserConfigurationException pce ) {
            pce.printStackTrace();
        }

        Container c = getContentPane();

        status = new JLabel( "Status" );
        c.add( status, BorderLayout.NORTH );

        display = new JTextArea();
        display.setLineWrap( true );
        display.setEditable( false );
        c.add( new JScrollPane( display ), BorderLayout.CENTER );
        display.append( "Server waiting for connections\n" );

        setSize( 300, 300 );
        //show();
        setVisible(true);

        // initialize variables
        onlineUsers = new Vector<UserThread>();
        users = initUsers();

        databaseManager = new DatabaseManager();
    }

    public void runServer()
    {
        ServerSocket server;

        try {
            // create a ServerSocket
            server = new ServerSocket( 5000, 100 );

            // wait for connections
            while ( true ) {
                Socket clientSocket = server.accept();

                display.append( "\nConnection received from: " +
                        clientSocket.getInetAddress().getHostName() );

                UserThread newUser =
                        new UserThread( clientSocket, this );

                newUser.start();
            }
        }
        catch ( IOException e ) {
            e.printStackTrace();
            System.exit( 1 );
        }
    }

    //남의 코드 가지고 올때는 메서드 남겨놓고 사용!
    private Document initUsers()
    {

        Document init = builder.newDocument();

        init.appendChild( init.createElement( "users" ) );
        return init;
    }

    public void updateGUI( String s )
    {
        display.append( "\n" + s );
    }

    public Document getUsers()
    {
        return users;
    }

    public void addUser( UserThread newUserThread )
    {
        // get new user's name
        String userName = newUserThread.getUsername();

        updateGUI( "Received new user: " + userName );

        // notify all users of user's login
        updateUsers( userName, "login" );

        // add new user element to Document users
        Element usersRoot = users.getDocumentElement();
        Element newUser = users.createElement( "user" );

        newUser.appendChild(
                users.createTextNode( userName ) );
        usersRoot.appendChild( newUser );

        updateGUI( "Added user: " + userName );

        // add to Vector onlineUsers
        onlineUsers.addElement( newUserThread );

        databaseManager.addUser(userName);
    }

    public void sendMessage( Document message )
    {
        // transfer message to specified receiver
        Element root = message.getDocumentElement();
        String from = root.getAttribute( "from" );
        String to = root.getAttribute( "to" );
        int index = findUserIndex( to );

        updateGUI( "Received message To: " + to + ",  From: " + from );

        // send message to corresponding user
        UserThread receiver =
                //( UserThread ) onlineUsers.elementAt( index );
                onlineUsers.elementAt( index );
        receiver.send( message );
        updateGUI( "Sent message To: " + to +
                ",  From: " + from );

        String messageText = root.getTextContent();

        databaseManager.saveMessage(from, to, messageText);
    }

    public void updateUsers( String userName, String type )
    {
        // create xml update document
        Document doc = builder.newDocument();
        Element root = doc.createElement( "update" );
        Element userElt = doc.createElement( "user" );

        doc.appendChild( root );
        root.setAttribute( "type", type );
        root.appendChild( userElt );
        userElt.appendChild( doc.createTextNode( userName ) );

        // send to all users
        for ( int i = 0; i < onlineUsers.size(); i++ ) {
            UserThread receiver =
                    //( UserThread ) onlineUsers.elementAt( i );
                    onlineUsers.elementAt( i );
            receiver.send( doc );
        }

        updateGUI( "Notified online users of " +
                userName + "'s " + type );
    }

    public int findUserIndex( String userName )
    {

        for ( int i = 0; i < onlineUsers.size(); i++ ) {
            UserThread current =

                    onlineUsers.elementAt( i );

            if ( current.getUsername().equals( userName ) )
                return i;
        }

        return -1;
    }

    public void removeUser( String userName )
    {

        int index = findUserIndex( userName );

        onlineUsers.removeElementAt( index );

        NodeList userElements =
                users.getDocumentElement().getElementsByTagName(
                        "user" );

        for ( int i = 0; i < userElements.getLength(); i++ ) {
            String str =
                    userElements.item( i ).getFirstChild().getNodeValue();

            if ( str.equals( userName ) )
                users.getDocumentElement().removeChild(
                        userElements.item( i ) );

        }

        updateGUI( "Removed user: " + userName );

        updateUsers( userName, "logout" );
    }
}
