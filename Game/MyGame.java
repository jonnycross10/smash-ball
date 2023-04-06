package a2;

import tage.*;
import tage.input.InputManager;
import tage.nodeControllers.BounceController;
import tage.nodeControllers.RotationController;
import tage.shapes.*;

import java.lang.Math;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import org.joml.*;

import a2.FwdAction;
import a2.TurnAction;

import java.util.Random;

public class MyGame extends VariableFrameRateGame
{
	private boolean isMounted;
	private InputManager im;
	private static Engine engine;

	private boolean paused=false;
	private int counter=0;
	private double lastFrameTime, currFrameTime, elapsTime;

	private GameObject dol, cub1, cub2, cub3, x, y, z, planeObj, child1, child2, child3;
	private ObjShape dolS, cubS, linxS, linyS, linzS;
	private TextureImage doltx, prize, grass, prizeAttained;
	private Light light1;

	private int score;
	private final float camDolMinProximity=10f;
	private final float camPrizeProximity=3f;

	private boolean cub1Active;
	private boolean cub2Active;
	private boolean cub3Active;

	private Plane plane;

	private NodeController rc,bc;

	private CameraOrbit3D orbitCam;

	public MyGame() { super(); }

	public static void main(String[] args)
	{	MyGame game = new MyGame();
		engine = new Engine(game);
		game.initializeSystem();
		game.game_loop();
	}

	@Override
	public void loadShapes()
	{	dolS = new ImportedModel("dolphinHighPoly.obj");
		cubS = new Cube();
		linxS = new Line(new Vector3f(0f,0f,0f), new Vector3f(3f,0f,0f));
		linyS = new Line(new Vector3f(0f,0f,0f), new Vector3f(0f,3f,0f));
		linzS = new Line(new Vector3f(0f,0f,0f), new Vector3f(0f,0f,-3f));
		plane = new Plane();
	}

	@Override
	public void loadTextures()
	{	doltx = new TextureImage("Dolphin_HighPolyUV.png");
		prize = new TextureImage("prize.png");
		grass = new TextureImage("grass-pattern.jpg");
		prizeAttained = new TextureImage("silver-background.jpg");
	}

	@Override
	public void buildObjects()
	{	Matrix4f initialTranslation, initialScale;

		// create world axes
		x = new GameObject(GameObject.root(), linxS);
		y = new GameObject(GameObject.root(), linyS);
		z = new GameObject(GameObject.root(), linzS);

		//set world axes' colors
		x.getRenderStates().setColor(new Vector3f(1f, 0f, 0f));
		y.getRenderStates().setColor(new Vector3f(0f, 1f, 0f));
		z.getRenderStates().setColor(new Vector3f(0f, 0f, 1f));

		// build dolphin in the center of the window
		dol = new GameObject(GameObject.root(), dolS, doltx);
		initialTranslation = (new Matrix4f()).translation(0,0,0);
		initialScale = (new Matrix4f()).scaling(3.0f);
		dol.setLocalTranslation(initialTranslation);
		dol.setLocalScale(initialScale);

		// build cubes
		Random rand = new Random();
		int rand1 = rand.nextInt(50)-25;
		int rand2 = rand.nextInt(50)-25;
		int rand3 = rand.nextInt(50)-25;

		cub1 = new GameObject(GameObject.root(), cubS, prize);
		initialTranslation = (new Matrix4f()).translation(rand1,.5f,rand3);
		initialScale = (new Matrix4f()).scaling(0.5f);
		cub1.setLocalTranslation(initialTranslation);
		cub1.setLocalScale(initialScale);

		cub2 = new GameObject(GameObject.root(), cubS, prize);
		initialTranslation = (new Matrix4f()).translation(rand3,.5f,rand2);
		initialScale = (new Matrix4f()).scaling(0.5f);
		cub2.setLocalTranslation(initialTranslation);
		cub2.setLocalScale(initialScale);

		cub3 = new GameObject(GameObject.root(), cubS, prize);
		initialTranslation = (new Matrix4f()).translation(rand2,.5f,rand1);
		initialScale = (new Matrix4f()).scaling(0.5f);
		cub3.setLocalTranslation(initialTranslation);
		cub3.setLocalScale(initialScale);

		planeObj = new GameObject(GameObject.root(), plane, grass);
		initialScale = (new Matrix4f()).scaling(40f);
		planeObj.setLocalScale(initialScale);
	}

	@Override
	public void initializeLights()
	{	Light.setGlobalAmbient(0.5f, 0.5f, 0.5f);
		light1 = new Light();
		light1.setLocation(new Vector3f(5.0f, 4.0f, 2.0f));
		(engine.getSceneGraph()).addLight(light1);
	}

	@Override
	public void initializeGame()
	{	
		isMounted = true;
		score = 0;
		cub1Active = true;
		cub2Active = true;
		cub3Active = true;
		lastFrameTime = System.currentTimeMillis();
		currFrameTime = System.currentTimeMillis();
		elapsTime = 0.0;

		rc = new RotationController(engine, new Vector3f(0,1,0), .001f);
		

		bc = new BounceController(engine, new Vector3f(0,1,0), .001f, 5f);

		(engine.getSceneGraph()).addNodeController(rc);
		(engine.getSceneGraph()).addNodeController(bc);
		(engine.getRenderSystem()).setWindowDimensions(1900,1000);

		isMounted = true;

		//INPUT SECTION
		im = engine.getInputManager();
		FwdAction fwdAction = new FwdAction(this);
		TurnAction turnAction = new TurnAction(this);
		PitchAction pitchAction = new PitchAction(this);
		RideAction ride = new RideAction(this);
		ZoomAction zoom = new ZoomAction(this);
		PanAction pan = new PanAction(this);

		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.A, turnAction,
		InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.D, turnAction,
		InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllGamepads(net.java.games.input.Component.Identifier.Axis.X, turnAction,
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.UP, pitchAction,
		InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.DOWN, pitchAction,
		InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllGamepads(net.java.games.input.Component.Identifier.Axis.RY, pitchAction,
		InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.W, fwdAction,
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		
		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.S, fwdAction,
		InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllGamepads(net.java.games.input.Component.Identifier.Axis.Y, fwdAction,
		InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		
		//im.associateActionWithAllGamepads(net.java.games.input.Component.Identifier.Button._0, ride,
		//InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);

		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.Y, zoom,
		InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.H, zoom,
		InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.I, pan,
		InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.K, pan,
		InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.J, pan,
		InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.L, pan,
		InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);


		// create the orbit view
		String gpName = im.getFirstGamepadName();
		Camera cam = (engine.getRenderSystem().getViewport("MAIN").getCamera());
		orbitCam = new CameraOrbit3D(cam,dol, gpName,engine);
		//placeCamBehindAv();
	}

	@Override
	public void update()
	{		
		lastFrameTime = currFrameTime;
		currFrameTime = System.currentTimeMillis();
		if (!paused) elapsTime += (currFrameTime - lastFrameTime) / 1000.0;
		orbitCam.updateCameraPosition();

		// build and set HUD
		int elapsTimeSec = Math.round((float)elapsTime);
		String scoreDisplayStr = "Score: " + Integer.toString(score);
		
		Vector3f dolWorldPosition = dol.getWorldLocation();
		String locationString = 
			"X: " + String.valueOf(dolWorldPosition.x()) +
			"Y: " + String.valueOf(dolWorldPosition.y()) +
			"Z: " + String.valueOf(dolWorldPosition.z());
			
		Vector3f hud1Color = new Vector3f(0,1,0);
		Vector3f hud2Color = new Vector3f(1,0,0);

		

		int width = (int) Math.ceil((engine.getRenderSystem().getGLCanvas().getWidth()/37.68));
		int height = (int) Math.ceil((engine.getRenderSystem().getGLCanvas().getHeight()/1.325));
		System.out.println(engine.getRenderSystem().getGLCanvas().getWidth());
		System.out.println(engine.getRenderSystem().getGLCanvas().getHeight());
		System.out.println(width);
		System.out.println(height);
		(engine.getHUDmanager()).setHUD1(scoreDisplayStr, hud1Color, 600, 15); //TODO set to screen dimensions
		(engine.getHUDmanager()).setHUD2(locationString, hud2Color, width, height);

		// update input manager
		im.update((float) elapsTime);// can prob take out
		//TODO organize checks in a better way
		updateScore();

		if(isMounted) {
			//mountCam();
			// recenter if dolphin is below world
			reCenterDolphBelowMap();
		}
		
		if(!camCloseToDol()) dismountCam();
		if(!isMounted) {
			
			// recenter if cam is below world
			reCenterCamBelowMap();
		}

		// double score if finished in under a minute and a half
		if(elapsTimeSec <90 && score == 3) score = score*2;

		// check if cubes are active. May want to change to loop/map/arraylist something later
		//TODO get rid of once Node controllers are finished
		//if (!cub1Active) spinGameObject(cub1);
		//if (!cub2Active) spinGameObject(cub2);
		//if (!cub3Active) spinGameObject(cub3);
		
	}

	@Override
	public void createViewports(){
		(engine.getRenderSystem()).addViewport("MAIN",0,0,1,1);
		
		// Create the smaller viewPort
		(engine.getRenderSystem()).addViewport("BR",0f, .75f, .25f,.25f);
		// ------------- positioning the main camera -------------
		(engine.getRenderSystem().getViewport("MAIN").getCamera()).setLocation(new Vector3f(0,0,5));


		Viewport smallVP = engine.getRenderSystem().getViewport("BR");
		
		Camera smallCam = smallVP.getCamera();
		smallCam.setLocation(new Vector3f(0,8,0));
		// Set U V N All have to be orthogonal
		smallCam.setU(new Vector3f(0,0,-1));
		smallCam.setV(new Vector3f(-1,0,0));
		smallCam.setN(new Vector3f(0,-1,0));
	}

	@Override
	public void keyPressed(KeyEvent e)
	{ 	Vector3f loc, fwd, up, right, newLocation;
		switch (e.getKeyCode())
			{ case KeyEvent.VK_1:
				paused = !paused;
				break;
			case KeyEvent.VK_2: // move dolphin forward
				fwd = dol.getWorldForwardVector();
				loc = dol.getWorldLocation();
				newLocation = loc.add(fwd.mul(.02f));
				dol.setLocalLocation(newLocation);
				break;
			case KeyEvent.VK_3: // move dolphin backward
				fwd = dol.getWorldForwardVector();
				loc = dol.getWorldLocation();
				newLocation = loc.add(fwd.mul(-.02f));
				dol.setLocalLocation(newLocation);
				break;
			/* 
			case KeyEvent.VK_SPACE: // view from dolphin
				if(isMounted){
					dismountCam(); //somewhat redundant
				}
				isMounted = !isMounted;
				break;
			*/
			}
		super.keyPressed(e);
	}

	public GameObject getAvatar() { return dol; }

	public boolean getMounted() { return isMounted; }

	public void setMounted(boolean mountStatus) {isMounted = mountStatus;}

	public Engine getEngine() { return engine; }

	public void updateScore(){
		//checks if cam is intersecting with prize
		Camera cam = (engine.getRenderSystem().getViewport("MAIN").getCamera());
		Vector3f camLoc = cam.getLocation();
		Vector3f cub1Loc = cub1.getWorldLocation();
		Vector3f cub2Loc = cub2.getWorldLocation();
		Vector3f cub3Loc = cub3.getWorldLocation();
		//if so, update score and delete prize 
		//TODO could be done with loop through an arraylist if time
		if(withinDistance(camLoc, cub1Loc, camPrizeProximity) && cub1Active){
			score++;
			cub1Active = false;
			//enable Node Controllers
			rc.addTarget(cub1);
			if(!rc.isEnabled()) rc.toggle();
			bc.addTarget(cub1);
			if (!bc.isEnabled()) bc.toggle();
			//take away prize texture
			cub1.setTextureImage(prizeAttained);
			// initialize child prize
			child1 = initChildPrizes();
			// set initial translation
			
			camLoc = cam.getLocation();
			Matrix4f initialTranslation = (new Matrix4f()).translation(score -1f, 1f, 0.2f);
			child1.setLocalTranslation(initialTranslation);
			// set child as child of dolphin
			child1.setParent(dol);
			child1.propagateTranslation(true);
			child1.propagateRotation(true);
		}
		else if (withinDistance(camLoc, cub2Loc, camPrizeProximity) && cub2Active){
			score++;
			cub2Active = false;
			//enable Node Controllers
			rc.addTarget(cub2);
			if(!rc.isEnabled()) rc.toggle();
			bc.addTarget(cub2);
			if (!bc.isEnabled()) bc.toggle();
			//take away prize texture
			cub2.setTextureImage(prizeAttained);
			// initialize child prize
			child2 = initChildPrizes();
			// set initial translation
			camLoc = cam.getLocation();
			Matrix4f initialTranslation = (new Matrix4f()).translation(score -1f,1f,0.2f);
			child2.setLocalTranslation(initialTranslation);
			// set child as child of dolphin
			child2.setParent(dol);
			child2.propagateTranslation(true);
			child2.propagateRotation(true);
		}
		else if (withinDistance(camLoc, cub3Loc, camPrizeProximity) && cub3Active){
			score++;
			cub3Active = false;
			//enable Node Controllers
			rc.addTarget(cub3);
			if(!rc.isEnabled()) rc.toggle();
			bc.addTarget(cub3);
			if (!bc.isEnabled()) bc.toggle();
			//take away prize texture
			cub3.setTextureImage(prizeAttained);
			// initialize child prize
			child3 = initChildPrizes();
			// set initial translation
			camLoc = cam.getLocation();
			Matrix4f initialTranslation = (new Matrix4f()).translation(score -1f,1f,0.2f);
			child3.setLocalTranslation(initialTranslation);
			// set child as child of dolphin
			child3.setParent(dol);
			child3.propagateTranslation(true);
			child3.propagateRotation(true);
		}

	}

	public void mountCam(){
		Vector3f loc, fwd, up, right;
		Camera cam;
		cam = (engine.getRenderSystem().getViewport("MAIN").getCamera());
		loc = dol.getWorldLocation(); //TODO one of these may help with rolling
		fwd = dol.getWorldForwardVector();
		up = dol.getWorldUpVector();
		right = dol.getWorldRightVector();
		cam.setU(right);
		cam.setV(up);
		cam.setN(fwd);
		cam.setLocation(loc.add(up.mul(1.3f)).add(fwd.mul(-2.5f)));
	}

	public void dismountCam() {
		System.out.println("dismounted");
		Vector3f loc, fwd, up, right;
		Camera cam;
		cam = (engine.getRenderSystem().getViewport("MAIN").getCamera());
		loc = dol.getWorldLocation(); //TODO one of these may help with rolling
		fwd = dol.getWorldForwardVector();
		up = dol.getWorldUpVector();
		right = dol.getWorldRightVector();
		cam.setU(right);
		cam.setV(up);
		cam.setN(fwd);
		cam.setLocation(loc.add(right.mul(-1f)).add(up.mul(.3f)));
	}

	public float checkProximity(float x1, float x2){
		return Math.abs(x2-x1); 
	}

	public boolean camCloseToDol(){
		Camera cam = (engine.getRenderSystem().getViewport("MAIN").getCamera());
		Vector3f dolLoc = dol.getWorldLocation();
		Vector3f camLoc = cam.getLocation();
		return withinDistance(camLoc, dolLoc, camDolMinProximity);
	}

	public boolean withinDistance(Vector3f v1, Vector3f v2, float dist){
		float dX = checkProximity(v1.x(), v2.x());
		float dY = checkProximity(v1.y(), v2.y());
		float dZ = checkProximity(v1.z(), v2.z());
		if (dX > dist || dY > dist || dZ>dist){
			return false;
		}
		return true;
	}

	public void reCenterCamBelowMap(){
		Camera cam = (engine.getRenderSystem().getViewport("MAIN").getCamera());
		Vector3f camLoc = cam.getLocation();
		if (camLoc.y() < 0){
			Vector3f newLoc = new Vector3f(camLoc.x(),.5f, camLoc.z());
			cam.setLocation(newLoc);
		}
	}

	public void reCenterDolphBelowMap(){
		Vector3f dolLoc = dol.getLocalLocation();
		if (dolLoc.y() < 0){
			Vector3f newLoc = new Vector3f(dolLoc.x(), 1f, dolLoc.z());
			dol.setLocalLocation(newLoc);
		}
	}

	public void spinGameObject(GameObject obj){
		Matrix4f objRot = obj.getLocalRotation();
		System.out.println("hitting this code");
		Matrix4f rotMatrix = (new Matrix4f()).rotation(.01f,0,1,0);
		objRot = objRot.mul(rotMatrix);
		obj.setLocalRotation(objRot);
	}

	public GameObject initChildPrizes(){
		GameObject obj = new GameObject(GameObject.root(), cubS, prize);
		Matrix4f initialScale = (new Matrix4f()).scaling(0.05f);
		obj.setLocalScale(initialScale);
		return obj;
	}

	public void placeCamBehindAv(){
		//get avatars position -val in the 
		Vector3f dolLoc = dol.getLocalLocation();
		Vector3f newLoc = dolLoc.add(new Vector3f(0,10f,0));
		Camera cam = (engine.getRenderSystem().getViewport("MAIN").getCamera());
		cam.setLocation(newLoc);
		cam.lookAt(dol);
	}

}