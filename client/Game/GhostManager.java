package Game;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import org.joml.*;
import tage.*;

public class GhostManager { 
    private MyGame game;
    private List<GhostAvatar> ghostAvs = new ArrayList<GhostAvatar>();
    public GhostManager(VariableFrameRateGame vfrg) { 
        game = (MyGame)vfrg;
    }
    
    public void createGhost(UUID id, Vector3f p) throws IOException { 
        ObjShape s = game.getGhostShape();
        TextureImage t = game.getGhostTexture();
        GhostAvatar newAvatar = new GhostAvatar(id, s, t, p);
        Matrix4f initialScale = (new Matrix4f().scaling(3.0f));
        newAvatar.setLocalScale(initialScale);
        ghostAvs.add(newAvatar);
    }
    public void removeGhostAvatar(UUID id) { 
        GhostAvatar ghostAv = findAvatar(id);
        if(ghostAv != null) { 
            game.getEngine().getSceneGraph().removeGameObject(ghostAv);
            ghostAvs.remove(ghostAv);
        }
        else { 
            System.out.println("unable to find ghost in list");
        } 
    }
    private GhostAvatar findAvatar(UUID id) { 
        GhostAvatar ghostAvatar;
        Iterator<GhostAvatar> it = ghostAvs.iterator();
        while(it.hasNext()) { 
            ghostAvatar = it.next();
            if(ghostAvatar.getId().compareTo(id) == 0) { 
                return ghostAvatar;
            } 
        }
        return null;
    }
    public void updateGhostAvatar(UUID id, Vector3f position) { 
        GhostAvatar ghostAvatar = findAvatar(id);
        if (ghostAvatar != null) { 
            ghostAvatar.setPosition(position); 
        }
        else { 
            System.out.println("unable to find ghost in list"); 
        }
    } 
}