package Game;
import tage.*;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;
import org.joml.*;

public class FwdAction extends AbstractInputAction
{ 
    private MyGame game;
    private GameObject av, terr;
    private Vector3f oldPosition, newPosition;
    private Vector4f fwdDirection;
    private boolean isMounted;

    public FwdAction(MyGame g){ 
        game = g;
    }

    @Override
    public void performAction(float time, Event e)
    { 
        Engine engine = game.getEngine();
        isMounted = game.getMounted();
        float keyValue = e.getValue();
        String inputName = e.getComponent().getName(); // W, S, Y Axis
        if (keyValue > -.2 && keyValue < .2) return; 
        if (!game.camCloseToDol()) return;
        float yawValue = keyValue *-.075f;
        if(isMounted){
            switch(inputName){
                case "W":
                    yawValue = .05f;
                    break;
                case "S":
                    yawValue = -.05f;
                    break;
                default:
                    break;
            }
            av = game.getAvatar();
            oldPosition = av.getWorldLocation();
            fwdDirection = new Vector4f(0f,0f,1f,1f);
            fwdDirection.mul(av.getWorldRotation());
            fwdDirection.mul(yawValue);
            newPosition = oldPosition.add(fwdDirection.x(),
            fwdDirection.y(), fwdDirection.z());
            av.setLocalLocation(newPosition);

        }        
        else{
            // update yaw value for keyboard input
            switch(inputName){
                case "W":
                    yawValue = .01f;
                    break;
                case "S":
                    yawValue = -.01f;
                    break;
                default:
                    break;
            }
            //declare vector variables
            System.out.println("moving cam");
            Camera cam;
            Vector3f fwd;
            //set camera
		    cam = (engine.getRenderSystem().getViewport("MAIN").getCamera());
            //update the forward vector
            cam.setLocation(cam.getLocation().add(cam.getN().mul(yawValue))); //TODO move to engine??
        }
    }
}