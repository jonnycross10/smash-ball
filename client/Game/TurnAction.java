package Game;

import tage.*;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;
import org.joml.*;

public class TurnAction extends AbstractInputAction
{
    private MyGame game;
    private GameObject av;
    private boolean isMounted;

    public TurnAction(MyGame game){
        this.game = game;
    }

    @Override
    public void performAction(float time, Event e)
    {   
        Engine engine = game.getEngine();
        isMounted = game.getMounted();
        float keyValue = e.getValue();
        String inputName = e.getComponent().getName(); //A, D, and X Axis
        if (keyValue > -.2 && keyValue < .2) return; // deadzone (works for A and D)
        if (!game.camCloseToDol()) return;
        float yawValue = keyValue *.3f;
        if(isMounted) {
            switch(inputName){
                case "A":
                    yawValue = -.5f;
                    break;
                case "D":
                    yawValue = .5f;
                    break;
                default:
                    break;
            }
            av = game.getAvatar();
            av.yaw(yawValue);
        }
        else {
            switch(inputName){
                case "A":
                    yawValue = -.5f;
                    break;
                case "D":
                    yawValue = .5f;
                    break;
                default:
                    break;
            }
            Camera cam;
		    cam = (engine.getRenderSystem().getViewport("MAIN").getCamera());
            //rotate the U and N axes around V
            cam.yaw(yawValue);
        }
    } 
}