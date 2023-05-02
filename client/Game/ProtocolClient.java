package Game;

import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;

import tage.networking.server.GameConnectionServer;
import tage.networking.server.IClientInfo;
import tage.networking.client.GameConnectionClient;
import org.joml.*;


public class ProtocolClient extends GameConnectionClient
{ 
    private MyGame game;
    private UUID id;
    private GhostManager ghostManager;
    public ProtocolClient(InetAddress remAddr, int remPort, ProtocolType pType, MyGame game) throws IOException { 
        super(remAddr, remPort, pType);
        this.game = game;
        this.id = UUID.randomUUID();
        ghostManager = game.getGhostManager();
    }

    @Override
    protected void processPacket(Object msg) { 
        String strMessage = (String) msg;
        System.out.println("message received -->" + strMessage);
        String[] messageTokens = strMessage.split(",");
        if(messageTokens.length > 0) {
            if(messageTokens[0].compareTo("join") == 0) { // format: join, success or join, failure
                if(messageTokens[1].compareTo("success") == 0) { 
                    System.out.println("join success confirmed");
                    game.setIsConnected(true);
                    sendCreateMessage(game.getPlayerPosition());
                }
                if(messageTokens[1].compareTo("failure") == 0) { 
                    System.out.println("join failure confirmed");
                    game.setIsConnected(false);
                } 
            }
            if(messageTokens[0].compareTo("bye") == 0) { // format: bye, remoteId
                UUID ghostID = UUID.fromString(messageTokens[1]);
                ghostManager.removeGhostAvatar(ghostID);
            }

            if ((messageTokens[0].compareTo("dsfr") == 0 )
            || (messageTokens[0].compareTo("create")==0)) { // format: create, remoteId, x,y,z or dsfr, remoteId, x,y,z
                UUID ghostID = UUID.fromString(messageTokens[1]);
                Vector3f ghostPosition = new Vector3f(
                Float.parseFloat(messageTokens[2]),
                Float.parseFloat(messageTokens[3]),
                Float.parseFloat(messageTokens[4]));
                try { 
                    ghostManager.createGhost(ghostID, ghostPosition);
                } 
                catch (IOException e) { 
                    System.out.println("error creating ghost avatar");
                } 
            }
            if(messageTokens[0].compareTo("wsds") == 0) {
                // Send the local client's avatar's information
				// Parse out the id into a UUID
				UUID ghostID = UUID.fromString(messageTokens[1]);
				sendDetailsForMessage(ghostID, game.getPlayerPosition());
            }
            if(messageTokens[0].compareTo("move") == 0) { 
                // move a ghost avatar
				// Parse out the id into a UUID
				UUID ghostID = UUID.fromString(messageTokens[1]);
				
				// Parse out the position into a Vector3f
				Vector3f ghostPosition = new Vector3f(
					Float.parseFloat(messageTokens[2]),
					Float.parseFloat(messageTokens[3]),
					Float.parseFloat(messageTokens[4]));
				
				ghostManager.updateGhostAvatar(ghostID, ghostPosition);
            }
            if(messageTokens[0].compareTo("createBall") == 0) { 
                Vector3f ballPosition = new Vector3f(
					Float.parseFloat(messageTokens[2]), //X
					Float.parseFloat(messageTokens[3]), //Y
					Float.parseFloat(messageTokens[4]));//Z
                    game.createBall(ballPosition);
            }
            if(messageTokens[0].compareTo("getBall") == 0) { 
                Vector3f ballLoc = game.getBallLoc();
                sendBallLoc(ballLoc);
            }
            if(messageTokens[0].compareTo("crUpdateBall") == 0) { 
                Vector3f ballPosition = new Vector3f(
					Float.parseFloat(messageTokens[2]), //X
					Float.parseFloat(messageTokens[3]), //Y
					Float.parseFloat(messageTokens[4]));//Z
                    game.createBall(ballPosition);
            }
        } 
    }

    public void sendJoinMessage() { 
        try { 
            sendPacket(new String("join," + id.toString()));
        } 
        catch (IOException e) { 
            e.printStackTrace();
        } 
    }

    public void sendByeMessage() {	
        try {	
            sendPacket(new String("bye," + id.toString()));
		} 
        catch (IOException e) {	
            e.printStackTrace();
	    }	
    }
    
    //also need code for:
    public void sendCreateMessage(Vector3f position){	
        try {	
            String message = new String("create," + id.toString());
			message += "," + position.x();
			message += "," + position.y();
			message += "," + position.z();
			
			sendPacket(message);
        } 
        catch (IOException e) {	
            e.printStackTrace();
	    }	
    }
    public void sendDetailsForMessage(UUID remoteId, Vector3f position) {	
        try {	
            String message = new String("dsfr," + remoteId.toString() + "," + id.toString());
			message += "," + position.x();
			message += "," + position.y();
			message += "," + position.z();
			
			sendPacket(message);
		} 
        catch (IOException e) {	
            e.printStackTrace();
	    }	
    }
    public void sendMoveMessage(Vector3f position)
	{	
        try {	
            String message = new String("move," + id.toString());
			message += "," + position.x();
			message += "," + position.y();
			message += "," + position.z();
			
			sendPacket(message);
		} 
        catch (IOException e) {	
            e.printStackTrace();
	    } 
    }
    public void sendBallLoc(Vector3f position) {	
        try {	
            String message = new String("ballLoc," + id.toString());
			message += "," + position.x();
			message += "," + position.y();
			message += "," + position.z();
			
			sendPacket(message);
		} 
        catch (IOException e) {	
            e.printStackTrace();
	    }	
    }
}