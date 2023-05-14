
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import jdk.nashorn.api.scripting.ScriptObjectMirror;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.joml.Vector3f;

import tage.networking.server.GameConnectionServer;
import tage.networking.server.IClientInfo;
public class GameServerUDP<K> extends GameConnectionServer<UUID>
{
	private ArrayList<Float> ballStart = new ArrayList<>();
	float ballX;
	float ballY;
	float ballZ;
	UUID firstClient;
	private int ballHealth;
	
    public GameServerUDP(int localPort) throws IOException{ 
        super(localPort, ProtocolType.UDP); 
    }

    @Override
    public void processPacket(Object o, InetAddress senderIP, int sndPort) {
        String message = (String) o;
        String[] msgTokens = message.split(",");
        if(msgTokens.length > 0) {
            // case where server receives a JOIN message
            // format: join,localid
            if(msgTokens[0].compareTo("join") == 0) { 
                try{ 
                    IClientInfo ci;
                    ci = getServerSocket().createClientInfo(senderIP, sndPort);
                    UUID clientID = UUID.fromString(msgTokens[1]);
                    addClient(ci, clientID);
                    sendJoinedMessage(clientID, true);
					handleBalls(clientID);
                }
                catch (IOException e){ 
                    e.printStackTrace();
                } 
            }
            // case where server receives a CREATE message
            // format: create,localid,x,y,z
            if(msgTokens[0].compareTo("create") == 0)
            { UUID clientID = UUID.fromString(msgTokens[1]);
            String[] pos = {msgTokens[2], msgTokens[3], msgTokens[4]};
            sendCreateMessages(clientID, pos);
            sendWantsDetailsMessages(clientID);
            }
            // case where server receives a BYE message
            // format: bye,localid
            if(msgTokens[0].compareTo("bye") == 0)
            { UUID clientID = UUID.fromString(msgTokens[1]);
            sendByeMessages(clientID);
            removeClient(clientID);
            }
            // case where server receives a DETAILS-FOR message
            if(msgTokens[0].compareTo("dsfr") == 0){
				UUID clientID = UUID.fromString(msgTokens[1]);
				UUID remoteID = UUID.fromString(msgTokens[2]);
				String[] pos = {msgTokens[3], msgTokens[4], msgTokens[5]};
				sendDetailsForMessage(clientID, remoteID, pos);
			}
            // case where server receives a MOVE message
            if(msgTokens[0].compareTo("move") == 0){ 
				UUID clientID = UUID.fromString(msgTokens[1]);
				String[] pos = {msgTokens[2], msgTokens[3], msgTokens[4]};
				sendMoveMessages(clientID, pos);
			}
			if(msgTokens[0].compareTo("ballLoc") == 0){ 
				ArrayList<Float> ballPosition = new ArrayList<Float>();
				ballPosition.add(Float.parseFloat(msgTokens[2])); //X
				ballPosition.add(Float.parseFloat(msgTokens[3])); //Y
				ballPosition.add(Float.parseFloat(msgTokens[4])); //Z
				crUpdateBall(ballPosition);
			}
			if(msgTokens[0].compareTo("ballHealth") == 0){ 
				UUID clientID = UUID.fromString(msgTokens[1]);
				ballHealth = Integer.parseInt(msgTokens[2]);
				updateHealth(ballHealth, clientID);
			}
        }
    } 

    public void sendJoinedMessage(UUID clientID, boolean success)
	{	try 
		{	System.out.println("trying to confirm join");
			String message = new String("join,");
			if(success)
				message += "success";
			else
				message += "failure";
			System.out.println(message);
			sendPacket(message, clientID);
			System.out.println("finished sending join message");
		} 
		catch (IOException e) 
		{	e.printStackTrace();
	}	}
	
	// Informs a client that the avatar with the identifier remoteId has left the server. 
	// This message is meant to be sent to all client currently connected to the server 
	// when a client leaves the server.
	// Message Format: (bye,remoteId)
	
	public void sendByeMessages(UUID clientID)
	{	try 
		{	String message = new String("bye," + clientID.toString());
			forwardPacketToAll(message, clientID);
		} 
		catch (IOException e) 
		{	e.printStackTrace();
	}	}
	
	// Informs a client that a new avatar has joined the server with the unique identifier 
	// remoteId. This message is intended to be send to all clients currently connected to 
	// the server when a new client has joined the server and sent a create message to the 
	// server. This message also triggers WANTS_DETAILS messages to be sent to all client 
	// connected to the server. 
	// Message Format: (create,remoteId,x,y,z) where x, y, and z represent the position

	public void sendCreateMessages(UUID clientID, String[] position)
	{	try 
		{	String message = new String("create," + clientID.toString());
			message += "," + position[0];
			message += "," + position[1];
			message += "," + position[2];	
			forwardPacketToAll(message, clientID);
		} 
		catch (IOException e) 
		{	e.printStackTrace();
	}	}
	
	// Informs a client of the details for a remote client�s avatar. This message is in response 
	// to the server receiving a DETAILS_FOR message from a remote client. That remote client�s 
	// message�s localId becomes the remoteId for this message, and the remote client�s message�s 
	// remoteId is used to send this message to the proper client. 
	// Message Format: (dsfr,remoteId,x,y,z) where x, y, and z represent the position.

	public void sendDetailsForMessage(UUID clientID, UUID remoteId, String[] position)
	{	try 
		{	String message = new String("dsfr," + remoteId.toString());
			message += "," + position[0];
			message += "," + position[1];
			message += "," + position[2];	
			sendPacket(message, clientID);
		} 
		catch (IOException e) 
		{	e.printStackTrace();
	}	}
	
	// Informs a local client that a remote client wants the local client�s avatar�s information. 
	// This message is meant to be sent to all clients connected to the server when a new client 
	// joins the server. 
	// Message Format: (wsds,remoteId)
	
	public void sendWantsDetailsMessages(UUID clientID)
	{	try 
		{	String message = new String("wsds," + clientID.toString());	
			forwardPacketToAll(message, clientID);
		} 
		catch (IOException e) 
		{	e.printStackTrace();
	}	}
	
	// Informs a client that a remote client�s avatar has changed position. x, y, and z represent 
	// the new position of the remote avatar. This message is meant to be forwarded to all clients
	// connected to the server when it receives a MOVE message from the remote client.   
	// Message Format: (move,remoteId,x,y,z) where x, y, and z represent the position.

	public void sendMoveMessages(UUID clientID, String[] position)
	{	try 
		{	String message = new String("move," + clientID.toString());
			message += "," + position[0];
			message += "," + position[1];
			message += "," + position[2];
			forwardPacketToAll(message, clientID);
		} 
		catch (IOException e) 
		{	e.printStackTrace();
	}	}

	public ArrayList<Float> initScript() {
		ScriptEngineManager factory = new ScriptEngineManager();
		String scriptFileName = "./ballStart.js";
		// get a list of the script engines on this platform
		List<ScriptEngineFactory> list = factory.getEngineFactories();
		System.out.println("Script Engine Factories found:");
		for (ScriptEngineFactory f : list)
		{ 
			System.out.println(
			" Name = " + f.getEngineName()
			+ " language = " + f.getLanguageName()
			+ " extensions = " + f.getExtensions());
		}
		// get the JavaScript engine
		ScriptEngine jsEngine = factory.getEngineByName("js");
		// run the script
		return(executeScript(jsEngine, scriptFileName));
	}

	private ArrayList<Float> executeScript(ScriptEngine engine, String scriptFileName){
		try{ 
			FileReader fileReader = new FileReader(scriptFileName);
			engine.eval(fileReader); //execute the script statements in the file
			Invocable invEngine = (Invocable) engine;
			Object [] arg = {};
			try{
				Collection<Object> ballSt =  ((ScriptObjectMirror) invEngine.invokeFunction("getStart",arg)).values();
				for (Object s : ballSt){
					System.out.println("ball start is " + s);
					ballStart.add(Float.valueOf(s.toString()));
				}
			}
			catch (ScriptException ex){
				System.out.println(scriptFileName + "method not able to execute "); 
			}
			catch(NoSuchMethodException ex){
				System.out.println(scriptFileName + "method not able to execute "); 
			}

			fileReader.close();
		}
		catch (FileNotFoundException e1){ 
			System.out.println(scriptFileName + " not found " + e1); 
		}
		catch (IOException e2){ 
			System.out.println("IO problem with " + scriptFileName + e2); 
		}
		catch (ScriptException e3){ 
			System.out.println("ScriptException in " + scriptFileName + e3); 
		}
		catch (NullPointerException e4){ 
			System.out.println ("Null ptr exception in " + scriptFileName + e4); 
		}
		//if ball height is below terrain height put it above terrain
		ballX = ballStart.get(0);
		ballY = ballStart.get(1);
		ballZ = ballStart.get(2);
		ballHealth =10;
		//Send x y and z to client so they can update their balls 
		return ballStart;
	}

	public void handleBalls(UUID clientID){
		ConcurrentHashMap<UUID, IClientInfo> clients = getClients();
		Collection<IClientInfo> clientVals = clients.values();
		List<IClientInfo> list = new ArrayList<IClientInfo>(clientVals);
		//TODO might want to send another message for updating health here

		//if it's the first client joining, then run the script to create the ball
		if(list.size()==1){
			firstClient = clientID;
			ArrayList<Float> ballInitValue = initScript();
			String message = new String("createBall," + clientID.toString());
			message += "," + ballInitValue.get(0); //X
			message += "," + ballInitValue.get(1); //Y
			message += "," + ballInitValue.get(2); //Z
			message += "," +  ballHealth;
			try{
				sendPacketToAll(message);
			}
			catch(IOException i){
				System.out.println("no balls");
			}
		}
		else if(list.size()>1){
			//ask client 1 for ball details
			String message = new String("getBall," + clientID.toString());
			try {
				sendPacket(message, firstClient);
			}
			catch(IOException i){
				System.out.println("unable to request balls from client");
			}
		}
	}

	public void crUpdateBall(ArrayList<Float> ballLoc){
		String message = new String("crUpdateBall," + firstClient.toString());
		message += "," + ballX;
		message += "," + ballY;
		message += "," + ballZ;
		message += "," +  ballHealth;
		try{
			forwardPacketToAll(message,firstClient);
		}
		catch(IOException i){
			System.out.println("no ball updates");
		}
	}

	public void updateHealth(int health, UUID clientId){
		ballHealth = health;
		try{
            String message = new String("ballHealth," + clientId.toString());
            message += "," + health;
            forwardPacketToAll(message, clientId);
        }
        catch(IOException e){
            e.printStackTrace();
        }
	}
}