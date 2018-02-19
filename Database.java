
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Database implements MyDatabase {
	public HashMap<String, Table> tables;
	ExecutorService ex;
	int numWorkerThreads;

	public Database() {
		this.tables = new HashMap<>();
	}

	@Override
	// initializeaza baza de date cu numWorker thread-uri
	public void initDb(int numWorkerThreads) {
		this.numWorkerThreads = numWorkerThreads;
		ex = Executors.newFixedThreadPool(numWorkerThreads);
	}

	@Override
	// opreste serviciul de baza de date
	public void stopDb() {
		ex.shutdown();
	}

	@Override
	// creeaza tabela tableName, cu coloaneme columnName de tip columnType
	public void createTable(String tableName, String[] columnNames, String[] columnTypes) {
		Column c;
		int i = 0;
		Table t = new Table(tableName);
		for (String n : columnNames) {
			c = new Column(n, columnTypes[i], new ArrayList<Object>());
			t.addColumn(c);
			i++;
		}
		tables.put(tableName, t);
	}

	// returneaza liste cu indicii care respecta conditia primita
	// conditia este primita sub forma: colToTest operator valToTest
	public synchronized ArrayList<ArrayList<Integer>> getIndeces(Table t, String valToTest, Column colToTest,
			String operator, boolean condVid) {
		int N = t.getColumns().get(0).getValues().size();
		ArrayList<ArrayList<Integer>> indeces = new ArrayList<>();
		// folosim o bariera prin care vor trece main-ul si cele numWorkerThreads pentru
		// a ne asigura ca nu
		// returnam rezultatul pana cand nu au terminat toate threadurile de executat
		// partea lor

		CyclicBarrier barrier = new CyclicBarrier(numWorkerThreads + 1);
		// vom imparti tabelul celor numWorkerTreads si fiecare va calcula o lista cu
		// inidcii liniilor din partea lor de tabel pentru care se respecta conditia
		for (int j = 0; j <= numWorkerThreads - 1; j++) {
			indeces.add(new ArrayList<Integer>());
			if (!condVid) {
				ex.submit(new TaskInsert(t, colToTest, condVid, operator, valToTest, j * N / numWorkerThreads,
						(j + 1) * N / numWorkerThreads, indeces.get(j), barrier));
			} else {
				ex.submit(new TaskInsert(t, colToTest, condVid, "", "", j * N / numWorkerThreads,
						(j + 1) * N / numWorkerThreads, indeces.get(j), barrier));
			}
		}

		// asteptam sa termine toate threadurile
		try {
			barrier.await();
		} catch (InterruptedException | BrokenBarrierException e1) {
			e1.printStackTrace();
		}

		return indeces;
	}

	@Override
	// intoarce o lista din tabela tableName cu atatea coloane cate elemente sunt in
	// operations.
	// elementele intoarse depind de contidie, de tipul operatiei si de elementele
	// din tabela
	public ArrayList<ArrayList<Object>> select(String tableName, String[] operations, String condition) {
		// verificam daca o tranzactie este in desfasurare si daca da lasam sa intre
		// doar threadul care a inceput-o
		if (tables.get(tableName).getLock().isLocked() && !tables.get(tableName).getLock().isHeldByCurrentThread()) {
			while (true) {
				if (!tables.get(tableName).getLock().isLocked()) {
					break;
				}
			}
		}

		Table t = tables.get(tableName);
		// ne asiguram ca nu se mai scrie in tabela cat timp se citeste
		try {
			t.getMutexR().acquire();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		t.setNr(t.getNr() + 1);
		if(t.getNr() == 1) {
			try {
				t.getRw().acquire();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		t.getMutexR().release();
		ArrayList<ArrayList<Object>> sol = new ArrayList<ArrayList<Object>>();
		boolean condVid = false;
		String[] tokens = null;
		String col = "";
		int pos = 0;
		Column colToTest = new Column();
		// determinam daca conditia este vida
		if (condition == "") {
			condVid = true;
		} else {
			tokens = condition.split(" ");
			col = tokens[0];
			// extragem coloana din conditie
			for (int i = 0; i < t.getColumns().size(); i++) {
				if (t.getColumns().get(i).getName().equals(col)) {
					colToTest = t.getColumns().get(i);
					break;
				}
			}
		}

		// pentru fiecare coloana din operations
		for (int i = 0; i < operations.length; i++) {
			ArrayList<Object> list = new ArrayList<>();
			String name;
			String op = "";
			// determinam numele coloanei din operatie ale carei elemente trebuiesc obtinute
			// si operatia care se aplica asupra lor
			if (operations[i].contains("sum")) {
				name = operations[i].substring(4, operations[i].length() - 1);
				op = "sum";
			} else if (operations[i].contains("min")) {
				op = "min";
				name = operations[i].substring(4, operations[i].length() - 1);
			} else if (operations[i].contains("max")) {
				op = "max";
				name = operations[i].substring(4, operations[i].length() - 1);
			} else if (operations[i].contains("avg")) {
				op = "avg";
				name = operations[i].substring(4, operations[i].length() - 1);
			} else if (operations[i].contains("count")) {
				op = "count";
				name = operations[i].substring(6, operations[i].length() - 1);
			} else {
				name = operations[i];
			}
			// determinam pozitia coloanei
			for (int k = 0; k < t.getColumns().size(); k++) {
				if (t.getColumns().get(k).getName().equals(name)) {
					pos = k;
					break;
				}
			}

			ArrayList<ArrayList<Integer>> indeces;
			// listele cu indicilor liniilor pentru care se respecta conditia
			if (!condVid) {
				indeces = getIndeces(t, tokens[2], colToTest, tokens[1], condVid);
			} else {
				indeces = getIndeces(t, "", colToTest, "", condVid);
			}

			// reunim toate elementele de pe pozitiile corespunzatoare din coloana dorita in
			// operations
			for (int j = 0; j < indeces.size(); j++) {
				for (int k = 0; k < indeces.get(j).size(); k++) {
					list.add(t.getColumns().get(pos).getValues().get(indeces.get(j).get(k)));
				}
			}

			// efectuam operatia asupra elementelor
			switch (op) {
			case "sum":
				int sum = 0;
				for (int e = 0; e < list.size(); e++) {
					sum += (int) list.get(e);
				}
				ArrayList<Object> sumA = new ArrayList<>();
				sumA.add(sum);
				sol.add(sumA);
				break;
			case "min":
				int min = Integer.MAX_VALUE;
				for (int e = 0; e < list.size(); e++) {
					if (min > (int) list.get(e))
						min = (int) list.get(e);
				}
				ArrayList<Object> sumMin = new ArrayList<>();
				sumMin.add(min);
				sol.add(sumMin);
				break;
			case "max":
				int max = Integer.MIN_VALUE;
				for (int e = 0; e < list.size(); e++) {
					if (max < (int) list.get(e))
						max = (int) list.get(e);
				}
				ArrayList<Object> sumMax = new ArrayList<>();
				sumMax.add(max);
				sol.add(sumMax);
				break;
			case "count":
				int c = list.size();
				ArrayList<Object> sumC = new ArrayList<>();
				sumC.add(c);
				sol.add(sumC);
				break;
			case "avg":
				int count = 0;
				int sumAvg = 0;
				for (int e = 0; e < list.size(); e++) {
					sumAvg += (int) list.get(e);
					count++;
				}
				ArrayList<Object> sumAvgV = new ArrayList<>();
				sumAvgV.add((float) sumAvg / count);
				sol.add(sumAvgV);
				break;
			default:
				sol.add(list);
			}
		}
		// permitem din nou operatii de scriere cand nu mai sunt cititori doritori
		try {
			t.getMutexR().acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		t.setNr(t.getNr() - 1);
		if(t.getNr() == 0) {
			t.getRw().release();
		}
		t.getMutexR().release();
		return sol;
	}

	@Override
	// scrie in tableName valorile din values daca condition e true
	public void update(String tableName, ArrayList<Object> values, String condition) {
		// verificam daca o tranzactie este in desfasurare si daca da lasam sa intre
		// doar threadul care a inceput-o
		if (tables.get(tableName).getLock().isLocked() && !tables.get(tableName).getLock().isHeldByCurrentThread()) {
			while (true) {
				if (!tables.get(tableName).getLock().isLocked()) {
					break;
				}
			}
		}
		Table t = tables.get(tableName);
		try {
			t.getRw().acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		Column colToTest = new Column();
		String[] tokens = null;
		
		//determinam daca conditia este vida
		boolean condVid = false;
		if (condition == "") {
			condVid = true;
		} else {
			tokens = condition.split(" ");
			for (Column c : t.getColumns()) {
				if (c.getName().equals(tokens[0])) {
					colToTest = c;
					break;
				}
			}
		}
		//listele cu indicii liniilor care respecta conditia
		ArrayList<ArrayList<Integer>> indeces;
		if (!condVid) {
			indeces = getIndeces(t, tokens[2], colToTest, tokens[1], condVid);
		} else {
			indeces = getIndeces(t, "", colToTest, "", condVid);
		}
		ArrayList<Integer> list = new ArrayList<>();

		//punem impreuna cele numWorkers liste de indici
		for (int j = 0; j < indeces.size(); j++) {
			for (int k = 0; k < indeces.get(j).size(); k++) {
				list.add(indeces.get(j).get(k));
			}
		}
		
		//refacem tabela, completand cu valorile din values liniile corespunzatoare din tabela
		for (int j = 0; j < list.size(); j++) {
			for (int k = 0; k < values.size(); k++) {
				t.getColumns().get(k).getValues().set(list.get(j), values.get(k));
			}
		}
		//permitem si alte operatii pe tabela
		t.getRw().release();
	}

	@Override
	// o noua linie la sfarsitul tabelei cu valorile values
	public void insert(String tableName, ArrayList<Object> values) {
		// verificam daca o tranzactie este in desfasurare si daca da lasam sa intre
		// doar threadul care a inceput-o
		if (tables.get(tableName).getLock().isLocked() && !tables.get(tableName).getLock().isHeldByCurrentThread()) {
			while (true) {
				if (!tables.get(tableName).getLock().isLocked()) {
					break;
				}
			}
		}
		Table t = tables.get(tableName);
		// nu mai permitem alte operatii de scriere/ citire pe tabela curenta
		try {
			t.getRw().acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		//pentru fiecare valoare din values
		for (int i = 0; i < values.size(); i++) {
			//verificam ca tipul valorii introduse sa corespunda cu tipul coloanei
			Class<? extends Object> type = values.get(i).getClass();
			String s = type.getName().toLowerCase();
			s = s.substring(10);
			if (s.contains(t.getColumns().get(i).getType())) {
				t.getColumns().get(i).addValue(values.get(i));
			} else {
				throw new RuntimeException();
			}

		}
		//permitem alte operatii pe tabela
		t.getRw().release();
	}

	@Override
	// incepe o tranzactie. operatiile din interior se executa atomic (ca o singura
	// operatie)
	public void startTransaction(String tableName) {
		tables.get(tableName).getLock().lock();
	}

	@Override
	// finalizeaza tranzactia
	// eliberare lock
	public void endTransaction(String tableName) {
		tables.get(tableName).getLock().unlock();
	}

}
