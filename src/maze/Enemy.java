package maze;

import java.awt.Color;
import java.awt.Point;
import java.util.Stack;

class Enemy {

	private int x;
	private int y;
	private Color color;
	Stack<Point> pathPoints;
	Lattice[][] mazeLattice = new Lattice[100][100];
	public Enemy(int x, int y) {
		setX(x);
		setY(y);
		setColor(Color.BLUE);
		this.pathPoints = new Stack<Point>();
	}
	public int getX() {
		return x;
	}
	public void setX(int x) {
		this.x = x;
	}
	public int getY() {
		return y;
	}
	public void setY(int y) {
		this.y = y;
	}
	public Color getColor() {
		return color;
	}
	public void setColor(Color color) {
		this.color = color;
	}
	public Stack<Point> getPathPoints() {
		return pathPoints;
	}
	public void setPathPoints(Stack<Point> pathPoints) {
		this.pathPoints = pathPoints;
	}
	public Lattice[][] getMazeLattice() {
		return mazeLattice;
	}
	public void setMazeLattice(Lattice[][] mazeLattice) {
		this.mazeLattice = mazeLattice;
	}
}
