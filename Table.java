import java.util.ArrayList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

public class Table {
	private String name;
	private ArrayList<Column> columns;
	private ReentrantLock lock;
	private int nr;
	private Semaphore mutexR;
	private Semaphore rw;
	
	public Table() {
		columns = new ArrayList<Column>();
	}
	
	public Table(String name) {
		this.name = name;
		this.columns = new ArrayList<Column>();
		lock = new ReentrantLock();
		mutexR = new Semaphore(1);
		setRw(new Semaphore(1));
		nr = 0;
	}
	
	public String getName() {
		return name;
	}
	
	
	public void setName(String name) {
		this.name = name;
	}
	public ArrayList<Column> getColumns() {
		return columns;
	}
	public void setColumns(ArrayList<Column> columns) {
		this.columns = columns;
	}
	
	public void addColumn(Column c) {
		columns.add(c);
	}
	
	public ReentrantLock getLock() {
		return lock;
	}

	public int getNr() {
		return nr;
	}

	public void setNr(int nr) {
		this.nr = nr;
	}

	public Semaphore getMutexR() {
		return mutexR;
	}

	public void setMutexR(Semaphore mutexR) {
		this.mutexR = mutexR;
	}

	public Semaphore getRw() {
		return rw;
	}

	public void setRw(Semaphore rw) {
		this.rw = rw;
	}

}
