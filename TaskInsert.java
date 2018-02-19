import java.util.ArrayList;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class TaskInsert implements Runnable {
	boolean condVid;
	String operator;
	Table t;
	int lineToStart;
	int lineToStop;
	Column colToTest;
	int pos;
	String valToTest;
	String op;
	CyclicBarrier barrier;
	ArrayList<Integer> indeces;
	
	public TaskInsert(Table t, Column colToTest, boolean condVid, String operator, String valToTest, int lineToStart, int lineToStop, 
			ArrayList<Integer> indeces, CyclicBarrier barrier) {
		this.operator = operator;
		this.indeces = indeces;
		this.lineToStart = lineToStart;
		this.lineToStop = lineToStop;
		this.valToTest = valToTest;
		this.condVid = condVid;
		this.t = t;
		this.barrier = barrier;
		this.colToTest = colToTest;
	}
	@Override
	public void run() {
		int type = 0;
		int compTo = 0;
		//daca conditia nu este nu este vida determinam tipul valorii cu care comparam (din conditie)
		if(!condVid) {
			if (valToTest.equals("true") || valToTest.equals("false")) {
				type = 1;
			} else {
				try {
					type = 0;
					compTo = Integer.parseInt(valToTest);
				} catch (Exception e) {
					type = 2;
				}
			}
		}
		if (!condVid) {
			//parcurgem tabela linie cu linie, de la pozitia de start pana la cea de stop
			for (int j = lineToStart; j < lineToStop; j++) {
				switch (operator) {
				//conditie de tipul element_coloana == valoare (valToTest)
				case "==":
					//verificam conditia in functie de tipul lui valToTest
					switch (type) {
					case 1:
						if (((Boolean) colToTest.getValues().get(j)).equals(Boolean.getBoolean(valToTest))) {
							indeces.add(j);
						}
						break;
					case 0:
						if (((int) colToTest.getValues().get(j)) == (Integer.parseInt(valToTest))) {
							indeces.add(j);
						}
						break;
					default:
						if (((String) colToTest.getValues().get(j)).equals(valToTest)) {
							indeces.add(j);
						}
					}
					break;
				case "<":
					//conditie de tipul element_coloana < valoare
					//daca valoarea nu este int se va arunca automat o exceptie
					compTo = Integer.parseInt(valToTest);
					if (((int) colToTest.getValues().get(j)) < compTo) {
						indeces.add(j);
					}
					break;
				default:
					//conditie de tipul element_coloana > valoare
					//daca valoarea nu este int se va arunca automat o exceptie
					compTo = Integer.parseInt(valToTest);
					if (((int) colToTest.getValues().get(j)) > compTo) {
						indeces.add(j);
					}
				}
			}
		} else {
			//conditia este vida, asa ca punem in lista rezultat poate pozitiile intre valoarea
			//de start si cea de stop
			for (int j = lineToStart; j < lineToStop; j++) {
				indeces.add(j);
			}
		}
		//asteptam sa termine si celalte threaduri executia
		try {
			barrier.await();
		} catch (InterruptedException | BrokenBarrierException e) {
			e.printStackTrace();
		}
	}

}
