import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;

import com.sun.j3d.utils.image.TextureLoader;
import com.sun.j3d.utils.universe.*;
import com.sun.j3d.utils.geometry.*;
import com.sun.j3d.utils.behaviors.vp.*;
import javax.media.j3d.*;
import javax.vecmath.*;


public class SafeUniverse extends JPanel implements ActionListener, KeyListener
// Holding the 3D canvas where the loaded elements are displayed
{

    private static final int PWIDTH = 512;      // size of panel
    private static final int PHEIGHT = 512;
    private static final int BOUNDSIZE = 100;   // larger than the world

    private static final Point3d USERPOS = new Point3d(0,5,20); // initial user position
    private ViewingPlatform vp;
    private TransformGroup steerTG;
    private SimpleUniverse su;
    private BranchGroup sceneBG;
    private BoundingSphere bounds;   // for environment nodes

    private Timer clock1;
    private boolean leftButton = false, rightButton = false;
    private Button startButton = new Button("Run");
    private Button stopButton = new Button("Stop");

    private Button setDefaultViewButton = new Button("Default View");

    private ArrayList<TransformGroup>rotCyl = new ArrayList<TransformGroup>();

    // parameters of cylinders
    int numOfCyl = 15;
    float disBetCyl = 1.0f;
    float vertPos = 3.0f;
    float cylRad = 1.0f;
    float cylH = 0.5f;
    float axRad = 0.1f;
    int posOfFirstCyl = 2;
    float angle = 0;


    public SafeUniverse()
    // A panel holding a 3D canvas
    {
        setLayout(new BorderLayout());
        setOpaque(false);
        setPreferredSize(new Dimension(PWIDTH, PHEIGHT));

        GraphicsConfiguration config = SimpleUniverse.getPreferredConfiguration();
        Canvas3D canvas3D = new Canvas3D(config);
        add("Center", canvas3D);
        canvas3D.addKeyListener(this);

        JPanel panel = new JPanel();
        panel.add(startButton);
        panel.add(stopButton);
        panel.add(setDefaultViewButton);

        add(""+ "North",panel);
        startButton.addActionListener(this);
        startButton.addKeyListener(this);
        setDefaultViewButton.addKeyListener(this);

        clock1 = new Timer(10, this);

        canvas3D.setFocusable(true);     // giving focus to the canvas
        canvas3D.requestFocus();
        su = new SimpleUniverse(canvas3D);

        vp = su.getViewingPlatform();
        steerTG = vp.getViewPlatformTransform();
        createSceneGraph();
        initUserPosition();        // setting user's viewpoint
        orbitControls(canvas3D);   // controlling the movement of the viewpoint

        su.addBranchGraph(sceneBG);
    }


    private void createSceneGraph()
    // Initializing the scene
    {
        sceneBG = new BranchGroup();
        bounds = new BoundingSphere(new Point3d(0,0,0), BOUNDSIZE);

        lightScene();                                   // adding the lights
        addBackground();                                // adding the sky
        sceneBG.addChild(new SafePlatform().getBG());   // adding the floor

        floatingCylinders();    // adding some floating cylinders

        sceneBG.compile();      // fixing the scene
    }


    private void lightScene()
    // Adding one ambient light and 2 directional lights
    {
        Color3f white = new Color3f(1.0f, 1.0f, 1.0f);

        // setting up the ambient light
        AmbientLight ambientLightNode = new AmbientLight(white);
        ambientLightNode.setInfluencingBounds(bounds);
        sceneBG.addChild(ambientLightNode);

        // setting up the directional lights
        Vector3f light1Direction  = new Vector3f(-1.0f, -1.0f, -1.0f);  // left, down, backwards
        Vector3f light2Direction  = new Vector3f(1.0f, -1.0f, 1.0f);    // right, down, forwards

        DirectionalLight light1 = new DirectionalLight(white, light1Direction);
        light1.setInfluencingBounds(bounds);
        sceneBG.addChild(light1);

        DirectionalLight light2 = new DirectionalLight(white, light2Direction);
        light2.setInfluencingBounds(bounds);
        sceneBG.addChild(light2);
    }


    private void addBackground()
    // Adding a blue sky
    {
        Background back = new Background();
        back.setApplicationBounds(bounds);
        back.setColor(0.17f, 0.65f, 0.92f);    // sky colour

        sceneBG.addChild(back);
    }


    private void orbitControls(Canvas3D c)
    // OrbitBehaviour allows the user to rotate around the scene, and to zoom in and out
    {
        OrbitBehavior orbit = new OrbitBehavior(c, OrbitBehavior.REVERSE_ALL);
        orbit.setSchedulingBounds(bounds);

        ViewingPlatform vp = su.getViewingPlatform();
        vp.setViewPlatformBehavior(orbit);
    }


    private void initUserPosition()
    // Setting the user's initial viewpoint using lookAt()
    {
        Transform3D t3d = new Transform3D();
        steerTG.getTransform(t3d);

        // args are: viewer pos, where looking, up direction
        t3d.lookAt(USERPOS, new Point3d(0,0,0), new Vector3d(0,4,0));
        t3d.invert();

        steerTG.setTransform(t3d);
    }

    // ---------------------- Floating cylinders -----------------

    public void floatingCylinders()
    {

        Appearance woodStyle = new Appearance();
        TextureLoader loader = new TextureLoader("img/wood.jpg", null);
        ImageComponent2D image = loader.getImage();

        Texture2D wood = new Texture2D(Texture.BASE_LEVEL, Texture.RGBA, image.getWidth(), image.getHeight());
        wood.setImage(0, image);
        wood.setBoundaryModeS(Texture.WRAP);
        wood.setBoundaryModeT(Texture.WRAP);

        woodStyle.setTexture(wood);

        // creating cylinders
        ArrayList<Cylinder>cylinders = new ArrayList<Cylinder>();

        // setting params of cylinders
        for(int i = 0; i < numOfCyl; i++)
            cylinders.add(new Cylinder(cylRad, cylH, Cylinder.GENERATE_NORMALS| Cylinder.GENERATE_TEXTURE_COORDS, woodStyle));

        // creating transformations of position for cylinders
        ArrayList<Transform3D>posCyl = new ArrayList<Transform3D>();

        //setting this transformation by position algorithm
        int posOfLastCyl = posOfFirstCyl;
        for (int i = 0; i < numOfCyl; i++, posOfLastCyl--){
            Transform3D k = new Transform3D();
            k.set(new Vector3f(0, disBetCyl*posOfLastCyl, -vertPos));
            posCyl.add(k);
        }

        // setting parameters for axis by axis algorithm
        cylinders.add(new Cylinder(axRad, disBetCyl*(posOfFirstCyl-posOfLastCyl), Cylinder.GENERATE_NORMALS| Cylinder.GENERATE_TEXTURE_COORDS, woodStyle));

        // creating and setting position transformation for axis
        Transform3D p = new Transform3D();
        p.set(new Vector3f(0, disBetCyl*(posOfFirstCyl+posOfLastCyl)/2,-vertPos));
        posCyl.add(p);

        // creating transformation groups and matching with transformation
        ArrayList<TransformGroup>tg = new ArrayList<TransformGroup>();
        for (int i = 0; i < numOfCyl+1; i++){
            TransformGroup k = new TransformGroup(posCyl.get(i));
            tg.add(k);

            TransformGroup n = new TransformGroup();
            rotCyl.add(n);
        }

        // rotation transformation
        Transform3D tmp_rot = new Transform3D();
        tmp_rot.rotX(Math.PI/2);
        TransformGroup tg_rot = new TransformGroup(tmp_rot);

        // matching transformation groups with rotation
        for (int i = 0; i < numOfCyl+1; i++){
            rotCyl.get(i).setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);

            rotCyl.get(i).addChild(cylinders.get(i));
            tg.get(i).addChild(rotCyl.get(i));
            tg_rot.addChild(tg.get(i));
        }

        // adding rotation to scene
        sceneBG.addChild(tg_rot);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()){
            case KeyEvent.VK_LEFT:
                leftButton = true;
            case KeyEvent.VK_RIGHT:
                rightButton = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()){
            case KeyEvent.VK_LEFT:
                leftButton = false;
            case KeyEvent.VK_RIGHT:
                rightButton = false;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if(e.getSource() == startButton){
            if(!clock1.isRunning())
                clock1.start();
        }
        else if(e.getSource() == stopButton){
            if(clock1.isRunning())
                clock1.stop();
        }
        else if(leftButton) {
            angle -= Math.PI / 10;
        }
        else if(rightButton) {
            angle += Math.PI / 10;
        }
        else if(e.getSource() == setDefaultViewButton){
            initUserPosition();
        }
        Transform3D rot = new Transform3D();
        rot.rotY(angle);

        for (int i = 0; i < numOfCyl+1; i++)
            rotCyl.get(i).setTransform(rot);

    }

}