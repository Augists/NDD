package bddvector;

import java.util.ArrayList;

import jdd.bdd.BDD;

public class NDDQueens {
	public int field_num;
	public int[] upperBound;
	public double[] div;
	public int[] bdds, nbdds;
	public int N;
	public NDD queen;
	public int bddqueen;
	public long time;
	public BDD bdd;

	private int X(int x, int y) {
		return bdds[y + x * N];
	}

	private int nX(int x, int y) {
		return nbdds[y + x * N];
	}

	private int[] boundPolicy1(int N, int F) {
		int[] bound = new int[F];
		for (int curr = 0; curr < F; curr++) {
			bound[curr] = (curr + 1) * (N * N / F) - 1;
		}
		bound[F - 1] = N * N - 1;
		return bound;
	}

	public double[] calculateDiv(int N, int F) {
		double[] ret = new double[F];
		for (int curr = 0; curr < F; curr++) {
			int len;
			if (curr == 0) {
				len = upperBound[curr] + 1;
			} else {
				len = upperBound[curr] - upperBound[curr - 1];
			}
			len = N * N - len;
			double d = 1.0;
			for (int t = 0; t < len; t++) {
				d = d * 2;
			}
			ret[curr] = d;
		}
		return ret;
	}

	public NDDQueens(int N, int F) {
		bdd = new BDD(1 + Math.max(100000, (int) (Math.pow(4.4, N - 6)) * 1000), 10000);
		this.N = N;
		this.field_num = F;
		NDD.bdd = bdd;
		time = System.currentTimeMillis();

		upperBound = boundPolicy1(N, F);
		div = calculateDiv(N, F);

		NDD.SetFieldNum(F);
		NDD.SetUpperBound(upperBound);

		// declare var line by line
		int all = N * N;
		bdds = new int[all];
		nbdds = new int[all];
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

		// queen = NDD.NDDTrue;
		queen = new NDD();
		ArrayList<Integer> vec = new ArrayList<>();
		for (int i = 0; i < field_num; i++)
			vec.add(1);
		queen.vectors.add(vec);

		//
		// bddqueen = 1;

		for (int i = 0; i < N; i++) {
			// System.out.println("orBatch " + i + " " + orBatch[i]);

			NDD predNDD = NDD.toNDD(orBatch[i]);

			// int back = NDD.toBDD(predNDD);
			// int pre = orBatch[i];
			// if (bdd.getVar(back) != bdd.getVar(pre)) {
			// System.out.println("not equal " + back + " " + pre);
			// }
			// bdd.printDot("/data/augists/NQueen/pre"+i, pre);
			// bdd.printDot("/data/augists/NQueen/back"+i, back);

			// System.out.println("bdd vector " + predNDD.vectors);

			queen = NDD.AND(queen, predNDD);

			//
			// int old = bddqueen;
			// bddqueen = bdd.ref(bdd.and(bddqueen, orBatch[i]));
			// bdd.deref(old);

			// System.out.println("queen" + queen.vectors);
			// bdd.printDot("/data/augists/NQueen/bddqueen_"+bddqueen, bddqueen);
		}

		for (int i = 0; i < N; i++) {
			for (int j = 0; j < N; j++) {
				NDD predNDD = NDD.toNDD(impBatch[i][j]);

				// int back = NDD.toBDD(predNDD);
				// int pre = impBatch[i][j];
				// if (bdd.getVar(back) != bdd.getVar(pre)) {
				// System.out.println("not equal " + back + " " + pre);
				// }
				// bdd.printDot("/data/augists/NQueen/pre_"+i+"_"+j, pre);
				// bdd.printDot("/data/augists/NQueen/back_"+i+"_"+j, back);

				// System.out.println("i j bdd vector " + i + "_" + j + "_" + predNDD.vectors);

				queen = NDD.AND(queen, predNDD);
				
				//
				// int old = bddqueen;
				// bddqueen = bdd.ref(bdd.and(bddqueen, impBatch[i][j]));
				// bdd.deref(old);

				System.out.println("i j queen vectors " + queen.vectors.size());
				// System.out.println(i+"_"+j+"_"+"sat count: " + NDD.satCount(queen));

				// System.out.println("ndd queen: " + NDD.toBDD(queen) + " bdd queen: " + bddqueen);

				// bdd.printDot("/data/augists/NQueen/bddqueen_"+i+"_"+j+"_"+bddqueen, bddqueen);
				// System.out.println("bdd queen sat count: " + bdd.satCount(bddqueen));
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
		NDDQueens q;
		q = new NDDQueens(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
		// System.out.println(
		// 		"N:" + q.N + " Field:" + q.field_num + " Time:" + q.time);
		System.out.println(
				"N:" + q.N + " Field:" + q.field_num + " Time:" + q.time + " Sat:" + NDD.satCount(q.queen));
	}
}