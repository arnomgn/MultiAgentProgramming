package models;
import org.json.JSONArray;

import models.*;


public class Grid {
	public Cell[][] grid = new Cell[Constants.DIM_GRID_X][Constants.DIM_GRID_Y];
	public boolean isOver = false;
	
	public Grid() {
		int i,j;
		for(i = 0; i < Constants.DIM_GRID_X ; i ++) {
			for(j = 0; j < Constants.DIM_GRID_Y ; j ++) {
				int number = numberCell(Constants.DIM_GRID_X, Constants.DIM_GRID_Y, i, j);
				Cell cell = new Cell(number, i, j);
				grid[i][j] = cell;
			}
		}
	}
	
	public void endGame() {
		this.isOver = true;
	}

	public boolean isOver() {
		return this.isOver;
	}
	
	public void printDashRow() {
		System.out.print("\n");
		for (int i=0; i< Constants.DIM_GRID_Y; i++) {
		    System.out.print("--- ");
		}
		System.out.print("\n");
	}

	public void display() {
		int i;
		int j;
		for(i = 0; i < Constants.DIM_GRID_X ; i ++) {
			this.printDashRow();
			for(j = 0; j < Constants.DIM_GRID_Y ; j ++) {
				Cell cell = grid[i][j];
				int value = cell.getValue();
				if (value == -1) {
					System.out.print("|" + "█" +  "| ");
				} else if (value == 0) {
					System.out.print("|" + " " + "| ");
				} else if (value==Constants.TRAVELER_VALUE) {
					System.out.print("|" + "x" + "| ");
				} else {
					System.out.print("|" + value + "| ");
				}
			}
			System.out.print("\n");
		}
	}
	
	public void updateCell(Cell newCell) {
		if (newCell != null) {
			int nl = newCell.nligne;
			int nc = newCell.ncolonne;
			grid[nl][nc] = newCell;
		}
	}
	
	public Cell getCell(int nl, int nc) {
		return grid[nl][nc];
	}
	
	// @todo
	public void updateListOfCells(Cell[] cells) {
		int j;
		for(j = 0; j < cells.length; j++) {
			System.out.println(" Old : " + this.grid[cells[j].nligne][cells[j].ncolonne].getValue() + " / new " + cells[j].getValue());
		}
	}
	
	public int numberCell(int line, int col, int i, int j) {
		if ((i % 2 ==0 || i == line-1) && (i <= (line/2 - 2) || i>=(line/2 + 2)) && (j >= ((col/2) - col/10) && j <= (col/2) + col/10)) {
			return -1;
		} else if (((j % 2 == 0 || j == col-1) && (j <= ((col/2) - 2) || j >= (col/2) + 2) ) && (i >= (((line/2)-1) - line/10 ) && i <= ((line/2)-1) + line/10)) {
			return -1;
		} else if ((i >=1 && i <= (1 + line/10)) && ((j >= 1 && j <= 1 +col/10 ) || (j >= (col-2) - col/10 && j <= col-2))) {
			return -1;
		} else if ((i >=(line-2 - line/10) && i <= (line-2)) && ((j >= 1 && j <= 1 +col/10 ) || (j >= (col-2) - col/10 && j <= col-2))) {
			return -1;
		} else {
			return 0;
		}
	}
	
	public boolean getObtacles(int i, int j) {
		boolean position;
		if(grid[i][j].getValue() == -1 ) {
			position = true;
		} else {
			position = false;
		}
		return position;
	}
	
	
}

