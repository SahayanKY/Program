package openGL;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;

import simulation.model3d.ModelHandler;

public class GLViewFrame extends JFrame{
	int size = 500;

	public static void main(String args[]) {
		new GLViewFrame("Hello, OpenGL!!");
	}

	public GLViewFrame(String title) {
		super(title);
		GLAnimator animator = new GLAnimator();

		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setLocation(300,300);

		JPanel parentPanel = new JPanel();

		JPanel canvasPanel = new JPanel();
		JLabel lb = new JLabel("setting.....");
		lb.setHorizontalAlignment(JLabel.CENTER);
		lb.setPreferredSize(new Dimension(size,size));
		canvasPanel.add(lb);
		parentPanel.add(canvasPanel);


		JPanel actionPanel = new JPanel();
		JButton bt = new JButton("button");
		bt.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				animator.changeStateAnimation();
			}

		});
		actionPanel.add(bt);
		parentPanel.add(actionPanel);

		add(parentPanel);
		pack();
		setVisible(true);


		//処理が重いので一旦別のスレッドに分岐させてフレームを表示させる
		SwingWorker<Object,Object> sw = new SwingWorker<Object,Object>(){

			@Override
			protected Object doInBackground() throws Exception {
				GLCapabilities caps = new GLCapabilities(GLProfile.get(GLProfile.GL2));

				GLCanvas canvas = new GLCanvas(caps);
				canvas.setPreferredSize(new Dimension(size,size));
				canvas.addGLEventListener(animator);
				//canvas.addMouseListener(this);

				animator.setModelList(ModelHandler.loadModelFile("D:\\アセンブリ.AMF", "amf"));
				animator.setAnimationConfigure(canvas,60,true);
				animator.startAnimation();


				canvasPanel.remove(lb);
				canvasPanel.add(canvas);
				GLViewFrame.this.setVisible(true);

				return null;
			}

		};

		sw.execute();
	}
}
