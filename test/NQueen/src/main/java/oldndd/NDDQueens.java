package oldndd;

import jdd.bdd.BDD;

public class NDDQueens {
	public static int field_num;
	public static int[] bound;
	public static double [] div;
	public int[] bdds, nbdds;
	public static int N;
	public FieldElement queen;
	public long time;
	public BDD bdd;
	// public FieldManager [] fieldManagers;

	private int X(int x, int y) {
		return bdds[y + x * N];
	}

	private int nX(int x, int y) {
		return nbdds[y + x * N];
	}

	public NDDQueens(int N) {
		bdd = new BDD(1 + Math.max(1000, (int) (Math.pow(4.4, N - 6)) * 1000), 10000);
		NDDQueens.N = N;
		FieldManager.bdd = bdd;
		time = System.currentTimeMillis();

		// fieldManagers = new FieldManager [field_num];
		// for(int curr=0;curr<field_num;curr++)
		// {
		// fieldManagers[curr] = new FieldManager(curr, this);
		// }
		
		bound = new int [field_num];
		for (int curr = 0; curr < field_num; curr++) {
			bound[curr] = (curr + 1) * (N*N / field_num);
		}
		bound[field_num - 1] = N*N;
		div = new double [field_num];
		for (int curr = 0; curr < field_num; curr++) {
			int len;
			if(curr == 0)
			{
				len = bound[curr];
			}
			else
			{
				len = bound[curr] - bound[curr-1];
			}
			len = N*N - len;
			double d = 1.0;
			for(int t=0;t<len;t++)
			{
				d = d*2;
			}
			div[curr] = d;
		}
		FieldElement.q = this;

		int all = N * N;
		bdds = new int[all];
		nbdds = new int[all];

		//declare var line by line
		for (int i = 0; i < all; i++) {
			bdds[i] = bdd.createVar();
			nbdds[i] = bdd.ref(bdd.not(bdds[i]));
		}

		int[] orBatch = new int[N];
		int[][] impBatch = new int[N][N];

		for (int i = 0; i < N; i++) {
			int e = 0;
			for (int j = 0; j < N; j++)
				e = bdd.orTo(e, X(i, j));
			orBatch[i] = e;
		}

		for (int i = 0; i < N; i++) {
			for (int j = 0; j < N; j++) {
				build(i, j, impBatch);
			}
		}

		queen = FieldElement.NDDTrue;

		for (int i = 0; i < N; i++) {
			FieldElement predNDD = FieldElement.toNDD(orBatch[i]);
			queen = FieldElement.And(queen, predNDD);
		}

		for (int i = 0; i < N; i++) {
			for (int j = 0; j < N; j++) {
				FieldElement predNDD = FieldElement.toNDD(impBatch[i][j]);
				queen = FieldElement.And(queen, predNDD);
			}
		}

		time = System.currentTimeMillis() - time;
	}

	private void build(int i, int j, int[][] impBatch) {
		int a, b, c, d;
		a = b = c = d = 1;

		int k, l;

		/* No one in the same column */
		for (l = 0; l < N; l++)
			if (l != j) {
				int mp = bdd.ref(bdd.imp(X(i, j), nX(i, l)));
				a = bdd.andTo(a, mp);
				bdd.deref(mp);
			}

		/* No one in the same row */
		for (k = 0; k < N; k++)
			if (k != i) {
				int mp = bdd.ref(bdd.imp(X(i, j), nX(k, j)));
				b = bdd.andTo(b, mp);
				bdd.deref(mp);
			}

		/* No one in the same up-right diagonal */
		for (k = 0; k < N; k++) {
			int ll = k - i + j;
			if (ll >= 0 && ll < N)
				if (k != i) {
					int mp = bdd.ref(bdd.imp(X(i, j), nX(k, ll)));
					c = bdd.andTo(c, mp);
					bdd.deref(mp);
				}
		}

		/* No one in the same down-right diagonal */
		for (k = 0; k < N; k++) {
			int ll = i + j - k;
			if (ll >= 0 && ll < N)
				if (k != i) {
					int mp = bdd.ref(bdd.imp(X(i, j), nX(k, ll)));
					d = bdd.andTo(d, mp);
					bdd.deref(mp);
				}
		}

		c = bdd.andTo(c, d);
		bdd.deref(d);
		b = bdd.andTo(b, c);
		bdd.deref(c);
		a = bdd.andTo(a, b);
		bdd.deref(b);

		impBatch[i][j] = a;
	}

	public static void main(String[] args) {
		field_num = Integer.parseInt(args[1]);
		NDDQueens q;
		q = new NDDQueens(Integer.parseInt(args[0]));
		System.out.println("N:"+N+" Field:"+field_num+" Time:"+q.time+" Sat:"+FieldElement.satCount(q.queen));
		// System.out.println("N:"+N+" Field:"+field_num+" Time:"+time+" Sat:"+FieldElement.satCount(q.queen) + " BDD node: " + BDD.mkNodeCount + " NDD node: " + FieldElement.mkCount);
	}
}