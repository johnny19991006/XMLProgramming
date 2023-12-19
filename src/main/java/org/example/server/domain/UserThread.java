package org.example.server.domain;

import java.net.*;
import java.io.*;

import org.w3c.dom.*;
import org.xml.sax.*;
import javax.xml.parsers.*;
//import com.sun.xml.tree.XmlDocument;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;


public class UserThread extends Thread
{
    private Socket connection;
    private InputStream input;
    private OutputStream output;
    private MessengerServer server;
    private String username = "";
    private boolean keepListening;
    private DocumentBuilderFactory factory;
    private DocumentBuilder builder;

    public UserThread( Socket s, MessengerServer ms )
    {
        try
        {
            // obtain the default parser
            factory = DocumentBuilderFactory.newInstance();

            // get DocumentBuilder
            builder = factory.newDocumentBuilder();
        }
        catch ( ParserConfigurationException pce ) {
            pce.printStackTrace();
            System.exit( 1 );
        }

        // initialize variables
        connection = s;
        server = ms;
        keepListening = true;

        // get input and output streams
        try {
            input = connection.getInputStream();
            output = connection.getOutputStream();
        }
        catch ( IOException e ) {
            e.printStackTrace();
            System.exit( 1 );
        }
    }

    public String getUsername()
    {
        return username;
    }

    public void messageReceived( Document message )
    {
        Element root = message.getDocumentElement();

        if ( root.getTagName().equals( "user" ) ) {


            String enteredName = root.getFirstChild().getNodeValue();

            if ( server.findUserIndex( enteredName ) != -1 )
                nameInUse(); // not a unique name
            else {


                send( server.getUsers() );

                username = enteredName; // update username variable

                // add user to server
                server.addUser( this );
            }
        }
        else if ( root.getTagName().equals( "message" ) )
            server.sendMessage( message );
        else if ( root.getTagName().equals( "disconnect" ) ) {
            keepListening = false;

            // remove user from server
            server.removeUser( username );
        }
    }

    private void nameInUse()
    {
        Document enterUniqueName = builder.newDocument();

        enterUniqueName.appendChild(
                enterUniqueName.createElement( "nameInUse" ) );

        send( enterUniqueName );
    }

    public void send( Document message )
    {
        try {
            // write to output stream
            //( ( XmlDocument )message).write( output );
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer serializer = transformerFactory.newTransformer();
            serializer.transform( new DOMSource (message), new StreamResult(output));
        }
        //catch ( IOException e ) {
        catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    public void run()
    {
        try {
            int bufferSize = 0;

            while ( keepListening ) {
                bufferSize = input.available();

                if ( bufferSize > 0 ) {
                    byte buf[] = new byte[ bufferSize ];

                    input.read( buf );

                    InputSource source = new InputSource(
                            new ByteArrayInputStream( buf ) );
                    Document message = builder.parse( source );

                    if ( message != null )
                        messageReceived( message );
                }
            }

            input.close();
            output.close();
            connection.close();
        }
        catch ( SAXException e ) {
            e.printStackTrace();
        }
        catch ( IOException e ) {
            e.printStackTrace();
        }
    }
}
