import BddTools.BddNodeStore;
import java.lang.Exception;

public class SudokuSolver {
	public static void main(String[] args) {
		SudokuSolver solver = new SudokuSolver(ProblemFactory.min_hint_problem);
		int bdd = solver.getBddOfTheConstraint();
		// System.out.println(solver.toString(bdd));
		solver.showAns(bdd);
		System.out.println("node num " + solver.getSize());
		System.out.println("hash size " + solver.getHashSize());
		System.out.println("cache size " + solver.getCacheSize());
	}

	public int getSize() {
		return store.getNodeSize();
	}

	public int getHashSize() {
		return store.getHashSize();
	}

	public int getCacheSize() {
		return store.getCacheSize();
	}

	public String toString(int node) {
		return store.getBddExpressString(node);
	}

	// 
	public void showAns(int node) {
		store.enumerate(node);
		int cnt = 0;
		while(store.getPath(cnt) != null) {
			System.out.println("------------------");
			System.out.println("Answer No." + (cnt + 1));
			String str = store.getPath(cnt);
			for(int i = 0; i < str.length(); i += 18)
				System.out.println(str.substring(i, i + 18));
			System.out.println("------------------");
			++cnt;
		}
	}

	public void show_table() {
		for (int i = 0; i < MAX_ROW; ++i) {
			for (int j = 0; j < MAX_COLMN; ++j) {
				System.out.print(table[i][j]);
			}
			System.out.println("");
		}
	}

	public final static int MAX_ROW = 9, MAX_COLMN = 9;
	public final static int MAX_ROW_WITH_MIN_TABLE = 3;
	public final static int MAX_COLMN_WITH_MIN_TABLE = 3;
	public final static int MAX_DIGIT = 9;
	private int[][] table;
	private UsableDigitsManager usableDigitsManager = new UsableDigitsManager();
	BddNodeStore store = new BddNodeStore();

	// 数独表の初期化
	private void initTable(String problem) {
		table = new int[MAX_ROW][];
		for (int i = 0; i < MAX_ROW; ++i) {
			table[i] = new int[MAX_COLMN];
			for (int j = 0; j < MAX_COLMN; ++j) {
				table[i][j] = Character.getNumericValue(problem.charAt(j + i * MAX_COLMN));
			}
		}
	}

	public SudokuSolver() {
	}

	public SudokuSolver(String problem) {
		initTable(problem);
		usableDigitsManager.init(problem, table);
		// 各マスの使用可能な数字を削減する
		usableDigitsManager.reduce(table);
		usableDigitsManager.setTable(table);
		// usableDigitsManager.show();
	}

	private int ans_bdd = store.getZeroNode();

	// 各種制約を作り統合した BDD を返す。これが数独の答えを表す BDD となる
	// BDD に制約を統合する順序によって、統合時間がかかるため以下のように少しづつ制約を作成、統合している
	public int getBddOfTheConstraint() {
		int root_node = store.getOneNode();
		long cons_time = System.currentTimeMillis();

		for (int row = 0; row < MAX_ROW; ++row) {
			for (int colmn = 0; colmn < MAX_COLMN; ++colmn) {
				// マス(row, colmn)の使用可能な数字について制約にする
				int one_cell = oneCellConstraint(row, colmn);
				root_node = store.and(root_node, one_cell);

				// その制約を作りたいミニテーブルについてすべてのマスがすでに制約として作られている場合に、その3x3のミニテーブルについての制約を作る
				// そうでないときtは－１を返す
				int t = getMinTableConstraint(row, colmn, root_node);
				if (t != -1) {
					root_node = t;
				}
				if (row == 5) {
					// 0～２行目と3～5行目のマスの値が異なるという制約を作り、統合する
					root_node = differentValueInColmn(colmn, 0, 3, root_node);
				} else if (row == 8) {
					// 3~5行目と6～8行目のマスの値が異なるという制約を作り、統合する
					root_node = differentValueInColmn(colmn, 3, 6, root_node);
				}
			}
			
			// 現在の行について値が異なるという制約を作り、統合する
			root_node = differentValueInRowConstraint(row, root_node);
		}

		for (int colmn = 0; colmn < MAX_COLMN; ++colmn) {
			// 0~3行と6～8行のマスが異なるという制約を作り、統合する
			root_node = differentValueInColmn(colmn, 0, 6, root_node);
		}

		System.out.println("create constrain. time is " + (System.currentTimeMillis() - cons_time) + "ms");
		return root_node;
	}

	// 列でマスの値がそれぞれ異なるという制約を作り、統合し、返す
	// ここでは、列を3行ごとに区切って、区切った部分同士で異なるとう制約にしている。つまり,
	// minTableRowUp行目からminTableRowUp + 2行目までと minTableRowDown行目からminTableRowDown +
	// 2 行目が 互いに異なる値になるように制約を作る
	private int differentValueInColmn(int colmn, int minTableRowUp, int minTableRowDown, int root_node) {
		for (int use_row = minTableRowUp; use_row < minTableRowUp + 3; ++use_row) {
			// use_row と異なる値を minTableRowDown ~ minTableRowDown + 2 の行が取るよう、制約を作る
			int different_value_node = store.getZeroNode();
			
			if (table[use_row][colmn] != 0) {
				// マスの数値が確定している場合、その値とminTableRowDow 行から minTableRowDown+2 行が異なる値を取るという制約を統合する
				int node = differentValueInColmnCaseOfConfirmedCell(colmn, use_row, minTableRowUp, minTableRowDown);
				different_value_node = store.or(different_value_node, node);
			} else {
				// マス(use_row, colmn)の数値の候補が複数ある場合、各候補を使用した場合と
				// minTableRowDow 行から minTableRowDown+2 行が異なる値を取るという制約をそれぞれ作り、統合する。
				int node = differentValueInColmnCaseOfUnconfirmedCell(colmn, use_row, minTableRowUp, minTableRowDown);
				different_value_node = store.or(different_value_node, node);
			}
			
			root_node = store.and(root_node, different_value_node);
		}
		return root_node;
	}

	// マス(use_row, colmn)の値が確定しているときに、そのマスと minTableRowDown ~ minTableRowDown+2 の行が
	// 異なる値を取るように制約を作る
	private int differentValueInColmnCaseOfConfirmedCell(int colmn, int use_row, int minTableRowUp,
			int minTableRowDown) {
		int useVariableLevel = (getCellNumber(use_row, colmn) - 1) * 9 + table[use_row][colmn]; //確定マスがその値を使用したときの変数レベル
		int node = store.getPositive(useVariableLevel);
		
		for (int bun_row = minTableRowDown; bun_row < minTableRowDown + 3; ++bun_row) {
			// マス(bun_row, colmn) が異なる値を取るように制約を作る
			int bunVariableLevel = (getCellNumber(bun_row, colmn) - 1) * 9 + table[use_row][colmn]; //禁止マスが確定マスの値を取った時の変数レベル
			int bun_node = store.getPositive(bunVariableLevel);
			bun_node = store.not(bun_node); // 使用しないように not 演算をする
			node = store.and(node, bun_node);
		}
		return node;
	}

	// マス(use_row, colmn)の値の候補がいくつかある場合、そのマスが各候補を取った場合に minTableRowDown ~
	// minTableRowDown+2 の行が 異なる値を取るように制約を作る
	private int differentValueInColmnCaseOfUnconfirmedCell(int colmn, int use_row, int minTableRowUp,
			int minTableRowDown) {
		int result_node = store.getZeroNode();

		for (int digit = 1; digit <= 9; ++digit) {
			// マス(use_row, colmn)が数字digitを使用した場合の制約を作る
			if (!usableDigitsManager.isUsable(use_row, colmn, digit)) // 使用可能でない場合は飛ばす
				continue;
			
			// マス(use_row, colmn)が数字digitを使用した場合の変数レベル 
			int useVariableLevel = (getCellNumber(use_row, colmn) - 1) * 9 + digit; 
			int node = store.getPositive(useVariableLevel);
			
			for (int bun_row = minTableRowDown; bun_row < minTableRowDown + 3; ++bun_row) {
				// マス(bun_row, colmn) がマス(use_row, colmn)と異なるという制約を作る
				int bunVariableLevel = (getCellNumber(bun_row, colmn) - 1) * 9 + digit; // マス(bun_row, colmn)がdigitを取った時の変数レベル
				int bun_node = store.getPositive(bunVariableLevel);
				
				// 使用しないようにする
				bun_node = store.not(bun_node); 
				// 他のマスも同様に digit を取れないので and で統合する
				node = store.and(node, bun_node); 
			}
			result_node = store.or(result_node, node);
		}
		return result_node;
	}

	// row 行の各列で異なる値を取るように制約を作る
	private int differentValueInRowConstraint(int row, int root_node) {
		for (int use_colmn = 0; use_colmn < MAX_COLMN; ++use_colmn) {
			// マス(row, use_colmn) の値と他のマスが異なるように制約を作る
			if (table[row][use_colmn] != 0) { // 値が確定しているとき
				int node = differentValueInRowConstraintCaseOfConfirmedCell(row, use_colmn);
				root_node = store.and(root_node, node);
			} else { // 候補が複数ある場合
				int node = differentValueInRowConstraintCaseOfUnconfirmedCell(row, use_colmn);
				root_node = store.and(root_node, node);
			}
		}
		return root_node;
	}

	// 確定マス(row, use_colmn)のある行で、他のマスがそのマスと異なる値を取るように制約を作り、返す
	private int differentValueInRowConstraintCaseOfConfirmedCell(int row,int use_colmn) {
		int useVariableLevel = (getCellNumber(row, use_colmn) - 1) * 9 + table[row][use_colmn]; //　確定マスとその値に対する変数レベル
		int use_node = store.getPositive(useVariableLevel);
		
		for(int bun_colmn = 0; bun_colmn < MAX_COLMN; ++bun_colmn) {
			// 確定マス（row, use_colmn)とマス(row, bun_colmn) が同じ値になってはいけないという制約を作る
			if(bun_colmn == use_colmn)
				continue;
			
			int bunVariableLevel = (getCellNumber(row, bun_colmn) - 1) * 9 + table[row][use_colmn]; //禁止マスが確定マスの数値を使用したときの変数レベル
			int bun_node = store.getPositive(bunVariableLevel);
			// 使用してはいけないので not
			bun_node = store.not(bun_node); 
			use_node = store.and(use_node, bun_node);
		}
		return use_node;
	}
	
	// マス(row, use_colmn)の複数の候補を取った場合に、他のマスがそのマスと異なる値を取るようにそれぞれの制約を作り、返す
	private int differentValueInRowConstraintCaseOfUnconfirmedCell(int row,int use_colmn) {
		int different_value_constraint = store.getZeroNode();
		
		for (int digit = 1; digit <= 9; ++digit) { 
			// マス（row, use_colmn) が数字digitを取る場合
			if (!usableDigitsManager.isUsable(row, use_colmn, digit)) // 使用可能な候補の数字でないなら飛ばす
				continue;
			
			int same_digit_is_bun = store.getOneNode();
			for (int bun_colmn = 0; bun_colmn < MAX_COLMN; ++bun_colmn) {
				int bunVariableLevel = (getCellNumber(row, bun_colmn) - 1) * 9 + digit; // マス(row, bun_colmn) で digit を使用する場合の変数レベル 
				int bun_node = store.getPositive(bunVariableLevel);
				
				// 使用するマスでないなら禁止マスなので not
				if (bun_colmn != use_colmn) 
					bun_node = store.not(bun_node);
				same_digit_is_bun = store.and(same_digit_is_bun, bun_node);
			}
			different_value_constraint = store.or(different_value_constraint, same_digit_is_bun);
		}
		return different_value_constraint;
	}

	private int getMinTableConstraint(int row, int colmn, int root_node) {
		// row, colmn がそれぞれの3x3のミニテーブルの最後のマスかどうか調べて、
		// その対応する3x3のミニテーブルの制約を作り、返す
		if (row == 2 && colmn == 2)
			return differentValueInMinTableConstraint(0, 0, root_node);
		else if (row == 2 && colmn == 5)
			return differentValueInMinTableConstraint(0, 3, root_node);
		else if (row == 2 && colmn == 8)
			return differentValueInMinTableConstraint(0, 6, root_node);
		else if (row == 5 && colmn == 2)
			return differentValueInMinTableConstraint(3, 0, root_node);
		else if (row == 5 && colmn == 5)
			return differentValueInMinTableConstraint(3, 3, root_node);
		else if (row == 5 && colmn == 8)
			return differentValueInMinTableConstraint(3, 6, root_node);
		else if (row == 8 && colmn == 2)
			return differentValueInMinTableConstraint(6, 0, root_node);
		else if (row == 8 && colmn == 5)
			return differentValueInMinTableConstraint(6, 3, root_node);
		else if (row == 8 && colmn == 9)
			return differentValueInMinTableConstraint(6, 6, root_node);
		return -1;
	}

	
	// マス(row, colmn)が初めのマスである3x3のミニテーブル内で、異なる数値を取るという制約を作る
	private int differentValueInMinTableConstraint(int row, int colmn, int root_node) {
		for (int use_row = row; use_row < row + 3; ++use_row) {
			for (int use_colmn = colmn; use_colmn < colmn + 3; ++use_colmn) {
				// マス(use_row, use_colmn) と他のミニテーブル内のマスが異なるようにする制約をつくり、統合する
				if (table[use_row][use_colmn] != 0) {
					// そのマスが確定ますの場合
					int node = differentValueInMinTableConstraintCaseOfConfirmedCell(row, colmn, use_row, use_colmn);
					root_node = store.and(root_node, node);
				} else {
					// 複数の数値を取れる場合
					int node = differentValueInMinTableConstraintCaseOfUnconfirmedCell(row, colmn, use_row, use_colmn);
					root_node = store.and(root_node, node);
				}
			}
		}
		return root_node;
	}
	
	// 確定マス(use_row, use_colmn) と他のミニテーブルのマスが異なる値を取るように制約を作る
	private int differentValueInMinTableConstraintCaseOfConfirmedCell(
			int row, // 3x3 のミニテーブルの最初のマスの行番号
			int colmn, // 3x3のミニテーブルの最初のマスの列番号
			int use_row, // 確定マスの行番号
			int use_colmn // 確定マスの列番号
			) {
		int use_node = store.getOneNode();
		
		for (int bun_row = row; bun_row < row + 3; ++bun_row) {
			for (int bun_colmn = colmn; bun_colmn < colmn + 3; ++bun_colmn) {
				// マス(bun_row, bun_colmn) が確定マスと異なる数値を取るという制約を作る
				
				// マス(bun_row, bun_colmn) が確定マスと同じ数字を取った場合の変数レベル
				int bunVariableLevel = (getCellNumber(bun_row, bun_colmn) - 1) * 9
						+ table[use_row][use_colmn];
				int bun_node = store.getPositive(bunVariableLevel);
				// 確定マスと異なるマスなら使用禁止にする
				if (use_row != bun_row || use_colmn != bun_colmn)
					bun_node = store.not(bun_node); 
				use_node = store.and(use_node, bun_node);
			}
		}
		return use_node;
	}
		
	// マス(use_row, use_colmn) に複数の数字の候補がある場合、そのどれかを取った時に他のミニテーブルのマスが異なる値を取るように制約を作る
	private int differentValueInMinTableConstraintCaseOfUnconfirmedCell(int row, int colmn, int use_row, int use_colmn) {
		int or_node = store.getZeroNode();
		
		for (int use_digit = 1; use_digit <= 9; ++use_digit) {
			// マス（use_row, use_colmn) が数字 digit を取った場合に、ミニテーブルの他のマスが異なる数字を取るという制約を作る
			
			// マス(use_row, use_colmn) が digit を取れないなら飛ばす
			if (!usableDigitsManager.isUsable(use_row, use_colmn, use_digit))
				continue;
			int same_digit_is_bun = store.getOneNode();
			for (int bun_row = row; bun_row < row + 3; ++bun_row) {
				for (int bun_colmn = colmn; bun_colmn < colmn + 3; ++bun_colmn) {
					// ミニテーブル内のマス(bun_row, bun_colmn)について、マス(use_row, use_colm) と異なる数字を取るようにする制約を作る
					
					// マス(bun_row, bun_colmn) が数字 digit を取った時の変数レベル
					int bunVariableLevel = (getCellNumber(bun_row, bun_colmn) - 1) * 9 + use_digit;
					int bun_node = store.getPositive(bunVariableLevel);
					
					// マス（use_row, use_colmn) と異なるなら、その変数を使用しないようにする
					if (use_row != bun_row || use_colmn != bun_colmn)
						bun_node = store.not(bun_node);
					same_digit_is_bun = store.and(same_digit_is_bun, bun_node);
				}
			}
			or_node = store.or(or_node, same_digit_is_bun);
		}
		return or_node;
	}
		
	// そのマスの数字が確定している場合と、いくつかの候補がある場合の制約の BDD を生成し返す
	private int oneCellConstraint(int row, int colmn) {
		int root_node = store.getOneNode();
		
		if (table[row][colmn] != 0) {
			// 確定している場合、その数字を使用する制約を作り、統合する
			int confirmed_num_constraint = confirmedNumberConstraint(row, colmn);
			root_node = store.and(root_node, confirmed_num_constraint);
		} else {
			// 使用可能な数字を組み合わせた制約を作り、統合する
			root_node = store.and(root_node, usableNumberCombinationConstraint(row, colmn));
		}
		return root_node;
	}

	// 表全体の中でのマスの番号を返す
	int getCellNumber(int row, int colmn) {
		return colmn + row * MAX_COLMN + 1;
	}

	// マスの値が確定している場合の制約を返す
	private int confirmedNumberConstraint(int row, int colmn) {
		final int digitNum = 9;
		int useNum = table[row][colmn];
		int cellNum = getCellNumber(row, colmn);
		int ret_node = store.getOneNode();
		// useNum を使用し、それ以外の数字は使用しないという制約を作る
		for (int i = 1; i <= digitNum; ++i) {
			int variableLevel = (cellNum - 1) * digitNum + i; // 変数レベルを求める
			int node = store.getPositive(variableLevel);
			if (i != useNum) // 確定している数字でない場合は使用しない
				node = store.not(node);
			ret_node = store.and(ret_node, node);
		}
		return ret_node;
	}

	// そのマスにおいて使用可能な数字を組み合わせた制約を作る
	private int usableNumberCombinationConstraint(int row, int colmn) {
		int ret_node = store.getZeroNode();
		int cellNum = getCellNumber(row, colmn);
		for (int i = 1; i <= 9; ++i) {
			if (!usableDigitsManager.isUsable(row, colmn, i)) // 数字iが使用可能でないなら飛ばす
				continue;
			int use_once_node = store.getOneNode();
			// 数字 i を使用しそれ以外の j を使用しない制約を作る
			for (int j = 1; j <= 9; ++j) {
				int variableLevel = (cellNum - 1) * 9 + j;
				int node = store.getPositive(variableLevel);
				if (i != j)
					node = store.not(node);
				use_once_node = store.and(use_once_node, node);
			}
			// 使用可能な数字の制約を or で組み合わせてどれかを必ず使用する制約を作る
			ret_node = store.or(ret_node, use_once_node);
			// System.out.println(i + " or " + store.getBddExpressString(ret_node));
		}
		return ret_node;
	}
}
