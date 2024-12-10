/*
 * copy from jdd/examples
 */
package jdd.examples;

import jdd.bdd.*;
import jdd.util.*;
import jdd.util.math.*;

/**
 * <pre>
 * The N Queens on a N * N chessboard...
 * we have borrowed this from JavaBDD, to see how much of the [lack of] speed depends
 * on Java...
 *
 * Note that this problem can be solved far faster with a more intelligent use of BDDs.
 *
 * [e-mail Arash and ask for an explanation, because none of us knows why he claims such a thing...]
 *
 * </pre>
 */

public class BDDQueens extends BDD implements Queens {
	private int[] bdds, nbdds;
	private int N, queen;
	private double sols, memory_usage;
	private long time;
	private boolean[] solvec;

	private int X(int x, int y) {
		return bdds[y + x * N];
	}

	private int nX(int x, int y) {
		return nbdds[y + x * N];
	}

	public BDDQueens(int N) {
		super(1 + Math.max(1000, (int) (Math.pow(4.4, N - 6)) * 1000), 10000);

		this.N = N;

		time = System.currentTimeMillis();

		int all = N * N;
		bdds = new int[all];
		nbdds = new int[all];
		for (int i = 0; i < all; i++) {
			bdds[i] = createVar();
			nbdds[i] = ref(not(bdds[i]));
		}

		queen = 1;

		int[] orBatch = new int[N];
		int[][] impBatch = new int[N][N];

		for (int i = 0; i < N; i++) {
			int e = 0;
			for (int j = 0; j < N; j++)
				e = orTo(e, X(i, j));
			orBatch[i] = e;
		}

		for (int i = 0; i < N; i++)
			for (int j = 0; j < N; j++) {
				build(i, j, impBatch);
			}

		for(int i=0;i<N;i++)
		{
			queen = andTo(queen, orBatch[i]);
			deref(orBatch[i]);
		}

		for (int i = 0; i < N; i++) {
			for (int j = 0; j < N; j++) {
				queen = andTo(queen, impBatch[i][j]);
				deref(impBatch[i][j]);
			}
		}

		time = System.currentTimeMillis() - time;
		sols = satCount(queen);
	}

	private void build(int i, int j, int[][] impBatch) {
		int a, b, c, d;
		a = b = c = d = 1;

		int k, l;

		/* No one in the same column */
		for (l = 0; l < N; l++)
			if (l != j) {
				int mp = ref(imp(X(i, j), nX(i, l)));
				a = andTo(a, mp);
				deref(mp);
			}

		/* No one in the same row */
		for (k = 0; k < N; k++)
			if (k != i) {
				int mp = ref(imp(X(i, j), nX(k, j)));
				b = andTo(b, mp);
				deref(mp);
			}

		/* No one in the same up-right diagonal */
		for (k = 0; k < N; k++) {
			int ll = k - i + j;
			if (ll >= 0 && ll < N)
				if (k != i) {
					int mp = ref(imp(X(i, j), nX(k, ll)));
					c = andTo(c, mp);
					deref(mp);
				}
		}

		/* No one in the same down-right diagonal */
		for (k = 0; k < N; k++) {
			int ll = i + j - k;
			if (ll >= 0 && ll < N)
				if (k != i) {
					int mp = ref(imp(X(i, j), nX(k, ll)));
					d = andTo(d, mp);
					deref(mp);
				}
		}

		c = andTo(c, d);
		deref(d);
		b = andTo(b, c);
		deref(c);
		a = andTo(a, b);
		deref(b);

		impBatch[i][j] = a;
	}

	public void showOneSolution() {
		if (solvec == null)
			return; // no solutions
		for (int x = 0; x < solvec.length; x++) {
			if ((x % N) == 0)
				JDDConsole.out.println();
			JDDConsole.out.print(solvec[x] ? "*|" : "_|");
		}
		JDDConsole.out.println();
	}

	// ---------------------------------------
	public int getN() {
		return N;
	}

	public double numberOfSolutions() {
		return sols;
	}

	public long getTime() {
		return time;
	}

	public double getMemory() {
		return memory_usage;
	}

	public boolean[] getOneSolution() {
		return solvec;
	}

	// -------------------------------------------
	public static void main(String[] args) {
		BDDQueens q;
		q = new BDDQueens(Integer.parseInt(args[0]));
		System.out.println("There are " + q.numberOfSolutions() + " solutions time: " + q.time + " N: " + args[0]);
	}

	/** testbench. do not call */
	public static void internal_test() {
		Test.start("BDDQueens");
		int[] correct = { 1, 0, 0, 2, 10, 4, 40, 92 /* , 352, 724 */ };
		for (int i = 0; i < correct.length; i++) {
			BDDQueens q = new BDDQueens(i + 1);
			// System.out.println("Q"+ (i + 1) + " --> " + q.numberOfSolutions());
			Test.checkEquality(q.numberOfSolutions(), correct[i], "correct solutions for " + (i + 1) + " queens");
		}
		Test.end();
	}
}
