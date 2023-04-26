
import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;

import tage.networking.server.GameConnectionServer;
import tage.networking.server.IClientInfo;
public class GameServerUDP extends GameConnectionServer<UUID>
{
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
}