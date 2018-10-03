
// 各マスで使用可能な数字を削減、保持するクラス
public class UsableDigitsManager {
	private final int MAX_ROW = SudokuSolver.MAX_ROW;
	private final int MAX_COLMN = SudokuSolver.MAX_COLMN;
	private boolean[][][] usableDigits; // マス(x, y) について使用可能な数値iについて使用可能かを usableDigits[y][x][i] で表す

	public UsableDigitsManager() {}
	
	// 初期化
	public void init(String problem, int[][] table) {
		initUsableDigits(problem, table);
	}
	
	public boolean isUsable(int row, int colmn, int digit) {
		return usableDigits[row][colmn][digit];
	}
	
	// 各マスの使用可能な数字の初期設定
	private void initUsableDigits(String problem, int[][] table) {
		usableDigits = new boolean[MAX_ROW][][];
		for (int i = 0; i < MAX_ROW; ++i) {
			usableDigits[i] = new boolean[MAX_COLMN][];
		}

		for (int i = 0; i < MAX_ROW; ++i) {
			for (int j = 0; j < MAX_COLMN; ++j) {
				usableDigits[i][j] = new boolean[10];
				// マス(i, j) が確定しているなら、それ以外の候補を消しておく
				for (int digit = 0; digit < usableDigits[i][j].length; ++digit) {
					if (table[i][j] != 0) {
						usableDigits[i][j][digit] = false;
					} else
						usableDigits[i][j][digit] = true;
				}
			}
		}
		reduceDigitAtConfirmedCells(table);
	}

	//　使用可能な数字を削減する
	public void reduce(int[][] table) {
		boolean isChange = true;
		// 削除できる候補がなくなるまで繰り返し、候補を削除する
		while(isChange) {
			isChange = setLineUnique(table);
			isChange = isChange || setBlockUnique(table);
			isChange = isChange || setCellUnique(table);
		}
	}
	
	
	// 確定しているマスについて、そのマスの数字を縦、横、ミニマップから削除する
	private void reduceDigitAtConfirmedCells(int[][] table) {
		for (int i = 0; i < MAX_ROW; ++i) {
			for (int j = 0; j < MAX_COLMN; ++j) {
				if(table[i][j] != 0) {
					setDigit(i, j, table[i][j]);
				}
			}
		}
	}
	
	// マス(row, colmn) に数値が設定された場合に、横のラインで使用できる数字を削減する
	private void setDigitRow(int row, int colmn, int digit) {
		for (int i = 0; i < MAX_COLMN; ++i) {
			if(i == colmn)
				continue;
			usableDigits[row][i][digit] = false;
		}
	}

	// マス(row, colmn) に数値が設定された場合に、縦のラインで使用できる数字を削減する
	private void setDigitColmn(int row, int colmn, int digit) {
		for (int i = 0; i < MAX_ROW; ++i) {
			if(i == row)
				continue;
			usableDigits[i][colmn][digit] = false;
		}
	}

	// マス(row, colmn) に数値が設定された場合に、3x3のミニテーブルで使用できる数字を削減する
	private void setDigitMiniTable(int row, int colmn, int digit) {
		int start_row = (row / 3) * 3; // ミニテーブルの最初のセルの座標を求める
		int start_colmn = (colmn / 3) * 3;
		for (int i = start_row; i < start_row + 3; ++i) {
			for (int j = start_colmn; j < start_colmn + 3; ++j) {
				if(i == row && j == colmn)
					continue;
				usableDigits[i][j][digit] = false;
			}
		}
	}

	// 使用可能な数字が1つしかない場合、その値を設定する。設定したときに true を返す
	private boolean setCellUnique(int[][] table) {
		boolean isChange = false;
		for (int row = 0; row < MAX_ROW; ++row) {
			for (int colmn = 0; colmn < MAX_COLMN; ++colmn) {
				if(table[row][colmn] != 0)
					continue;
				int cnt = 0;
				int d = -1;
				// 使用した数字をカウントする
				for (int digit = 1; digit <= 9; ++digit) {
					if (usableDigits[row][colmn][digit]) {
						++cnt;
						d = digit;
					}
				}
				if (cnt == 1) {
					isChange = true;
					setDigit(row, colmn, d);
					table[row][colmn] = d;
				}
			}
		}
		return isChange;
	}

	// 数字を設定する。そのマスのほかの数値は使用不可にする。また、縦横、ミニテーブルの使用不可な数字として設定する。
	private void setDigit(int row, int colmn, int digit) {
		setDigitRow(row, colmn, digit);
		setDigitColmn(row, colmn, digit);
		setDigitMiniTable(row, colmn, digit);
		for (int d = 1; d <= 9; ++d)
			usableDigits[row][colmn][d] = false;
		usableDigits[row][colmn][digit] = true;
	}

	// 数独の表に確定した数字を設定する。
	public void setTable(int[][] table) {
		for (int row = 0; row < MAX_ROW; ++row) {
			for (int colmn = 0; colmn < MAX_COLMN; ++colmn) {
				int cnt = 0;
				int d = -1;
				for (int digit = 1; digit <= 9; ++digit) {
					if (usableDigits[row][colmn][digit]) {
						++cnt;
						d = digit;
					}
				}
				if (cnt == 1  && table[row][colmn] == 0) {
					setDigit(row, colmn, d);
				}
			}
		}
	}
	
	// 横のラインの中である数字iが一つのマスでしか使用できない場合、その数字で確定する
	private boolean setLineUniqueInRow(int[][] table) {
		boolean isChange = false;
		for(int row = 0; row < MAX_ROW; ++row) {
			for(int digit = 1; digit <= 9; ++digit){
				int cnt = 0;
				int set_colmn = -1;
				for(int colmn = 0; colmn < MAX_COLMN; ++colmn) {
					if(usableDigits[row][colmn][digit]) {
						++cnt;
						set_colmn = colmn;
					}
				}
				if(cnt == 1 && table[row][set_colmn] == 0) {
					setDigit(row, set_colmn, digit);
					isChange = true;
					table[row][set_colmn] = digit;
				}
			}
		}
		return isChange;
	}
	
	// 縦のラインの中である数字iが一つのマスでしか使用できない場合、その数字で確定する
	private boolean setLineUniqueInColmn(int[][] table) {
		boolean isChange = false;
		for(int colmn = 0; colmn < MAX_COLMN; ++colmn) {
			for(int digit = 1; digit <= 9; ++digit){
				int cnt = 0;
				int set_row = -1;
				for(int row = 0; row < MAX_ROW; ++row) {
					if(usableDigits[row][colmn][digit]) {
						++cnt;
						set_row = row;
					}
				}
				if(cnt == 1 && table[set_row][colmn] == 0) {
					setDigit(set_row, colmn, digit);
					isChange = true;
					table[set_row][colmn] = digit;
				}
			}
		}
		return isChange;
	}
	
	// 縦もしくは横のラインである数字iを使用可能なマスが一つしかない場合、そのマスは数字iで確定する。
	private boolean setLineUnique(int[][] table) {
		return setLineUniqueInRow(table) || setLineUniqueInColmn(table);
	}
	
	// 3x3のミニテーブルの中で、ある数字iが使用可能なマスが1つしかない場合、そのマスは数字iで確定する。
	private boolean setBlockUnique(int[][] table) {
		boolean isChange = false;
		for(int row = 0; row < MAX_ROW; row += 3)
			for(int colmn = 0; colmn < MAX_COLMN; colmn += 3) {
				if(setBlockUnique(row, colmn, table))
					isChange = true;
			}
		return isChange;
	}
	
	// マス(row, colmn)から始まるミニマップでの削減
	private boolean setBlockUnique(int row, int colmn, int[][] table) {
		boolean isChange = false;
		for(int digit = 1; digit <= 9; ++digit) {
			int cnt = 0;
			int set_row = -1, set_colmn = -1;
			for(int i = row; i < row + 3; ++i) {
				for(int j = colmn; j < colmn + 3; ++j) {
					if(usableDigits[i][j][digit]) {
						++cnt;
						set_row = i;
						set_colmn = j;
					}
				}
			}
			if(cnt == 1 && table[set_row][set_colmn] == 0) {
				isChange = true;
				setDigit(set_row, set_colmn, digit);
				table[set_row][set_colmn] = digit;
				break;
			}
		}
		return isChange;
	}
	

	public void show() {
		for (int i = 0; i < MAX_ROW; ++i) {
			for (int j = 0; j < MAX_COLMN; ++j) {
				System.out.println("(" + i + ", " + j + ")");
				for (int k = 1; k <= 9; ++k) {
					if (usableDigits[i][j][k])
						System.out.print(k);
				}
				System.out.println(" ");
			}
		}
	}

}
