package view;

import java.awt.Color;
import java.awt.Point;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import view.Cell.Type;

public class Controller implements Initializable, Runnable {
	
	@FXML
	public RadioButton up;
	@FXML
	public RadioButton down;
	@FXML
	public RadioButton left;
	@FXML
	public RadioButton right;
	@FXML
	public RadioButton normal;
	@FXML
	public RadioButton highway;
	@FXML
	public RadioButton hard;
	@FXML
	public ToggleGroup direction;
	@FXML
	public ToggleGroup type;
	@FXML
	public Button reset;
	@FXML
	public Button undo;
	@FXML
	public Button addNext;
	@FXML
	public Button gtd;
	@FXML
	public Button tenMoves;
	@FXML
	public Button fifty;
	@FXML
	public Button allMoves;
	@FXML
	public Button viterbi;
	@FXML
	public Button viterbiMax;
	@FXML
	public Button viterbi50;
	@FXML
	public Button viterbiTen;
	@FXML
	public AnchorPane anchorA;
	@FXML
	public AnchorPane anchorB;
	@FXML
	public AnchorPane anchorC;
	@FXML
	public AnchorPane anchorD;

	private final int MAPSIZE = 50;
	private SimGUI grid;
	private Cell gridVals[][];
	private int count, viterbiCount;
	private ArrayList<MoveObs> moveObs = new ArrayList<MoveObs>();
	private ArrayList<Point> bestPoints;
	private LoadedData lD;
	private boolean success, largeGrid, viterbiMode = false;
	private double error[], actProb[];
	public Point selectedCell;
	
	private final static String path = "Trial Grids\\Grid-";
	
	public enum Direction { UP, DOWN, LEFT, RIGHT }
	
	public class MoveObs{
		Direction dir;
		Cell.Type obs;
		public MoveObs(Direction dir, Cell.Type obs){
			this.dir = dir;
			this.obs = obs;
		}
	}
	
	public class BestPath{
		int nonce;
		String s;
		double prob;
		ArrayList<Point> list = new ArrayList<Point>();
		
		public BestPath(String s, double prob, ArrayList<Point> list, int nonce){
			this.s = s;
			this.prob = prob;
			this.list = list;
			this.nonce = nonce;
		}
		
	}
	
	public class LoadedData{
		String moves, obs;
		ArrayList<Point> points;
		public LoadedData(String moves, String obs, ArrayList<Point> points){
			this.moves = moves;
			this.obs = obs;
			this.points = points;
		}
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		
	}
	
	public void generateTruthData(){
		gtd.setVisible(false);
		for(int X = 0; X < 10; X++){
			gridVals = new Cell[MAPSIZE][MAPSIZE];
			String moves = "", obs = "";
			Cell.Type o;
			ArrayList<Point> points;
			
			int totalLeft = MAPSIZE * MAPSIZE;
			int nCells = (int)(.5 * totalLeft);
			int hCells = (int)(.2 * totalLeft);
			int tCells = (int)(.2 * totalLeft);
			
			for(int i = 0; i < gridVals.length; i++){
				for(int j = 0; j < gridVals[0].length; j++){
					int x = (int)(Math.random() * totalLeft);
					totalLeft--;
					if(x < nCells){
						nCells--;
						gridVals[i][j] = new Cell(i,j,0);
					}else if(x < nCells + hCells){
						hCells--;
						gridVals[i][j] = new Cell(i,j, Cell.Type.HIGHWAY,0);
					}else if(x < nCells + hCells + tCells){
						tCells--;
						gridVals[i][j] = new Cell(i,j, Cell.Type.HARD,0);
					}else{
						gridVals[i][j] = new Cell(i,j, Cell.Type.BLOCKED,0);
					}				
				}
			}
			printGrid(X, gridVals);
			
			for(int Y = 0; Y < 10; Y++){
				Point start;
				points = new ArrayList<Point>();
				do{
					start = new Point((int)(Math.random()*MAPSIZE),
							(int)(Math.random()*MAPSIZE));
				}while(!validPos(start.x,start.y));
				points.add(start);
				
				Point tmp = start, next;
				for(int i = 0; i < 100; i++){
					switch((int)(Math.random() * 4)){
					case 0:		moves += 'U';	next = new Point(tmp.x-1, tmp.y);	break;
					case 1:		moves += 'D';	next = new Point(tmp.x+1, tmp.y);	break;
					case 2:		moves += 'L';	next = new Point(tmp.x, tmp.y-1);	break;
					default:	moves += 'R';	next = new Point(tmp.x, tmp.y+1);
					}
					
					if(Math.random() < .9 && validPos(next.x, next.y))
						tmp = next;
					else
						next = tmp;
					points.add(next);
					
					o = gridVals[next.x][next.y].type;
					double x = Math.random();
					if(x < .05)
						switch(o){
						case NORMAL: 	obs += 'H';	break;
						case HIGHWAY: 	obs += 'T';	break;
						default: 		obs += 'N';	break;
						}
					else if(x < .1){
						switch(o){
						case NORMAL: 	obs += 'T';	break;
						case HIGHWAY: 	obs += 'N';	break;
						default: 		obs += 'H';	break;
						}
					}else{
						switch(o){
						case NORMAL: 	obs += 'N';	break;
						case HIGHWAY: 	obs += 'H';	break;
						default: 		obs += 'T';	break;
						}
					}
					
				}
				printGTD(X,Y,moves, obs, points);
			}
		}
		gtd.setVisible(true);
	}
	
	private void printGrid(int x, Cell[][] gV){
		String name = x + "\\Grid.txt";
		FileWriter file;
		
		try {
			file = new FileWriter(path + name, false);
			for(int i = 0; i < MAPSIZE; i++){
				for(int j = 0; j < MAPSIZE; j++){
					switch(gV[i][j].type){
					case NORMAL: 	file.write('N');	break;
					case HARD:		file.write('T');	break;
					case HIGHWAY:	file.write('H');	break;
					default:		file.write('B');
					}
				}
				file.write(System.getProperty("line.separator"));
			}

			file.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void printGTD(int x, int y, String m, String o, ArrayList<Point> p){
		String name = x + "\\" + "GTD-" + y + ".txt";
		FileWriter file;
		
		try {
			file = new FileWriter(path + name, false);
			
			file.write("0: " + printP(p.remove(0)));
			file.write(System.getProperty("line.separator"));
			
			for(int i = 1; i < 101; i++){
				file.write(i + ": " + printP(p.remove(0)) 
				+ "\t" + m.charAt(i-1) +"\t" + o.charAt(i-1));
				file.write(System.getProperty("line.separator"));
			}

			file.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void initializeSmallGrid(){
		gridVals = new Cell[3][3];
		double d = .125;
		
		gridVals[0][0] = new Cell(0,0,Type.HIGHWAY,d);
		gridVals[0][1] = new Cell(0,1,Type.HIGHWAY,d);
		gridVals[0][2] = new Cell(0,2,Type.HARD,d);
		gridVals[1][0] = new Cell(1,0,d);
		gridVals[1][1] = new Cell(1,1,d);
		gridVals[1][2] = new Cell(1,2,d);
		gridVals[2][0] = new Cell(2,0,d);
		gridVals[2][1] = new Cell(2,1,Type.BLOCKED,0);
		gridVals[2][2] = new Cell(2,2,Type.HIGHWAY,d);
		
		count = 1;
	}
	
	public void launchSmallGrid(){
		anchorC.setDisable(true);
		anchorA.setDisable(false);
		initializeSmallGrid();
		
		grid = new SimGUI(3, 3, this);
		grid.addWindowListener(new WindowAdapter(){
			@Override
			public void windowClosing(WindowEvent e) {
				Platform.runLater(new Runnable() {
				    @Override
				    public void run() {
				        guiTerminate();
				    }
				});
		    }
		});
	
		updateCells();
		setBestPosition();
		largeGrid = false;
	}
	
	public void multipleMoves(ActionEvent e){
		Button b = (Button)e.getSource();
		int i;
		
		if(b == tenMoves)
			i = 10;
		else if(b == fifty)
			i = 50;
		else
			i = 100 - (count-1);
	
		for(int j = 0; j < i; j++)
			addNext(e);
		
		updateCells();
		setBestPosition();
	}
	
	public void addNext(ActionEvent e){
		double d[][] = new double[gridVals.length][gridVals[0].length];
		double total = 0;
		Type typeEntered;
		Direction directionEntered;
		
		if(grid == null)
			return;
		
		if(largeGrid){
			switch(lD.moves.charAt(count-1)){
			case 'U':	directionEntered = Direction.UP;	break;
			case 'D':	directionEntered = Direction.DOWN;	break;
			case 'L':	directionEntered = Direction.LEFT;	break;
			default:	directionEntered = Direction.RIGHT;
			}
			
			switch(lD.obs.charAt(count-1)){
			case 'N':	typeEntered = Cell.Type.NORMAL;	break;
			case 'T':	typeEntered = Cell.Type.HARD;	break;
			default:	typeEntered = Cell.Type.HIGHWAY;
			}
		}else{
			if((RadioButton)type.getSelectedToggle() == normal)
				typeEntered = Cell.Type.NORMAL;
			else if((RadioButton)type.getSelectedToggle() == highway)
				typeEntered = Cell.Type.HIGHWAY;
			else
				typeEntered = Cell.Type.HARD;
			
			if((RadioButton)direction.getSelectedToggle() == up)
				directionEntered = Direction.UP;
			else if((RadioButton)direction.getSelectedToggle() == down)
				directionEntered = Direction.DOWN;
			else if((RadioButton)direction.getSelectedToggle() == left)
				directionEntered = Direction.LEFT;
			else
				directionEntered = Direction.RIGHT;
		}
		
		
		for(int i = 0; i < gridVals.length; i++)
			for(int j = 0; j < gridVals[0].length; j++){
				if(gridVals[i][j].type == Cell.Type.BLOCKED)
					continue;
				int i2 = i, j2 = j;
				d[i][j] += .1 * gridVals[i][j].data.get(count-1);
				
				switch(directionEntered){
				case UP:	i2--;	break;
				case DOWN:	i2++;	break;
				case LEFT:	j2--;	break;
				default:	j2++;
				}
				
				if(validPos(i2,j2))
					d[i2][j2] 	+= .9 * gridVals[i][j].data.get(count-1);
				else
					d[i][j] 	+= .9 * gridVals[i][j].data.get(count-1);
			}
		
		for(int i = 0; i < gridVals.length; i++)
			for(int j = 0; j < gridVals[0].length; j++){
				if(gridVals[i][j].type == Cell.Type.BLOCKED)
					continue;
				if(typeEntered == gridVals[i][j].type)
					d[i][j] = d[i][j] * .9;
				else
					d[i][j] = d[i][j] * .05;
				total += d[i][j];
			}
		
		for(int i = 0; i < gridVals.length; i++)
			for(int j = 0; j < gridVals[0].length; j++){
				if(gridVals[i][j].type == Cell.Type.BLOCKED)
					continue;
				double probability = d[i][j]/total;
				gridVals[i][j].data.add(probability);
			}
		
		moveObs.add(new MoveObs(directionEntered, typeEntered));
		count++;
		disableButtons();
		reset.setDisable(false);
		undo.setDisable(false);
		if(((Button)e.getSource()) == addNext)
			updateCells();
		setBestPosition();
	}
	
	private void disableButtons(){
		if(largeGrid){
			if(count >= 101){
				addNext.setDisable(true);
				allMoves.setDisable(true);
				fifty.setDisable(true);
				tenMoves.setDisable(true);
			}else if(count > 91){
				addNext.setDisable(false);
				allMoves.setDisable(false);
				fifty.setDisable(true);
				tenMoves.setDisable(true);
			}else if(count > 51){
				addNext.setDisable(false);
				allMoves.setDisable(false);
				fifty.setDisable(true);
				tenMoves.setDisable(false);
			}else{
				addNext.setDisable(false);
				allMoves.setDisable(false);
				fifty.setDisable(false);
				tenMoves.setDisable(false);
			}
		}
	}
	
	private boolean validPos(int x, int y){
		if(x >= 0 && y >= 0 && x < gridVals.length && y < gridVals[0].length)
			return gridVals[x][y].type != Cell.Type.BLOCKED;
		else
			return false;
	}
	
	public void reset(){
		if(grid == null)
			return;
		
		for(int i = 0; i < gridVals.length; i++)
			for(int j = 0; j < gridVals[0].length; j++){
				double d = gridVals[i][j].data.get(0);
				gridVals[i][j].data = new ArrayList<Double>();
				gridVals[i][j].data.add(d);
			}
		
		moveObs = new ArrayList<MoveObs>();
		count = 1;
		disableButtons();
		reset.setDisable(true);
		undo.setDisable(true);
		updateCells();
		setBestPosition();
	}
	
	public void undo(){
		if(grid == null)
			return;
		
		for(int i = 0; i < gridVals.length; i++)
			for(int j = 0; j < gridVals[0].length; j++)
				if(gridVals[i][j].type != Cell.Type.BLOCKED)
					gridVals[i][j].data.remove(count-1);
		
		count--;
		disableButtons();
		moveObs.remove(count-1);
		if(count == 1){
			reset.setDisable(true);
			undo.setDisable(true);	
		}
		updateCells();
		setBestPosition();
	}
	
	private void setBestPosition(){
		double val = 0.0;
		ArrayList<Point> best = new ArrayList<Point>();
		String s = "Most Likely Cell(s): ";
		boolean multiple = false;
		ArrayList<Cell> orderedPoints = new ArrayList<Cell>();
		bestPoints = new ArrayList<Point>();
		
		for(int i = 0; i < gridVals.length; i++)
			for(int j = 0; j < gridVals[0].length; j++){
				if(gridVals[i][j].type == Cell.Type.BLOCKED)
					continue;
				if(gridVals[i][j].data.get(count-1) > val){
					best = new ArrayList<Point>();
					best.add(gridVals[i][j].self);
					val = gridVals[i][j].data.get(count-1);
				}else if(gridVals[i][j].data.get(count-1) == val)
					best.add(gridVals[i][j].self);
				orderedPoints.add(gridVals[i][j]);
			}
		
		orderedPoints.sort((a,b) -> {
			double d = b.data.get(count-1) - a.data.get(count-1);
			if(d == 0)	return 0;
			if(d < 0)	return -1;
			return 1;	});
		
		Point p = null;
		while(best.size() > 0){
			p = best.remove(0);
			if(multiple)
				s += "; " + printP(p);
			else
				s += printP(p);
			multiple = true;
		}
		
		String line = "Moves: " + (count-1);
		if(count > 1){
			if(largeGrid){
				line += " (" + lD.moves.charAt(count-2) + "," + lD.obs.charAt(count-2) + ")";
				for(int i = 0; i < 10; i++){
					bestPoints.add(orderedPoints.remove(0).self);
				}
			}else
				line += " (" + moveObs.get(count-2).dir + "," + moveObs.get(count-2).obs + ")";
		}
		
		line += "\tProbability: " + val + "   " + s;
		
		if(largeGrid){
			line += "\tActual Pos: " + printP(lD.points.get(count-1));
			if(count > 1)
				line += "\tError: " + Double.toString(getError(p));
		}
		
		grid.setLabel1(line);
		if(count <= 20)
			setBestPath(p);
	}
	
	private double getError(Point p){
		Point actual = lD.points.get(count-1);
		error[count-2] = Point.distance(p.x, p.y, actual.x, actual.y);
		actProb[count-2] = gridVals[actual.x][actual.y].data.get(count-1);
		return error[count-2];
	}
	
	private void setBestPath(Point p){
		int nonce = (int)(Math.random()*999999);
		grid.setLabel2("Best Path to "+ printP(p) +": " + viterbi(p, count, nonce).s);
	}
	
	@SuppressWarnings("unchecked")
	private BestPath viterbi(Point p, int x, int n){
		Point parent;
		double prob;
		BestPath b, bp1, bp2;
		String s, probStr;
		NumberFormat formatter1 = new DecimalFormat("0.###E0");
		NumberFormat formatter2 = new DecimalFormat(".###");
		ArrayList<Point> arr;
	     
		if(!validPos(p.x, p.y))
			return new BestPath("", 0.0, null, 0);
		
		if(gridVals[p.x][p.y].bPs[x-1] != null 
				&& gridVals[p.x][p.y].bPs[x-1].nonce == n)
			return gridVals[p.x][p.y].bPs[x-1];
		
		if(x < 2){
			prob = gridVals[p.x][p.y].data.get(0);
			if(prob < .0001)
				s = formatter1.format(prob);
			else
				s = formatter2.format(prob);
			arr = new ArrayList<Point>();
			arr.add(p);
			
			b = new BestPath("[" + printP(p) + " = "+ s +"]; ",
					prob, arr, n);
			gridVals[p.x][p.y].bPs[0] = b;
			
			return b;
		}
				
		switch(moveObs.get(x-2).dir){
		case UP:	parent = new Point(p.x + 1, p.y);	break;
		case DOWN:	parent = new Point(p.x - 1, p.y);	break;
		case LEFT:	parent = new Point(p.x, p.y + 1);	break;
		default:	parent = new Point(p.x, p.y - 1);
		}
		
		bp1 = viterbi(p,		x - 1, n);
		bp2 = viterbi(parent,	x - 1, n);
		
		if(bp1.prob * .1 > bp2.prob *.9){
			prob = bp1.prob * .1;
			parent =  p;
			s = bp1.s;
			arr = (ArrayList<Point>) bp1.list.clone();
		}else{
			prob = bp2.prob * .9;
			s = bp2.s;
			arr = (ArrayList<Point>) bp2.list.clone();
		}
		
		arr.add(p);
		
		if(moveObs.get(x-2).obs == gridVals[p.x][p.y].type)
			prob *= .9;
		else
			prob *= .05;
		
		if(prob < .0001)
			probStr = formatter1.format(prob);
		else
			probStr = formatter2.format(prob);
		
		b = new BestPath(s + "[" + printP(p) + " = "
				+ probStr +"]; ", prob, arr, n);
		gridVals[p.x][p.y].bPs[x-1] = b;
		return b;
	}
	
	private String printP(Point p){
		return "(" + (p.x+1) + "," + (p.y+1) + ")";
	}
	
	private void updateCells(){
		for(int i = 0; i < gridVals.length; i++)
			for(int j = 0; j < gridVals[0].length; j++){
				grid.setText(i, j, gridVals[i][j].toString());
				if(gridVals[i][j].type == Cell.Type.BLOCKED){
					grid.setCell(i,j, Color.BLACK);
				}else
					grid.setGradientColor(i,j, 
							gridVals[i][j].data.get(count-1));
			}
	}
	
	public void browse(){
		String gridFile;
		FileChooser fileChooser = new FileChooser();
		fileChooser.setInitialDirectory(new File("Trial Grids"));
		fileChooser.setTitle("Open Grid File");
		fileChooser.getExtensionFilters().addAll(
		         new ExtensionFilter("Text Files", "*.txt"));
		Stage stage = new Stage();
		File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
        	gridFile = file.getPath();
        	fileChooser.setTitle("Open Ground Truth Data File");
    		fileChooser.setInitialDirectory(new File(gridFile).getParentFile());
        	file = fileChooser.showOpenDialog(stage);
        	if (file != null){
        		success = true;
	        	count = 1;
        		loadGrid(gridFile);
        		loadGTD(file.getPath());
        		
        		if(success){
	        		anchorC.setDisable(true);
	        		anchorB.setDisable(false);
	        		anchorA.setDisable(false);
	        		viterbi.setDisable(false);
	        		largeGrid = true;
	        		error = new double[100];
	        		actProb = new double[100];
        		}
        	}
        }
	}
	
	private void loadGTD(String filepath){
		String line, tokens[], moves = "", obs = "", delimiters = "[(),\t]";
		File file = new File(filepath);
		ArrayList<Point> points = new ArrayList<Point>();

		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
 
			line = 	reader.readLine();
			tokens = line.split(delimiters);
			points.add(new Point(Integer.parseInt(tokens[1])-1,
					Integer.parseInt(tokens[2])-1));
			
			for(int i = 0; i < 100; i++){
				line = 	reader.readLine();
				tokens = line.split(delimiters);
				points.add(new Point(Integer.parseInt(tokens[1])-1,
						Integer.parseInt(tokens[2])-1));
				
				moves += tokens[4];
				obs += tokens[5];
			}
			reader.close();
			lD = new LoadedData(moves, obs, points);
		} catch (IOException e){
			success = false;
			e.printStackTrace();
		}
	}
	
	private void loadGrid(String filepath){
		File file = new File(filepath);
		double d = 1.0/(MAPSIZE*MAPSIZE*.9);
		String line;
		gridVals = new Cell[MAPSIZE][MAPSIZE];

		try{
			BufferedReader reader = new BufferedReader(new FileReader(file));
			
			for(int i = 0; i < MAPSIZE; i++){
				line = reader.readLine();
				for(int j = 0; j < MAPSIZE; j++){
					switch(line.charAt(j)){
					case 'N':	gridVals[i][j] = new Cell(i,j, d);	break;
					case 'T':	gridVals[i][j] = new Cell(i,j, Cell.Type.HARD, d);		break;
					case 'H':	gridVals[i][j] = new Cell(i,j, Cell.Type.HIGHWAY, d);	break;
					default:	gridVals[i][j] = new Cell(i,j, Cell.Type.BLOCKED, 0);
					}
				}
			}
			reader.close();
			if(success){
				if(grid == null || grid.buttons.length != MAPSIZE){
					grid = new SimGUI(MAPSIZE, MAPSIZE, this);
					grid.addWindowListener(new WindowAdapter(){
						@Override
						public void windowClosing(WindowEvent e) {
							Platform.runLater(new Runnable() {
							    @Override
							    public void run() {
							    	guiTerminate();
							    }
							});
					    }
					});
				}
				updateCells();
				setBestPosition();
			}
		} catch (IOException e){
			success = false;
			e.printStackTrace();
		}
	}
	
	public void activateViterbi(){
		if(anchorD.isDisabled()){
			anchorA.setDisable(true);
			anchorB.setDisable(true);
			anchorD.setDisable(false);
		}else{
			viterbiMode = false;
			anchorA.setDisable(false);
			anchorB.setDisable(false);
			anchorD.setDisable(true);
		}
	}
	
	private void highlightBestTen(){
		Point p;
		for(int i = 0; i < 10; i++){
			p = bestPoints.get(i);
			if(i == 0)
				grid.setCell(p.x, p.y, Color.BLUE);
			else
				grid.setCell(p.x, p.y, Color.CYAN);
		}
	}
	
	public void animateViterbi(){
		new Thread(this).start();
	}
	
	public void viterbiBestTen(ActionEvent e){
		Button b = (Button)e.getSource();
		
		if(count > 2)
			reset();
		
		if(b == viterbiTen){
			multipleMoves(new ActionEvent(tenMoves,null));
			viterbiCount = 10;
		}else if(b == viterbi50){
			multipleMoves(new ActionEvent(fifty,null));
			viterbiCount = 50;
		}else{
			multipleMoves(new ActionEvent(fifty,null));
			multipleMoves(new ActionEvent(fifty,null));
			viterbiCount = 100;
		}
		
		highlightBestTen();
		
		viterbiMode = true;
	}
	
	private double[] pathError(ArrayList<Point> arr){
		double d[] = new double[101];
		Point a, b;
		
		for(int i = 0; i < 101; i++){
			a = arr.get(i);
			b = lD.points.get(i);
			d[i] = Point.distance(a.x, a.y, b.x, b.y);
		}
		return d;
	}
	
	public void computeTotalError(){
		String pathname;
		Cell copy[][];
		double err[][] = new double[100][];
		double act[][] = new double[100][];
		double pathErr[][] = new double[100][];
		int nonce;
		count = 1;
		Point p;
		ArrayList<Point> arr;
		
		for(int i = 0; i < 10; i++){
			success = true;
			pathname = path + i + "\\";
			loadGrid(pathname + "Grid.txt");
			copy = gridVals.clone();
			for(int j = 0; j < 10; j++){
				gridVals = copy.clone();
				loadGTD(pathname + "GTD-" + j + ".txt");
				largeGrid = true;
				error = new double[100];
				actProb = new double[100];
				multipleMoves(new ActionEvent(allMoves,null));
				viterbiMode = true;
				err[(i*10)+j] = error;
				act[(i*10)+j] = actProb;
				
				p = bestPoints.get(0);
				
				nonce = (int)(Math.random() * 999999);
				BestPath b = viterbi(p, 101, nonce);
				arr = b.list;
				
				pathErr[(i*10)+j] = pathError(arr);
				count = 1;
			}
			printErrorAndProbs(i, err, act);
			largeGrid = false;
		}
		
		printAllErrorAndProbs(err, act, pathErr);
	}

	private void printAllErrorAndProbs(double err[][], double act[][], double pathErr[][]){
		String pathname1 ="Trial Grids\\All-Error.txt";
		String pathname2 ="Trial Grids\\All-Probabilities.txt";
		String pathname3 ="Trial Grids\\All-Best-Path-Error.txt";
		FileWriter file1, file2, file3;
		NumberFormat formatter = new DecimalFormat("###.###");
		NumberFormat formatter1 = new DecimalFormat("###.##########");
		
		try {
			file1 = new FileWriter(pathname1, false);
			file2 = new FileWriter(pathname2, false);
			file3 = new FileWriter(pathname3, false);
			for(int i = 0; i < 100; i++){
				file1.write((i+1) + ":\t");
				file2.write((i+1) + ":\t");
				file3.write((i) + ":\t");
				for(int j = 0; j < 100; j++){
					file1.write(formatter.format(err[j][i]) + "\t");
					file2.write(formatter1.format(act[j][i]) + "\t");
					file3.write(formatter.format(pathErr[j][i]) + "\t");
				}
				file1.write(System.getProperty("line.separator"));
				file2.write(System.getProperty("line.separator"));
				file3.write(System.getProperty("line.separator"));
			}
			

			file3.write("100:\t");
			for(int i = 0; i < 100; i++){
				file3.write(formatter.format(pathErr[i][100]) + "\t");
			}
			
			file1.close();
			file2.close();
			file3.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void printErrorAndProbs(int x, double err[][], double act[][]){
		String name1 = x + "\\Error.txt", name2 = x + "\\Actual-Probabilities.txt";
		FileWriter file1, file2;
		NumberFormat formatter = new DecimalFormat("###.###");
		
		try {
			file1 = new FileWriter(path + name1, false);
			file2 = new FileWriter(path + name2, false);
			for(int i = 0; i < 100; i++){
				file1.write((i+1) + ":\t");
				file2.write((i+1) + ":\t");
				for(int j = 0; j < 10; j++){
					file1.write(formatter.format(err[(x*10)+j][i]) + "\t");
					file2.write(formatter.format(act[(x*10)+j][i]) + "\t");
				}
				file1.write(System.getProperty("line.separator"));
				file2.write(System.getProperty("line.separator"));
			}

			file1.close();
			file2.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void guiTerminate(){
		reset();
		anchorA.setDisable(true);
		anchorB.setDisable(true);
		anchorC.setDisable(false);
		anchorD.setDisable(true);
		viterbi.setDisable(true);
	}

	@Override
	public void run() {		
		ArrayList<Point> arr;
		
		if(!viterbiMode || grid.getColor(selectedCell.x, selectedCell.y) != Color.CYAN)
			return;
		
		int nonce = (int)(Math.random() * 999999);
		BestPath b = viterbi(selectedCell, viterbiCount + 1, nonce);
		arr = b.list;
		
		updateCells();
	
		Point prev;
		Double d;
		Point tmp;
		
		for(int i = 0; i < viterbiCount+1; i++){
			tmp = arr.get(i);
			if(i > 0){
				prev = arr.get(i-1);
				d = gridVals[prev.x][prev.y].data.get(viterbiCount);
				grid.setGradientColor(prev.x, prev.y, d);
			}
			
			try {
				Thread.sleep(50);
				grid.setCell(tmp.x, tmp.y, Color.CYAN);
				Thread.sleep(300);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		highlightBestTen();		
	}	
}
