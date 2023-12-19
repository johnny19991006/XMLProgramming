package org.example.client.domain;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import java.net.*;
import java.util.*;

import org.example.server.repository.DatabaseManager;
import org.w3c.dom.*;
import org.xml.sax.*;
import javax.xml.parsers.*;
//import com.sun.xml.tree.XmlDocument;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;


public class MessengerClient extends JFrame
{
    private JPanel centerPanel, namePanel;
    private JLabel status, nameLab;
    private JTextField name;
    private ImageIcon bug;
    private JButton submit;
    private Socket clientSocket;
    private OutputStream output;
    private InputStream input;
    private boolean keepListening;
    private ClientStatus clientStatus;
    private Document users;
    private Vector<Conversation> conversations;
    private DocumentBuilderFactory factory;
    private DocumentBuilder builder;

    public MessengerClient()
    {

        super ( "Messenger Client" );

        try
        {

            factory = DocumentBuilderFactory.newInstance();


            builder = factory.newDocumentBuilder();
        }
        catch ( ParserConfigurationException pce ) {
            pce.printStackTrace();
        }


        Container c = getContentPane();

        centerPanel = new JPanel( new GridLayout( 2, 1 ) );

        namePanel = new JPanel();

        nameLab = new JLabel( "Please enter your name: " );
        namePanel.add( nameLab );

        name = new JTextField( 15 );
        namePanel.add( name );

        centerPanel.add( namePanel );

        bug = new ImageIcon( "travelbug.jpg" );
        submit = new JButton( "Submit", bug );
        submit.setEnabled( false );
        centerPanel.add( submit );

        submit.addActionListener(
                new ActionListener() {
                    public void actionPerformed( ActionEvent e ) {
                        loginUser();
                    }
                }
        );

        c.add( centerPanel, BorderLayout.CENTER );

        status = new JLabel( "Status: Not connected" );
        c.add( status, BorderLayout.SOUTH );

        addWindowListener(
                new WindowAdapter() {
                    public void windowClosing( WindowEvent e ) {
                        System.exit( 0 );
                    }
                }
        );

        setSize( 200, 200 );
        setVisible(true);
        //initializeDatabase();
    }

    public void runMessengerClient()
    {
        try {
            clientSocket = new Socket(
                    InetAddress.getByName( "127.0.0.1" ), 5000 );
            status.setText( "Status: Connected to " +
                    clientSocket.getInetAddress().getHostName() );//HTTP응답코드를 준다, CRUD를 한다.(DB연결)


            output = clientSocket.getOutputStream();
            input = clientSocket.getInputStream();

            submit.setEnabled( true );
            keepListening = true;

            int bufferSize = 0;

            while ( keepListening ) {

                bufferSize = input.available();

                if ( bufferSize > 0 ) {
                    byte buf[] = new byte[ bufferSize ];

                    input.read( buf );

                    InputSource source = new InputSource(
                            new ByteArrayInputStream( buf ) );
                    Document message;

                    try {


                        message = builder.parse( source );

                        if ( message != null )
                            messageReceived( message );

                    }
                    catch ( SAXException se ) {
                        se.printStackTrace();
                    }
                    catch ( Exception e ) {
                        e.printStackTrace();
                    }
                }
            }

            input.close();
            output.close();
            clientSocket.close();
            System.exit( 0 );
        }
        catch ( IOException e ) {
            e.printStackTrace();
            System.exit( 1 );
        }
    }

    public void loginUser()
    {

        Document submitName = builder.newDocument();
        Element root = submitName.createElement( "user" );

        submitName.appendChild( root );
        root.appendChild(
                submitName.createTextNode( name.getText() ) );

        send( submitName );
    }

    public Document getUsers()
    {
        return users;
    }

    public void stopListening()
    {
        keepListening = false;
    }

    public void messageReceived( Document message )
    {
        Element root = message.getDocumentElement();

        if ( root.getTagName().equals( "nameInUse" ) )

            JOptionPane.showMessageDialog( this,
                    "That name is already in use." +
                            "\nPlease enter a unique name." );
        else if ( root.getTagName().equals( "users" ) ) {

            users = message;
            clientStatus = new ClientStatus( name.getText(), this );
            conversations = new Vector<Conversation>();
            setVisible(false);
        }
        else if ( root.getTagName().equals( "update" ) ) {


            String type = root.getAttribute( "type" );
            NodeList userElt = root.getElementsByTagName( "user" );
            String updatedUser =
                    userElt.item( 0 ).getFirstChild().getNodeValue();


            if ( type.equals( "login" ) )

                clientStatus.add( updatedUser );
            else {

                clientStatus.remove( updatedUser );


                int index = findConversationIndex( updatedUser );

                if ( index != -1 ) {
                    Conversation receiver =

                            conversations.elementAt( index );

                    receiver.updateGUI( updatedUser + " logged out" );
                    receiver.disableConversation();
                }
            }
        }
        else if ( root.getTagName().equals( "message" ) ) {
            String from = root.getAttribute( "from" );
            String messageText = root.getFirstChild().getNodeValue();


            int index = findConversationIndex( from );

            if ( index != -1 ) {

                Conversation receiver =

                        conversations.elementAt( index );
                receiver.updateGUI( from + ":  " + messageText );
            }
            else {

                Conversation newConv =
                        new Conversation( from, clientStatus, this );
                newConv.updateGUI( from + ":  " + messageText );
            }
        }
    }

    public int findConversationIndex( String userName )
    {
        for ( int i = 0; i < conversations.size(); i++ ) {
            Conversation current =
                    conversations.elementAt( i );

            if ( current.getTarget().equals( userName ) )
                return i;
        }

        return -1;
    }

    public void addConversation( Conversation newConversation )
    {
        conversations.add( newConversation );
    }

    public void removeConversation( String userName )
    {
        conversations.removeElementAt(
                findConversationIndex( userName ) );
    }
    /*private void initializeDatabase() {
        databaseManager = new DatabaseManager();
    }*/

    /*public void sendMessage(String messageText) {
        Document sendMessage = builder.newDocument();
        Element root = sendMessage.createElement("message");
        sendMessage.appendChild(root);
        root.appendChild(sendMessage.createTextNode(messageText));

        send(sendMessage);
        databaseManager.saveMessage(name.getText(),messageText);
    }*/

    public void send( Document message )
    {
        try {

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer serializer = transformerFactory.newTransformer();
            //StringWriter writer = new StringWriter();
            serializer.transform( new DOMSource (message), new StreamResult(output));
            //String messageText = writer.toString();
            //String sender = name.getText();
            //databaseManager.saveMessage(sender, messageText);

        }
        catch ( Exception e ) {
            e.printStackTrace();
        }
    }


}
