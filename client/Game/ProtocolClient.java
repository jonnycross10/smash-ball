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
        String[] messageTokens = strMessage.split(",");
        if(messageTokens.length > 0) {
            if(messageTokens[0].compareTo("join") == 0) { // format: join, success or join, failure
                if(messageTokens[1].compareTo("success") == 0) { 
                    game.setIsConnected(true);
                    sendCreateMessage(game.getPlayerPosition());
                }
                if(messageTokens[1].compareTo("failure") == 0) { 
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

            }
            if(messageTokens[0].compareTo("move") == 0) { 

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
    public void sendCreateMessage(Vector3f pos) { // format: (create, localId, x,y,z)
        try { 
            String message = new String("create," + id.toString());
            message += "," + pos.x()+"," + pos.y() + "," + pos.z();
            sendPacket(message);
        }
        catch (IOException e) { 
            e.printStackTrace();
        } 
    
    }
    //also need code for:
    public void sendByeMessage() {}
    public void sendDetailsForMessage(UUID remId, Vector3f pos) {}
    public void sendMoveMessage(Vector3f pos) {}
}