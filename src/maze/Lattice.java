package maze;

import java.awt.Point;

class Lattice {
	private boolean Passable;// 小球是否能通过
	private Point Father;//是否遍历过
	private boolean Seen;
	public Lattice() {
		setPassable(false);
		setFather(null);
		setSeen(false);
	}

	public boolean isPassable() {
		return Passable;
	}

	public void setPassable(boolean isPassable) {
		this.Passable = isPassable;
	}

	/**
	 * @return the father
	 */
	public Point getFather() {
		return Father;
	}

	/**
	 * @param father the father to set
	 */
	public void setFather(Point father) {
		this.Father = father;
	}

	public boolean isSeen() {
		return Seen;
	}

	public void setSeen(boolean seen) {
		this.Seen = seen;
	}
}
