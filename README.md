# SudokuSolverWithBDD
Sudoku solver using BDD (Binary decision diagram)

# BDD を利用した数独ソルバー  
BDD が制約式を表現するデータ構造であることを利用し、数独ソルバーを作成した。

## 使用方法  
以下のように使用する
数独の9*9の表を長さ81の文字列で表現する。つまり、表の上の行から順につなげた文字列で、空白行は数字０で表現する。
それを SudokuSolver クラスのコンストラクタに渡し、数独ソルバーのインスタンスを作成する.
、
SudokuSolver solver = new SudokuSolver(ProblemFactory.min_hint_problem);
このようにする。min_hint_problem が数独の問題を表現している文字列
そして、solver.getBddOfTheConstrant() で数独の制約を表す BDD を得る。その BDD を
solver.showAns() の引数とすることで解が表示される。

### 例  
int bdd = solver.getBddOfTheConstraint();
solver.showAns(bdd);
System.out.println("node num " + solver.getSize());
System.out.println("hash size " + solver.getHashSize());
System.out.println("cache size " + solver.getCacheSize());

このように実行すると  
、
create constrain. time is 2004ms  
------------------  
Answer No.1  
3 2 9 8 5 1 7 4 6   
8 5 4 6 7 9 1 3 2  
7 6 1 2 3 4 9 8 5  
5 9 2 3 8 7 4 6 1  
6 4 3 1 9 2 5 7 8  
1 8 7 4 6 5 2 9 3  
2 7 6 9 1 8 3 5 4  
4 3 5 7 2 6 8 1 9  
9 1 8 5 4 3 6 2 7  
------------------ 
node num 420027  
hash size 420025  
cache size 1755652  

のような答えを表示する。上から順に
１制約を表す BDD を作成するのに使用した時間
２数独の答え
３作成したノード、ハッシュ、キャッシュの数
を表示している。

## プログラムの説明  
SudokuSolverWithBDD/src/BddTools/BddNodeStore.java　は、作成した BDD ライブラリ
SudokuSolverWithBDD/src/SudokuSolver.java　で、BDD ライブラリを利用し数独の制約を表す BDD を作成している
数独の制約を BDD で表現するために、各マスが１～９のどれか数字を使用したことを表す変数を導入している。
この変数の1-枝、0-枝ををたどることで、そのマスが数字 d を取る、取らないを表現している。

### 変数順序  
変数順序によって BDD の作成効率が大きく異なってくる。そのため、プログラムで使用した変数順序について説明する。
変数順序は左上のマスから、右のマスへ辿っていき、端まで来たらその下の段の左端へ戻るという順序。その順番で
初めのマスを2番とし順に付けた。マスの番号として左上から2～82番を取る。そして、各マスには1～9までの数字を取るため、
それに対応するために、マスの番号をiとすると、i がそのマスが数字 1 を取った時の変数レベル、i+1 が数字 2 を取った時の変数レベル
i+j (1<=j<=9) が数字 j を取った時の変数レベルとなる。

### 制約の作成  
BDD 数独の制約を作成し統合していくことで、数独全体の制約を表す BDD を作成する。しかし、作成し統合する順序によって BDD の統合に時間がかかる。
そのため、できるだけ時間がかからないように制約を作成し、統合している。具体的にはある1つの制約式において変数番号が大きく離れている場合、
その制約式をできるだけ小さい制約式の集合に分割して統合している。これにより、通常よりも大きく時間が削減された。
