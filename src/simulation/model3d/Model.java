package simulation.model3d;

public abstract class Model {
	public float specificGravity=0;
	public float red=0,green=0,blue=0;
	public float mPrefix=1;

	public void setColor(float red, float green, float blue) {
		this.red = red;
		this.green = green;
		this.blue = blue;
	}

	public void setMaterialDencity(float density) {
		this.specificGravity = density;
	}

}
