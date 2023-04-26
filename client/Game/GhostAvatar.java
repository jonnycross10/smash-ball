package Game;

import java.util.UUID;
import org.joml.*;
import tage.*;

public class GhostAvatar extends GameObject { 
    private UUID id;
    public GhostAvatar(UUID id, ObjShape s, TextureImage t, Vector3f p) { 
        super(GameObject.root(), s, t);
        this.id = id;
        setPosition(p);
        System.out.println("hit this");
    }

    //TODO also need accessors and setters for id and position
    public UUID getId() {
        return this.id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Vector3f getPosition() {
        return getWorldLocation();
    }

    public void setPosition(Vector3f position) {
        setLocalLocation(position);
    }
}
