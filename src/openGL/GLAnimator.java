package openGL;

import static com.jogamp.opengl.GL.*;
import static com.jogamp.opengl.fixedfunc.GLLightingFunc.*;
import static com.jogamp.opengl.fixedfunc.GLMatrixFunc.*;

import java.util.ArrayList;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.FPSAnimator;

import simulation.model3d.Model;


public class GLAnimator implements GLEventListener {
	private final GLU glu = new GLU();
	private float r=0;
	private FPSAnimator animator;
	private ArrayList<Model> modelList = null;

	public void setAnimationConfigure(GLAutoDrawable drawable, int fps, boolean scheduleAtFixedRate) {
		animator = new FPSAnimator(drawable, fps, scheduleAtFixedRate);
	}

	public void startAnimation() {
		if(animator == null) {
			return;
		}else {
			animator.start();
		}
	}

	public void changeStateAnimation() {
		if(animator == null) {
			return;
		}else {
			if(animator.isPaused()) {
				animator.resume();
			}else {
				animator.pause();
			}
		}
	}

	public void setModelList(ArrayList<Model> modelList){
		this.modelList = modelList;
	}

	@Override
	public void init(GLAutoDrawable drawable) {
		GL2 gl2 = drawable.getGL().getGL2();
		gl2.glClearColor(0.8f, 0.8f, 0.8f, 1);
		gl2.glEnable(GL_DEPTH_TEST);
		gl2.glEnable(GL_CULL_FACE);
		gl2.glCullFace(GL_BACK);

		//ライトの設定
		gl2.glEnable(GL_LIGHTING);
		gl2.glEnable(GL_LIGHT0);
		//色の設定
		gl2.glLightfv(GL_LIGHT0, GL_DIFFUSE, new float[] {0.9f, 0.9f, 0.9f, 1},0);
		gl2.glLightfv(GL_LIGHT0, GL_SPECULAR, new float[] {0.9f, 0.9f, 0.9f, 1},0);
		gl2.glLightfv(GL_LIGHT0, GL_AMBIENT, new float[] {0.9f, 0.9f, 0.9f, 1},0);
		//位置の設定
		gl2.glLightfv(GL_LIGHT0, GL_POSITION, new float[] {0,300,300,0}, 0);

	}

	@Override
	public void display(GLAutoDrawable drawable) {
		GL2 gl2 = drawable.getGL().getGL2();
		gl2.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

		gl2.glLoadIdentity();

		//視点の位置、向き
		glu.gluLookAt(120, 120, 120, 0, 0, 0, 0, 1, 0);
		gl2.glRotatef(-0.5f*r,0,1,0);


		gl2.glDisable(GL_LIGHTING);
		gl2.glBegin(GL_LINES);
			gl2.glColor3f(1.0f, 0, 0);
			gl2.glVertex3fv(new float[]{-5,0,0},0);
			gl2.glVertex3fv(new float[] {25,0,0},0);
		gl2.glEnd();

		gl2.glBegin(GL_LINES);
			gl2.glColor3f(0, 1.0f, 0);
			gl2.glVertex3fv(new float[]{0,-5,0},0);
			gl2.glVertex3fv(new float[] {0,25,0},0);
		gl2.glEnd();

		gl2.glBegin(GL_LINES);
			gl2.glColor3f(0, 0, 1.0f);
			gl2.glVertex3fv(new float[]{0,0,-5},0);
			gl2.glVertex3fv(new float[] {0,0,25},0);
		gl2.glEnd();

		gl2.glBegin(GL_LINES);
			gl2.glColor3f(1.0f, 1.0f, 1.0f);
			gl2.glVertex3fv(new float[]{-10,-10,-3},0);
			gl2.glVertex3fv(new float[] {10,10,3},0);
		gl2.glEnd();
		gl2.glEnable(GL_LIGHTING);


		if(modelList == null) {
			return;
		}else {

			// 図形の回転
			//gl2.glRotatef(r, 1.0f, 1.0f, 0.3f);


			for(Model model:modelList) {
				painter.paint(gl2,model);
			}

			if(r++ >= 720.0f) {
				r = 0;
			}
		}

		gl2.glDisable(GL_LIGHTING);
		gl2.glBegin(GL_LINES);
			gl2.glColor3f(1.0f,1.0f,0.0f);
			gl2.glVertex3fv(new float[] {0,0,20},0);
			gl2.glVertex3fv(new float[] {20,20,-10},0);
		gl2.glEnd();
		gl2.glEnable(GL_LIGHTING);

	}

	ModelPainter painter = new AMFPainter();

	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
		GL2 gl2 = drawable.getGL().getGL2();

		//gl2.glViewport(x, y, width, height);
		//JOGL内部で実行済み

		gl2.glMatrixMode(GL_PROJECTION);
		//透視変換行列を指定

		gl2.glLoadIdentity();
		//透視変換行列を単位行列にする

		//gl2.glOrthof(-1.0f*width/300, 1.0f*width/300, -1.0f*height/300, 1.0f*height/300, -1.0f, 1.0f);

		glu.gluPerspective(30.0, (double)width / (double)height, 1.0, 300.0);

		gl2.glMatrixMode(GL_MODELVIEW);
	}

	@Override
	public void dispose(GLAutoDrawable drawable) {
		if(animator != null) {
			animator.stop();
		}
	}


}
