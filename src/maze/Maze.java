package maze;

import java.applet.Applet;
import java.applet.AudioClip;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Point;
import java.awt.desktop.SystemSleepEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Random;
import java.util.Stack;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;

class Maze extends JPanel {
	Player player;
	Player keyErrPlayer;
	Player gameOverPlayer;
	private Point entrance = null;
	private Point exit = null;
	private Enemy[] enemys = new Enemy[30];
	private int rowNumber;// ����
	private int colNumber;// ����
	private int numofenemys = 1;
	private int LatticeWidth;// ���ӵĿ��
	private Ball ball;
	private Lattice[][] mazeLattice;
	private boolean startTiming = false;
	private JPanel panel = new JPanel();
	private JTextField timeText = new Timers(), stepNumberText = new JTextField("0");
	private boolean computerDo = false;
	private Thread thread = null;
	private boolean gameOver = false;
	private boolean isBgmPause = false;
	private boolean isfirst = true;
	private int stepNmber;
	private static final char DepthFirstSearchSolveMaze = 0;
	private static final char BreadthFirstSearchSolveMaze = 1;
	private char solveMaze = DepthFirstSearchSolveMaze;
	private static final char DepthFirstSearchCreateMaze = 0;
	private static final char RandomizedPrimCreateMaze = 1;
	private static final char RecursiveDivisionCreateMaze = 2;
	private char createMaze = DepthFirstSearchCreateMaze;
	private boolean promptSolveMaze = false;
	final int[] dirs = {0, 1, 0, -1, 0};
	private ExecutorService enemyService = Executors.newFixedThreadPool(10);
	private ExecutorService audioService = Executors.newFixedThreadPool(10);

	public Maze(int row, int col) {
		this.setRowNumber(row);
		this.setColNumber(col);
		this.LatticeWidth = 15;
		mazeLattice = new Lattice[getRowNumber() + 2][getColNumber() + 2];
		setLayout(new BorderLayout(0, 0));
		getTimeText().setForeground(Color.BLUE);
		getTimeText().setFont(new Font("����", Font.PLAIN, 14));
		getTimeText().setHorizontalAlignment(JTextField.CENTER);
		stepNumberText.setEnabled(false);
		getStepNumberText().setForeground(Color.BLUE);
		getStepNumberText().setFont(new Font("����", Font.PLAIN, 14));
		getStepNumberText().setHorizontalAlignment(JTextField.CENTER);
		Label timeLabel = new Label("Time:"), stepLabel = new Label("StepNumber:");
		timeLabel.setAlignment(Label.RIGHT);
		stepLabel.setAlignment(Label.RIGHT);
		panel.setLayout(new GridLayout(1, 4));
		add(panel, BorderLayout.NORTH);
		panel.add(timeLabel);
		panel.add(getTimeText());
		panel.add(stepLabel);
		panel.add(getStepNumberText());
		panel.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		this.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (!isComputerDo()) {
					requestFocus();
				}
			}
		});
		setKeyListener();
		createMaze();
	}

	public void init() {
		mazeLattice = new Lattice[getRowNumber() + 2][getColNumber() + 2];
		setPromptSolveMaze(false);
		setComputerDo(false);
		setThreadStop();
		resetStepNumber();
		resetTimer();
		if (isfirst) {
			creatEnemyThread();
			creatAudioThread();
			isfirst = false;
		}
		for (int i = 1; i < getRowNumber() + 1; ++i)
			for (int j = 1; j < getColNumber() + 1; ++j) {
				mazeLattice[i][j] = new Lattice();
			}
		for (int i = 0; i < getRowNumber() + 2; ++i) {
			mazeLattice[i][0] = new Lattice();
			mazeLattice[i][getColNumber() + 1] = new Lattice();
		}
		for (int j = 0; j < getColNumber() + 2; ++j) {
			mazeLattice[0][j] = new Lattice();
			mazeLattice[getRowNumber() + 1][j] = new Lattice();
		}
		ball = new Ball(0, 1);
		creatEnemy();
		setEntrance(new Point(0, 1));
		setExit(new Point(getColNumber() + 1, getRowNumber()));
		mazeLattice[getEntrance().y][getEntrance().x].setPassable(true);
		mazeLattice[getExit().y][getExit().x].setPassable(true);
	}

	// �Ƿ��Ѿ��߳��Թ�
	public boolean isWin() {
		if (getExit().x == ball.getX() && getExit().y == ball.getY()) {
			return true;
		}
		return false;
	}

	// ����Ѿ��߳��Թ���������Ϸ�����Ի���
	private void GameOverMessage() throws InterruptedException {
		setGameOver(true);
		setBgmPause(true);
		setPlayerClose();
		((Timers) getTimeText()).stop();
		try {
			// ��ȡ��ǰjava������Ŀ�µ�yy.wav�ļ�
			File file1 = new File("media//win.wav");
			AudioClip sound = Applet.newAudioClip(file1.toURI().toURL());
			sound.play();
		} catch (Exception e) {
			e.printStackTrace();
		}
		JOptionPane.showMessageDialog(null,
				"Congratulations on getting out of the maze!\n" + "Time you have used to go out of the maze is: "
						+ timeText.getText() + "\nThe number of the steps you have used to go out of the maze is: "
						+ stepNmber,
				"GameOver", JOptionPane.INFORMATION_MESSAGE);
	}
	// ����������Ϸ����
	private void MeetEnemy() throws JavaLayerException, InterruptedException {
		setGameOver(true);
		setBgmPause(true);
		setPlayerClose();
		((Timers) getTimeText()).stop();
		File file1 = new File("media//game_over.mp3");
		try {
			BufferedInputStream buffer = new BufferedInputStream(new FileInputStream(file1.toString()));
			gameOverPlayer = new Player(buffer);
			gameOverPlayer.play();
		} catch (FileNotFoundException e) {
			// TODO: handle exception
		}

		JOptionPane.showMessageDialog(null,
				"Sorry, you have dead!\n" + "\nThe number of the steps you have used is: " + stepNmber,
				"GameOver", JOptionPane.INFORMATION_MESSAGE);
	}

	// ��֤���������Ƿ񳬽�
	private boolean isOutofBorder(int x, int y) {
		if ((x == 0 && y == 1) || (x == getColNumber() + 1 && y == getRowNumber()))
			return false;
		else
			return (x > getColNumber() || y > getRowNumber() || x < 1 || y < 1) ? true : false;
	}

	// �����Թ�
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		for (int i = 0; i < getRowNumber() + 2; ++i)
			for (int j = 0; j < getColNumber() + 2; ++j) {
				g.drawRect((j + 1) * LatticeWidth, (i + 1) * LatticeWidth + 30, LatticeWidth, LatticeWidth);
				if (mazeLattice[i][j].isPassable())
					g.setColor(Color.WHITE);
				else
					g.setColor(Color.BLACK);
				g.fillRect((j + 1) * LatticeWidth, (i + 1) * LatticeWidth + 30, LatticeWidth, LatticeWidth);
			}
		g.setColor(Color.RED);
		g.fillRect((getColNumber() + 2) * LatticeWidth, (getRowNumber() + 1) * LatticeWidth + 30, LatticeWidth,
				LatticeWidth);
		g.setColor(ball.getColor());
		g.drawOval((ball.getX() + 1) * LatticeWidth, (ball.getY() + 1) * LatticeWidth + 30, LatticeWidth, LatticeWidth);
		g.fillOval((ball.getX() + 1) * LatticeWidth, (ball.getY() + 1) * LatticeWidth + 30, LatticeWidth, LatticeWidth);
		// set the color of enemy.
		for (int i = 0; i < this.numofenemys; ++i) {
			g.setColor(enemys[i].getColor());
			g.drawOval((enemys[i].getX() + 1) * LatticeWidth, (enemys[i].getY() + 1) * LatticeWidth + 30, LatticeWidth, LatticeWidth);
			g.fillOval((enemys[i].getX() + 1) * LatticeWidth, (enemys[i].getY() + 1) * LatticeWidth + 30, LatticeWidth, LatticeWidth);
		}
		
		if (isPromptSolveMaze()) {
			Stack<Point> pathStack = promptsolveMaze();
			g.setColor(Color.GREEN);
			Point start = pathStack.pop();
			while (!pathStack.isEmpty()) {
				Point end = pathStack.pop();
				Graphics2D g2 = (Graphics2D) g;
				g2.setStroke(new BasicStroke(3.0f));
				g2.drawLine((int) (start.getX() + 1) * LatticeWidth + LatticeWidth / 2,
						(int) (start.getY() + 1) * LatticeWidth + 30 + LatticeWidth / 2,
						(int) (end.getX() + 1) * LatticeWidth + LatticeWidth / 2,
						(int) (end.getY() + 1) * LatticeWidth + 30 + LatticeWidth / 2);
				start = end;
			}
		}
	}

	// ���ü��̼�����
	synchronized private void move(int c) {
		int tx = ball.getX(), ty = ball.getY();
		switch (c) {
		case KeyEvent.VK_LEFT:
			--tx;
			break;
		case KeyEvent.VK_RIGHT:
			++tx;
			break;
		case KeyEvent.VK_UP:
			--ty;
			break;
		case KeyEvent.VK_DOWN:
			++ty;
			break;
		case KeyEvent.VK_ESCAPE:
			System.exit(0);
			break;
		default:
			// ��ֹ����������������Ȼ����������Ч��ʹ�Ʋ�������
			tx = 0;
			ty = 0;
			break;
		}
		if (!isOutofBorder(tx, ty) && mazeLattice[ty][tx].isPassable()) {
			try {
				File file1 = new File("media//keyPressed.wav");
				AudioClip sound = Applet.newAudioClip(file1.toURI().toURL());
				sound.play();
			} catch (Exception e) {
				e.printStackTrace();
			}
			ball.setX(tx);
			ball.setY(ty);
			++stepNmber;
			stepNumberText.setText(Integer.toString(stepNmber));
			if (!isStartTiming()) {
				setStartTiming(!isStartTiming());
				((Timers) getTimeText()).start();
			}
		}
	}

	private void setKeyListener() {
		this.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (!isWin()) {
					int c = e.getKeyCode();
					move(c);
					repaint();
					if (isWin() && !isComputerDo())
						try {
							GameOverMessage();
						} catch (InterruptedException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
				}
			}
		});
	}

	// ����ʱ������
	public void resetTimer() {
		setStartTiming(false);
		getTimeText().setText("00:00:00");
		((Timers) timeText).restart();
	}

	// ���Ʋ�������
	public void resetStepNumber() {
		setStepNmber(0);
		stepNumberText.setText(Integer.toString(stepNmber));
	}

	// ����С���λ��
	public void setBallPosition(Point p) {
		ball.setX(p.x);
		ball.setY(p.y);
		repaint();
	}

	// ����һ���Թ�
	public void createMaze() {
		init();
		AbstractCreateMaze c = null;
		if (getCreateMaze() == DepthFirstSearchCreateMaze)
			c = new DepthFirstSearchCreateMaze();
		else if (getCreateMaze() == RandomizedPrimCreateMaze)
			c = new RandomizedPrimCreateMaze();
		else if (getCreateMaze() == RecursiveDivisionCreateMaze)
			c = new RecursiveDivisionCreateMaze();
		c.createMaze(mazeLattice, getColNumber(), getRowNumber());
		repaint();
	}

	// �ҳ��߳��Թ���·��
	private Stack<Point> solveMaze(Point p) {
		AbstractSolveMaze a = null;
		if (getSolveMaze() == BreadthFirstSearchSolveMaze)
			a = new BreadthFirstSearchSolveMaze();
		else if (getSolveMaze() == DepthFirstSearchSolveMaze)
			a = new DepthFirstSearchSolveMaze();
		return a.solveMaze(mazeLattice, p, getExit(), getColNumber(), getRowNumber());
	}

	// �ҳ�С��λ�ڸ���λ��ʱ�߳��Թ���·��
	private Stack<Point> promptsolveMaze() {
		AbstractSolveMaze a = null;
		if (getSolveMaze() == BreadthFirstSearchSolveMaze)
			a = new BreadthFirstSearchSolveMaze();
		else if (getSolveMaze() == DepthFirstSearchSolveMaze)
			a = new DepthFirstSearchSolveMaze();
		return a.solveMaze(mazeLattice, new Point(ball.getX(), ball.getY()), getExit(), getColNumber(), getRowNumber());
	}

	// �����ڸ���λ�õ�С���߳��Թ���·����ʾ���Թ�����ʾ����ʱ�䳤����ΪcomputerSolveMazeForBallPosition()����
	private void computerSolveMazeForBallPositionForTime(int time) {
		if (getThread() == null)
			setThread(new Thread() {
				@Override
				public void run() {
					while (!isInterrupted())
						try {
							setPromptSolveMaze(true);
							repaint();
							Thread.sleep(time);
							setPromptSolveMaze(false);
							repaint();
							setThreadStop();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							break;
						}
				}
			});
		getThread().start();
	}

	// ��timeʱ����ʾ���ڸ���λ��С���߳��Թ���·��
	public boolean computerSolveMazeForBallPosition() {
		setThreadStop();
		((Timers) getTimeText()).stop();
		int time = 0;
		Object[] selections = { "forever", "10s", "5s", "3s", "1s" };
		Object select = JOptionPane.showInputDialog(null, "Please select the speed of which the ball runs",
				"Jiang's Maze", JOptionPane.INFORMATION_MESSAGE, null, selections, selections[2]);
		if (select != null) {
			switch ((String) select) {
			case "forever":
				time = 2000000000;
				break;
			case "10s":
				time = 10000;
				break;
			case "5s":
				time = 5000;
				break;
			case "3s":
				time = 3000;
				break;
			case "1s":
				time = 1000;
				break;
			default:
				break;
			}
			computerSolveMazeForBallPositionForTime(time);
			((Timers) getTimeText()).proceed();
			return true;
		} else
			return false;
	}

	// ��speed�ٶ��ɼ���������ڳ�ʼλ�õ�С���߳��Թ�����ΪcomputerSolveMaze()��������
	private void computerSolveMazeForSpeed(int speed) {
		setComputerDo(true);
		Point p = null;
		if (isWin())
			p = getEntrance();
		else
			p = new Point(ball.getX(), ball.getY());
		Stack<Point> stack = solveMaze(p);
		resetTimer();
		resetStepNumber();
		if (getThread() == null)
			setThread(new Thread() {
				@Override
				public void run() {
					while (!isInterrupted())
						try {
							while (!stack.isEmpty()) {
								Point p = stack.pop();
								setBallPosition(p);
								++stepNmber;
								stepNumberText.setText(Integer.toString(stepNmber));
								Thread.sleep(speed);
							}
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							break;
						}
				}
			});
		getThread().start();
	}

	// �ɼ���������ڳ�ʼλ�õ�С���߳��Թ�
	public boolean computerSolveMaze() {
		int speed = 0;
		setThreadStop();
		Object[] selections = { "lower seed", "low speed", "medium speed", "high speed", "higher speed" };
		Object select = JOptionPane.showInputDialog(null, "Please select the speed of which the ball runs",
				"Jiang's Maze", JOptionPane.INFORMATION_MESSAGE, null, selections, selections[2]);
		if (select != null) {
			switch ((String) select) {
			case "lower seed":
				speed = 400;
				break;
			case "low speed":
				speed = 300;
				break;
			case "medium speed":
				speed = 200;
				break;
			case "high speed":
				speed = 100;
				break;
			case "higher speed":
				speed = 20;
				break;
			default:
				break;
			}
			computerSolveMazeForSpeed(speed);
			return true;
		} else
			return false;
	}
	
	// ������˺�Ŀ���ǲ�������Ѱ��Χ�ڣ��������ȥ����Ѱ��Ŀ��
	public boolean isInRange(Ball ball, Enemy enemy) {
		int len = colNumber / 2;
		int subX = ball.getX() - enemy.getX();
		int subY = ball.getY() - enemy.getY();
		int dis2 = Math.abs(subX * subX + subY * subY);
		if (dis2 < len*len) {
			return true;
		}
		return false;
	}
	
	public Stack<Point> seekTarget(Ball ball, Enemy enemy) {
		Stack<Point> s = new Stack<Point>();
		Point tempballPoint = new Point(ball.getX(), ball.getY());
		if (isInRange(ball, enemy)) {
			s = findPath(mazeLattice, enemy, tempballPoint, getColNumber(), getRowNumber());
		}
		return s;
	}
	
	
	// ��������
	public void creatEnemy() {
		Random rand = new Random();
		int x, y;
		for (int i = 0; i < this.numofenemys; ++i) {
			x = 2 * rand.nextInt(colNumber / 2) + 1;
			y = 2 * rand.nextInt(rowNumber / 2) + 1;
			while (true) {
				if (!isInRange(ball, new Enemy(x, y))) {
					break;
				} else {
					x = 2 * rand.nextInt(colNumber / 2) + 1;
					y = 2 * rand.nextInt(rowNumber / 2) + 1;
				}
			}
			enemys[i] = new Enemy(x, y);
			enemys[i].setMazeLattice(mazeLattice);
		}
	}
	
	// ���������ƶ����߳�
	public void creatEnemyThread() {
		enemyService.execute(new EnemyThread());
	}
	
	public void MoveEnemyPoint(Lattice[][] mazeLattice, Enemy enemys[]) throws JavaLayerException, InterruptedException {
		int curX = 0, curY = 0;
		Random rand = new Random();
		for (int i = 0; i < this.numofenemys; ++i) {
			if (!enemys[i].getPathPoints().empty()) {
				Point temp = enemys[i].getPathPoints().pop();
				curX = temp.x;
				curY = temp.y;
				enemys[i].setX(curX);
				enemys[i].setY(curY);
			} else {
				int j = rand.nextInt(4);
				curX = enemys[i].getX() + dirs[j];
				curY = enemys[i].getY() + dirs[j+1];
				if (!isOutofBorder(curY, curX) && mazeLattice[curY][curX].isPassable()) {
					enemys[i].setX(curX);
					enemys[i].setY(curY);
					enemys[i].setPathPoints(seekTarget(ball, enemys[i]));
				}
			}
			if (ball.getX() == enemys[i].getX() && ball.getY() == enemys[i].getY()) MeetEnemy();
		}
	}
	
	public Stack<Point> findPath(Lattice[][] mazeLattice, Enemy enemy, Point ball, int colNumber, int rowNumber) {
		// TODO Auto-generated method stub
		int curX = 0, curY = 0;
		boolean found = false;
		Point tempenemyPoint = new Point(enemy.getX(), enemy.getY());
		for (int i = 0; i < rowNumber + 2; ++i)
			for (int j = 0; j < colNumber + 2; ++j)
				enemy.mazeLattice[i][j].setSeen(false);
		Stack<Point> pathStack = new Stack<Point>();
		pathStack.push(ball);
		enemy.mazeLattice[ball.x][ball.y].setSeen(true);
		while (!pathStack.empty()) {
			found = false;
			Point temPoint = pathStack.peek();
			for (int i = 0; i < 4; ++i) {
				curX = temPoint.x + dirs[i];
				curY = temPoint.y + dirs[i+1];
				if (!isOutofBorder(curY, curX) && !enemy.mazeLattice[curY][curX].isSeen() && mazeLattice[curY][curX].isPassable()) {
					found = true;
					enemy.mazeLattice[curY][curX].setSeen(true);
					pathStack.push(new Point(curX, curY));
					break;
				}
			}
			if (curX == tempenemyPoint.x && curY == tempenemyPoint.y) break;
			if (!found) pathStack.pop();
		}
		return pathStack;
	}
	
	public BufferedInputStream getBufferedInputStream(File file) throws FileNotFoundException {
		BufferedInputStream buffer = new BufferedInputStream(new FileInputStream(file.toString()));
		return buffer;
	}

	public int getLatticeWidth() {
		return LatticeWidth;
	}

	public void setLatticeWidth(int latticeWidth) {
		LatticeWidth = latticeWidth;
	}

	public JTextField getTimeText() {
		return timeText;
	}

	/**
	 * @return the startTiming
	 */
	public boolean isStartTiming() {
		return startTiming;
	}

	/**
	 * @param startTiming the startTiming to set
	 */
	public void setStartTiming(boolean startTiming) {
		this.startTiming = startTiming;
	}

	/**
	 * @return the entrance
	 */
	public Point getEntrance() {
		return entrance;
	}

	/**
	 * @param entrance the entrance to set
	 */
	public void setEntrance(Point entrance) {
		this.entrance = entrance;
	}

	/**
	 * @return the exit
	 */
	public Point getExit() {
		return exit;
	}

	/**
	 * @param exit the exit to set
	 */
	private void setExit(Point exit) {
		this.exit = exit;
	}

	/**
	 * @return the computerDo
	 */
	public boolean isComputerDo() {
		return computerDo;
	}

	/**
	 * @param computerDo the computerDo to set
	 */
	public void setComputerDo(boolean computerDo) {
		this.computerDo = computerDo;
	}

	/**
	 * @return the thread
	 */
	public Thread getThread() {
		return thread;
	}

	/**
	 * @param thread the thread to set
	 */
	private void setThread(Thread thread) {
		this.thread = thread;
	}

	public void setThreadStop() {
		if (getThread() != null) {
			if (isPromptSolveMaze())
				setPromptSolveMaze(false);
			thread.interrupt();
			setThread(null);
		}
	}

	/**
	 * @return the stepNumberText
	 */
	public JTextField getStepNumberText() {
		return stepNumberText;
	}

	/**
	 * @return the stepNmber
	 */
	public int getStepNmber() {
		return stepNmber;
	}

	/**
	 * @param stepNmber the stepNmber to set
	 */
	public void setStepNmber(int stepNmber) {
		this.stepNmber = stepNmber;
	}

	/**
	 * @return the rowNumber
	 */
	public int getRowNumber() {
		return rowNumber;
	}

	/**
	 * @param rowNumber the rowNumber to set
	 */
	public void setRowNumber(int rowNumber) {
		this.rowNumber = rowNumber;
	}

	/**
	 * @return the colNumber
	 */
	public int getColNumber() {
		return colNumber;
	}

	/**
	 * @param colNumber the colNumber to set
	 */
	public void setColNumber(int colNumber) {
		this.colNumber = colNumber;
	}

	/**
	 * @return the solveMaze
	 */
	public char getSolveMaze() {
		return solveMaze;
	}

	/**
	 * @param solveMaze the solveMaze to set
	 */
	public void setSolveMaze(char solveMaze) {
		this.solveMaze = solveMaze;
	}

	/**
	 * @return the createMaze
	 */
	public char getCreateMaze() {
		return createMaze;
	}

	/**
	 * @param createMaze the createMaze to set
	 */
	public void setCreateMaze(char createMaze) {
		this.createMaze = createMaze;
	}

	/**
	 * @return the promptSolveMaze
	 */
	public boolean isPromptSolveMaze() {
		return promptSolveMaze;
	}

	/**
	 * @param promptSolveMaze the promptSolveMaze to set
	 */
	public void setPromptSolveMaze(boolean promptSolveMaze) {
		this.promptSolveMaze = promptSolveMaze;
	}

	public void creatAudioThread() {
		audioService.execute(new AudioThread());
	}

	public void setAudioThreadStop() {
		isBgmPause = true;
	}

	public int getNumofenemys() {
		return numofenemys;
	}

	public void setNumofenemys(int numofenemys) {
		this.numofenemys = numofenemys;
	}
	
	class EnemyThread implements Runnable {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			while (!gameOver) {
				try {
					Thread.sleep(500);
					MoveEnemyPoint(mazeLattice, enemys);
					repaint();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (JavaLayerException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}	
			}
		}
	}
	
	class AudioThread implements Runnable {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			while (!isBgmPause) {
				try {
					getPlayer().play();
				} catch (Exception e) {
					break;
				}
			}
		}
		
	}

	public ExecutorService getEnemyService() {
		return enemyService;
	}

	public void setEnemyService() {
		enemyService = Executors.newFixedThreadPool(10);
	}

	public ExecutorService getAudioService() {
		return audioService;
	}

	public void setAudioService() {
		audioService = Executors.newFixedThreadPool(10);
	}
	
	public void createAudioThread(ExecutorService audioService) {
		this.audioService = audioService;
	}

	public Player getPlayer() throws JavaLayerException, FileNotFoundException {
		
		File file = new File("media//bgm.mp3");
		BufferedInputStream buffer = new BufferedInputStream(new FileInputStream(file.toString()));
		player = new Player(buffer);
		return player;
	}

	public void setPlayerClose() {
		this.player.close();
	}

	public boolean isGameOver() {
		return gameOver;
	}

	public void setGameOver(boolean gameOver) {
		this.gameOver = gameOver;
	}

	public boolean isBgmPause() {
		return isBgmPause;
	}

	public void setBgmPause(boolean isBgmPause) {
		this.isBgmPause = isBgmPause;
	}

	public boolean getIsfirst() {
		return isfirst;
	}

	public void setIsfirst(boolean isfirst) {
		this.isfirst = isfirst;
	}

}