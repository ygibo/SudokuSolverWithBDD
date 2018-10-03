package BddTools;
import java.util.Vector;
import java.util.Objects;
import java.util.HashMap;
import java.util.Map;

public class BddNodeStore {
	
	public static void main(String[] args) {
		System.out.println("start");
		BddNodeStore store = new BddNodeStore();
		int c = store.getPositive(1);
		c = store.not(c);
		int b = store.getPositive(2);
		b = store.or(b, c);
		int a = store.getPositive(3);
		int ret = store.or(store.and(store.not(a), c), store.and(a, b));
		System.out.println(store.getBddExpressString(ret));
		System.out.println(store.getBddExpressString(store.not(ret)));
		//store.show_store();
	}

	// BDD レコード、重複した BDD を生成しないように使用する
	private final class BddRecord{
		private final int varLevel;
		private final int zero_edge, one_edge;

		public BddRecord(int level, int zero, int one) {
			varLevel = level;
			zero_edge = zero;
			one_edge = one;
		}

		public int getLevel() {
			return varLevel;
		}

		public int getZeroEdge() {
			return zero_edge;
		}

		public int getOneEdge() 
		{
			return one_edge;
		}

		@Override
		public boolean equals(Object object) {
			if(object instanceof BddRecord) {
				BddRecord node = (BddRecord) object;
				return (varLevel == node.varLevel) && (zero_edge == node.zero_edge) &&
						(one_edge == node.one_edge);
			}
			return false;
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(varLevel, zero_edge, one_edge);
		}
		
		public void show() {
			System.out.println(varLevel + ", " + zero_edge + ", " + one_edge);
		}
	}

	// 演算キャッシュ用のレコード
	private final class CacheRecord{
		private final char op;
		private final int left, right;
		public CacheRecord(int lhs, int rhs, char operand) {
			op = operand;
			left = lhs;
			right = rhs;
		}
		@Override
		public boolean equals(Object object) {
			if(object instanceof CacheRecord) {
				CacheRecord record = (CacheRecord) object;
				if(op == record.op &&
						left == record.left &&
						right == record.right)
					return true;
			}
			return false;
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(op, left, right);
		}
	}
	
	private Vector<BddRecord> bddStore = new Vector<>(); // BDD レコードを保持
	private Map<BddRecord, Integer> nodeMap = new HashMap<BddRecord, Integer>(); // BDD レコードとそのインデックスを記録
	private Map<CacheRecord, Integer> calcCache = new HashMap<CacheRecord, Integer>(); // 演算キャッシュ
	
	// 作成された BDD レコードの数を返す
	public int getNodeSize() {
		return bddStore.size();
	}
	
	// 使用したハッシュのサイズを返す
	public int getHashSize() {
		return nodeMap.size();
	}
	
	// 使用したキャッシュサイズを返す
	public int getCacheSize() {
		return calcCache.size();
	}
	
	// BDD ノードの初期化、Zeroノード、One ノードを作り保存
	public BddNodeStore() {
		bddStore.addElement(new BddRecord(0, 0, 0)); // add zero node
		bddStore.addElement(new BddRecord(0, 1, 1)); // add one node
	}
	
	// 作成された BDD ノードの一覧を表示する
	public void show_store() {
		for(BddRecord node : bddStore) {
			node.show();
		}
	}

	// Zero ノードを得る
	public int getZeroNode() {
		return 0;
	}

	// One ノードを得る
	public int getOneNode() {
		return 1;
	}

	// BDD レコードの登録
	private int register(BddRecord record) {
		//　ハッシュを調べ、すでに存在しているなら既存のレコードのインデックスを返す
		Integer existNode = nodeMap.get(record);
		if(existNode == null) { // 存在していないなら新たに登録する
			existNode = bddStore.size();
			nodeMap.put(record, bddStore.size());
			bddStore.addElement(record);
		}
		return existNode;
	}

	// node_f ノードとnode_b ノードを op 演算で統合し、その結果のノードを返す
	private int calcurateOperand(int node_f, int node_b, char op) {
		switch(op) {
		case '+':
			return bool_or(node_f, node_b);
		case '*':
			return bool_and(node_f, node_b);
		}
		return 0;
	}
	
	// node_f, node_b が Zero ノードかOne ノードのときの or, 簡単に計算できる
	private int bool_or(int node_f, int node_b) {
		if(node_f == 1)
			return 1;
		else if(node_f == 0)
			return node_b;
		else if(node_b == 1)
			return 1;
		else if(node_b == 0)
			return node_f;
		return 0;
	}
	
	// node_f, node_b が Zero ノードかOne ノードのときの and, 簡単に計算できる
	private int bool_and(int node_f, int node_b) {
		if(node_f == 1)
			return node_b;
		else if(node_f == 0)
			return 0;
		else if(node_b == 1)
			return node_f;
		else if(node_b == 0)
			return 0;
		return 0;
	}
	
	// Apply 演算、op 演算を再帰的に行い、node_f, node_b ノードを統合する
	private int apply(int node_f, int node_b, char op) {
		if(node_f == 0 || node_f == 1 || node_b == 0 || node_f == 1)
			return calcurateOperand(node_f, node_b, op);
		CacheRecord cacheRecord = new CacheRecord(node_f, node_b, op);
		Integer node = calcCache.get(cacheRecord);
		if(node != null) {
			//System.out.println("hit");
			return node;
		}
		
		int leftLevel = bddStore.elementAt(node_f).getLevel();
		int rightLevel = bddStore.elementAt(node_b).getLevel();
		
		if(leftLevel > rightLevel) {
			int nextLeft = apply(bddStore.elementAt(node_f).getZeroEdge(), node_b, op);
			int nextRight = apply(bddStore.elementAt(node_f).getOneEdge(), node_b, op);
			if(nextLeft == nextRight)
				return nextLeft;
			BddRecord record = new BddRecord(leftLevel, nextLeft, nextRight);
			node = register(record);
		}else if(leftLevel == rightLevel) {
			int nextLeft = apply(bddStore.elementAt(node_f).getZeroEdge(),
					bddStore.elementAt(node_b).getZeroEdge(), op);
			int nextRight = apply(bddStore.elementAt(node_f).getOneEdge(),
					bddStore.elementAt(node_b).getOneEdge(), op);
			if(nextLeft == nextRight)
				return nextLeft;
			BddRecord record = new BddRecord(leftLevel, nextLeft, nextRight);
			node = register(record);
		}else if(leftLevel < rightLevel) {
			int nextLeft = apply(node_f, bddStore.elementAt(node_b).getZeroEdge(), op);
			int nextRight = apply(node_f, bddStore.elementAt(node_b).getOneEdge(), op);
			if(nextLeft == nextRight)
				return nextLeft;
			BddRecord record = new BddRecord(rightLevel, nextLeft, nextRight);
			node = register(record);
		}
		calcCache.put(cacheRecord, node);
		return node;	
		
	}
	
	// not 演算
	public int not(int node) {
		if(node == 0)
			return 1;
		else if(node == 1)
			return 0;
		CacheRecord cacheRecord = new CacheRecord(node, 0, '^');
		Integer cacheNode = calcCache.get(cacheRecord);
		if(cacheNode != null)
			return cacheNode;
		
		int level = bddStore.elementAt(node).getLevel();
		int left = bddStore.elementAt(node).getZeroEdge();
		int right = bddStore.elementAt(node).getOneEdge();
		BddRecord bddRecord = new BddRecord(level, not(left), not(right));
		int retNode = register(bddRecord);
		calcCache.put(cacheRecord, retNode);
		return retNode;
	}
	
	// 変数レベルがvarLevelの変数について、1枝がOne ノード、0枝がZeroノードのノードを返す
	public int getPositive(int varLevel) {
		BddRecord bddRecord = new BddRecord(varLevel, 0, 1);
		return register(bddRecord);
	}

	// and 演算
	public int and(int leftNode, int rightNode) {
		if(leftNode == rightNode)
			return leftNode;
		return apply(leftNode, rightNode, '*');
	}
	
	// or 演算
	public int or(int leftNode, int rightNode) {
		if(leftNode == rightNode)
			return leftNode;
		return apply(leftNode, rightNode, '+');
	}
	
	// bddNode ノードを文字列で表示する
	public String getBddExpressString(int bddNode) {
		if(bddNode == 0)
			return Integer.toString(0);
		else if(bddNode == 1)
			return Integer.toString(1);
		BddRecord currentNode = bddStore.elementAt(bddNode);
		return new String(
				"(" + Integer.toString(currentNode.getLevel()) + 
				", " + getBddExpressString(currentNode.getZeroEdge()) +
				", " + getBddExpressString(currentNode.getOneEdge()) + 
				")");
	}
	
	private Vector<String> path;
	
	// One ノードに達するパスをすべて列挙する
	public void enumerate(int bddNode) {
		Vector<String> vec = new Vector<>();
		path = new Vector<String>();
		enumerate_sub(bddNode, 0, vec);
	} 
	
	public String getPath(int i) {
		if(path.size() <= i)
			return null;
		return path.elementAt(i);
	}
	
	private void set_vec(Vector<String> vec) {
		String str = new String("");
		for(int i = vec.size() - 1; i >= 0; --i) {
			String num = vec.elementAt(i);
			if(num.charAt(0) != '^') {
				str += ((Integer.parseInt(num) - 1) % 9) + 1;
				str += " ";
			}
		}
		path.addElement(str);
	}
	
	// One ノードに達するパスを列挙する再帰関数
	private void enumerate_sub(int bdd, int index, Vector<String> vec) {
		if(bdd == 1) {
			set_vec(vec);
			return;
		}else if(bdd == 0)
			return;
	
		BddRecord record = bddStore.elementAt(bdd);
		
		// follow zero edge
		if(index >= vec.size())
			vec.addElement(new String("^" + record.varLevel));
		else
			vec.setElementAt(new String("^" + record.varLevel), index);
		enumerate_sub(record.getZeroEdge(), index + 1, vec);
		
		// follow one edge
		if(index >= vec.size())
			vec.addElement(Integer.toString(record.getLevel()));
		else
			vec.setElementAt(Integer.toString(record.getLevel()), index);
		enumerate_sub(record.getOneEdge(), index+1, vec);
	}
}