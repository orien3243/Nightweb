package net.i2p.client.streaming;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Properties;

import net.i2p.I2PAppContext;
import net.i2p.I2PException;
import net.i2p.client.I2PClient;
import net.i2p.client.I2PClientFactory;
import net.i2p.client.I2PSession;
import net.i2p.client.I2PSessionException;
import net.i2p.util.Log;

/**
 * Simplify the creation of I2PSession and transient I2P Destination objects if 
 * necessary to create a socket manager.  This class is most likely how classes
 * will begin their use of the socket library
 *
 */
public class I2PSocketManagerFactory {

    public static final String PROP_MANAGER = "i2p.streaming.manager";
    public static final String DEFAULT_MANAGER = "net.i2p.client.streaming.impl.I2PSocketManagerFull";
    
    /**
     * Create a socket manager using a brand new destination connected to the
     * I2CP router on the local machine on the default port (7654).
     * 
     * Blocks for a long time while the router builds tunnels.
     * 
     * @return the newly created socket manager, or null if there were errors
     */
    public static I2PSocketManager createManager() {
        return createManager(getHost(), getPort(), (Properties) System.getProperties().clone());
    }
    
    /**
     * Create a socket manager using a brand new destination connected to the
     * I2CP router on the local machine on the default port (7654).
     * 
     * Blocks for a long time while the router builds tunnels.
     * 
     * @param opts Streaming and I2CP options, may be null
     * @return the newly created socket manager, or null if there were errors
     */
    public static I2PSocketManager createManager(Properties opts) {
        return createManager(getHost(), getPort(), opts);
    }

    /**
     * Create a socket manager using a brand new destination connected to the
     * I2CP router on the specified host and port.
     * 
     * Blocks for a long time while the router builds tunnels.
     * 
     * @param host I2CP host null to use default
     * @param port I2CP port <= 0 to use default
     * @return the newly created socket manager, or null if there were errors
     */
    public static I2PSocketManager createManager(String host, int port) {
        return createManager(host, port, (Properties) System.getProperties().clone());
    }

    /**
     * Create a socket manager using a brand new destination connected to the
     * I2CP router on the given machine reachable through the given port.
     * 
     * Blocks for a long time while the router builds tunnels.
     *
     * @param i2cpHost I2CP host null to use default
     * @param i2cpPort I2CP port <= 0 to use default
     * @param opts Streaming and I2CP options, may be null
     * @return the newly created socket manager, or null if there were errors
     */
    public static I2PSocketManager createManager(String i2cpHost, int i2cpPort, Properties opts) {
        I2PClient client = I2PClientFactory.createClient();
        ByteArrayOutputStream keyStream = new ByteArrayOutputStream(512);
        try {
            client.createDestination(keyStream);
            ByteArrayInputStream in = new ByteArrayInputStream(keyStream.toByteArray());
            return createManager(in, i2cpHost, i2cpPort, opts);
        } catch (IOException ioe) {
            getLog().error("Error creating the destination for socket manager", ioe);
            return null;
        } catch (I2PException ie) {
            getLog().error("Error creating the destination for socket manager", ie);
            return null;
        }
    }

    /**
     * Create a socket manager using the destination loaded from the given private key
     * stream and connected to the default I2CP host and port.
     * 
     * Blocks for a long time while the router builds tunnels.
     *
     * @param myPrivateKeyStream private key stream, format is specified in {@link net.i2p.data.PrivateKeyFile PrivateKeyFile}
     *                           or null for a transient destination. Caller must close.
     * @return the newly created socket manager, or null if there were errors
     */
    public static I2PSocketManager createManager(InputStream myPrivateKeyStream) {
        return createManager(myPrivateKeyStream, getHost(), getPort(), (Properties) System.getProperties().clone());
    }
    
    /**
     * Create a socket manager using the destination loaded from the given private key
     * stream and connected to the default I2CP host and port.
     * 
     * Blocks for a long time while the router builds tunnels.
     *
     * @param myPrivateKeyStream private key stream, format is specified in {@link net.i2p.data.PrivateKeyFile PrivateKeyFile}
     *                           or null for a transient destination. Caller must close.
     * @param opts Streaming and I2CP options, may be null
     * @return the newly created socket manager, or null if there were errors
     */
    public static I2PSocketManager createManager(InputStream myPrivateKeyStream, Properties opts) {
        return createManager(myPrivateKeyStream, getHost(), getPort(), opts);
    }
    
    /**
     * Create a socket manager using the destination loaded from the given private key
     * stream and connected to the I2CP router on the specified machine on the given
     * port.
     * 
     * Blocks for a long time while the router builds tunnels.
     *
     * @param myPrivateKeyStream private key stream, format is specified in {@link net.i2p.data.PrivateKeyFile PrivateKeyFile}
     *                           or null for a transient destination. Caller must close.
     * @param i2cpHost I2CP host null to use default
     * @param i2cpPort I2CP port <= 0 to use default
     * @param opts Streaming and I2CP options, may be null
     * @return the newly created socket manager, or null if there were errors
     */
    public static I2PSocketManager createManager(InputStream myPrivateKeyStream, String i2cpHost, int i2cpPort,
                                                 Properties opts) {
        try {
            return createManager(myPrivateKeyStream, i2cpHost, i2cpPort, opts, true);
        } catch (I2PSessionException ise) {
            getLog().error("Error creating session for socket manager", ise);
            return null;
        }
    }
    
    /**
     * Create a disconnected socket manager using the destination loaded from the given private key
     * stream, or null for a transient destination.
     * 
     * Non-blocking. Does not connect to the router or build tunnels.
     * For servers, caller MUST call getSession().connect() to build tunnels and start listening.
     * For clients, caller may do that to build tunnels in advance;
     * otherwise, the first call to connect() will initiate a connection to the router,
     * with significant delay for tunnel building.
     *
     * @param myPrivateKeyStream private key stream, format is specified in {@link net.i2p.data.PrivateKeyFile PrivateKeyFile}
     *                           or null for a transient destination. Caller must close.
     * @param i2cpHost I2CP host null to use default
     * @param i2cpPort I2CP port <= 0 to use default
     * @param opts Streaming and I2CP options, may be null
     * @return the newly created socket manager, non-null (throws on error)
     * @since 0.9.8
     */
    public static I2PSocketManager createDisconnectedManager(InputStream myPrivateKeyStream, String i2cpHost,
                                                             int i2cpPort, Properties opts) throws I2PSessionException {
        if (myPrivateKeyStream == null) {
            I2PClient client = I2PClientFactory.createClient();
            ByteArrayOutputStream keyStream = new ByteArrayOutputStream(512);
            try {
                client.createDestination(keyStream);
            } catch (Exception e) {
                throw new I2PSessionException("Error creating keys", e);
            }
            myPrivateKeyStream = new ByteArrayInputStream(keyStream.toByteArray());
        }
        return createManager(myPrivateKeyStream, i2cpHost, i2cpPort, opts, false);
    }
    
    /**
     * Create a socket manager using the destination loaded from the given private key
     * stream and connected to the I2CP router on the specified machine on the given
     * port.
     * 
     * Blocks for a long time while the router builds tunnels if connect is true.
     *
     * @param myPrivateKeyStream private key stream, format is specified in {@link net.i2p.data.PrivateKeyFile PrivateKeyFile}
     *                           non-null. Caller must close.
     * @param i2cpHost I2CP host null to use default
     * @param i2cpPort I2CP port <= 0 to use default
     * @param opts Streaming and I2CP options, may be null
     * @param connect true to connect (blocking)
     * @return the newly created socket manager, non-null (throws on error)
     * @since 0.9.7
     */
    private static I2PSocketManager createManager(InputStream myPrivateKeyStream, String i2cpHost, int i2cpPort,
                                                 Properties opts, boolean connect) throws I2PSessionException {
        I2PClient client = I2PClientFactory.createClient();
        if (opts == null)
            opts = new Properties();
        Properties syscopy = (Properties) System.getProperties().clone();
        for (Map.Entry<Object, Object> e : syscopy.entrySet()) {
            String name = (String) e.getKey();
            if (!opts.containsKey(name))
                opts.setProperty(name, (String) e.getValue());
        }
        // as of 0.8.1 (I2CP default is BestEffort)
        if (!opts.containsKey(I2PClient.PROP_RELIABILITY))
            opts.setProperty(I2PClient.PROP_RELIABILITY, I2PClient.PROP_RELIABILITY_NONE);

        if (i2cpHost != null)
            opts.setProperty(I2PClient.PROP_TCP_HOST, i2cpHost);
        if (i2cpPort > 0)
            opts.setProperty(I2PClient.PROP_TCP_PORT, "" + i2cpPort);
        
        I2PSession session = client.createSession(myPrivateKeyStream, opts);
        if (connect)
            session.connect();
        I2PSocketManager sockMgr = createManager(session, opts, "manager");
        return sockMgr;
    }

    private static I2PSocketManager createManager(I2PSession session, Properties opts, String name) {
        I2PAppContext context = I2PAppContext.getGlobalContext();
        String classname = opts.getProperty(PROP_MANAGER, DEFAULT_MANAGER);
        try {
            Class<?> cls = Class.forName(classname);
            if (!I2PSocketManager.class.isAssignableFrom(cls))
                throw new IllegalArgumentException(classname + " is not an I2PSocketManager");
            Constructor<I2PSocketManager> con = (Constructor<I2PSocketManager>)
                  cls.getConstructor(new Class[] {I2PAppContext.class, I2PSession.class, Properties.class, String.class});
            I2PSocketManager mgr = con.newInstance(new Object[] {context, session, opts, name});
            return mgr;
        } catch (Throwable t) {
            getLog().log(Log.CRIT, "Error loading " + classname, t);
            throw new IllegalStateException(t);
        }

    }

    private static String getHost() {
        return System.getProperty(I2PClient.PROP_TCP_HOST, "127.0.0.1");
    }

    private static int getPort() {
        int i2cpPort = 7654;
        String i2cpPortStr = System.getProperty(I2PClient.PROP_TCP_PORT);
        if (i2cpPortStr != null) {
            try {
                i2cpPort = Integer.parseInt(i2cpPortStr);
            } catch (NumberFormatException nfe) {
                // gobble gobble
            }
        }
        return i2cpPort;
    }

    /** @since 0.9.7 */
    private static Log getLog() {
        return I2PAppContext.getGlobalContext().logManager().getLog(I2PSocketManagerFactory.class);
    }
}
