Dumitrache Daniela Andreea
331CB
Algoritmi Paraleli si Distribuiti - Tema2

In implementarea temei am folosit clasele Table si Column pentru a construi tabelele.
O coloana este caracterizata de nume, tip de date si un ArrayList cu valorile retinute
in coloana respectiva. Un tabel este caracterizat de nume, de o lista de coloane, iar
pentru sincronizari fiecare tabel va contine un ReentrantLock (folosite in timpul 
tranzactiilor), precum si doua semafoare binare si un intreg, reprezentand numarul de
cititori (pentru a implementa sincronizarea citiori-scriitori conform metodei descrisa
in curs): 

Clasa Database implementeaza interfata MyDatabase si contine un hashmap pentru a face
mapari intre tabele si numele acestora, precum si un ExecutorService, pentru paralelizarea
operatiilor Update si Select, si numarul de workeri maximi pentru acestea (numWorkerThreads).
Am considerat ca oricati clienti poti face cereri in paralele pe tabele diferite, numWorkerTreads
fiind maximul de workeri ce lucreaza in paralele pentru functiile update si select.

Pentru implementarea tranzactiilor:
- startTransaction: se apeleaza lock pe instanta de ReentrantLock a tabelei corespunzatoare
- endTransaction: se apeleaza unlock pe lockul tabelei.
La inceputul functiilor insert, update si select verfic daca lock-ul tabelei este setat, iar
daca da las sa treaca thread-ul curent doar daca detine lockul. Altfel, threadul curent va astepta
pana se elibereaza lock-ul (se da endTransaction); Daca lock-ul nu este detinut de niciun thread,
atunci threadul curent poate continua executia programului in mod normal.

Sincronizarea folosind citiori-scriitori:
Fiecare tabela contine:
- semaforul binar mutexR, folosit pentru a incrementa/ decrementa corect numarul de cititori
- nr, numarul de cititori
- semaforul binar rw, folosit la comun de cititori si scriitori pentru a nu intra unii peste ceilalti
Fuctionare:
- functia Select este singura care realizeaza citiri. Astfel, la intrarea in functie se 
incrementeaza numarul de cititori. Daca procesul curent este primul citior, atunci se va
face acquire pe semaforul comun, pentru a nu permite scriitorilor sa modifice tabela.
La final se decrementeaza numarul de cititori si cand se ajunge la ultimul cititor
se da release semaforului comun, permitand noi operatii de scriere.
- functiile update si insert sunt cele care realizeaza scrieri. La intrarea in functie
se face acquire pe semaforul comun de read/write, blocand accesul altor cititori/ scriitori.
La iesirea din functie se da release, permitand noi operatii.

Insert:
Pentru realizarea operatiei de insert se parcurge vectorul values, se verifica daca elementul
curent din values are acelasi tip cu al coloanei de pe pozitia corespunzatoare, iar daca 
da, se va insera elementul in coloana. In caz ca apar nepotriviri de tip se atunca o exceptie
de Runtime;

GetIndeces. 
Folosita de update si Select. Aceasta returneaza o lista cu numWorkerTreads
componente, ce contin indicii liniilor pentru care conditia primita ca parametru
de functia select/update este respectata. Pentru a paraleliza procesul de cautare a indicilor
am impartit tabela in numWorkerTreads parti. Folosind executorService voi da submit la 
numWorkerThreads task-uri, care vor calcula indicii intre o valoare si start si o valoare
de stop in felul urmator:
- conditie vida: toti indicii intre start si stop sunt pusi in lista rezultat, primita
de Task prin constructor.
- conditie nevida: conditia va avea forma: coloanaTest operator valoareDeTest; determin tipul
valorii de test, apoi, in functie de operator, voi testa daca valoarea curenta din
coloana de test respecta conditia. Daca da, indicele liniei pe care se afla valoarea se va adauga
in lista primita de Task prin constructor. Fiecare worker va testa conditia pe pozitiile dintre valoarea
de start si cea de stop, primite tot prin constructor.
In cazul in care nu ar fi  primita o valoare de tipul potrivit s-ar arunca exceptii.
Pentru a ma asigura ca indicii sunt dati in ordinea buna folosesc o lista de liste, lista de pe pozitia
i fiind data workerului i spre completare. De asemenea, folosesc o bariera initializata
la numWorkerThreads + 1. Cand un worker ajunge la finanul run-ului sau va astepta la bariera pana 
cand ceilalti workeri isi termina executia. De asemenea, pentru a nu isi continua executia 
dupa ce s-a dat submit la taskuri threadul principal asteapta si el la bariera.
Astefel, cand se va trce mai departe, toate listele vor contine indicii calculati de fiecare
worker. Functia va returna lista de liste ale care componente au fost completate de workeri.
Functia este synchronized pentru a nu fi apelata de mai multe threaduri si a avea
astfel rezultate eronate / deadlocks in cazul in care 2 thread-uri ar apela executorul.

Select:
	Pentru fiecare membru din operations se determina coloana dorita si operatia
(in caz ca exista) ce se va executa asupra elementelor din coloana dorita de pe
pozitiile care respecta condition. Dupa care se apeleaza functia getIndeces pentru
a afla pozitiile acestea. Se construieste o lista cu toate elementele de pe
coloana din operations de pe pozitiile returnate de functia getIndeces si se aplica
,daca este necesar, operatia ceruta pe elementele respective. Rezultatul este introdus
in lista finala.

Update:
Se determina pozitia liniilor ce se vor inlocui folosind getIndeces. Apoi voi pune
impreuna rezultate din cele numWorkerThreads liste returnate de getIndeces intr-o singura
lista  de indici. Dupa aceasta, pentru fiecare pozitie, voi schimba valorile de pe
linia respectiva cu valorile din values.

Am testat tema pe clusterul facultatii folosind coada campus-haswell.q si valorile initiale
din teste. O rulare cu toate cele  teste (sanitiyCheck, scalabilitate si consistenta) a durat
15-20 de minute, iar o rulare cu teste de sanityCheck si scalabilitate a durat 2-5 minute.
Valori obtinute:

There are now 1 Threads
[[532894452]]
Insert time 33258
Update time 14676 
Select time 2914 
There are now 2 Threads
[[532894452]]
Insert time 23079 
Update time 1632 
Select time 1982 
There are now 4 Threads
[[532894452]]
Insert time 20415 
Update time 1945 
Select time 1385 

There are now 1 Threads
[[532894452]]
Insert time 33501
Update time 901
Select time 11865
There are now 2 Threads
[[532894452]]
Insert time 17591
Update time 1981
Select time 2084
There are now 4 Threads
[[532894452]]
Insert time 13027
Update time 2701
Select time 1400

[[532894452]]
Insert time 32439
Update time 728
Select time 12514
There are now 2 Threads
[[532894452]]
Insert time 24268
Update time 1035
Select time 2231
There are now 4 Threads
Insert time 23719
Update time 1979
[[532894452]]
Select time 1779

There are now 1 Threads
[[532894452]]
Insert time 42441
Update time 686
Select time 3040
There are now 2 Threads
[[532894452]]
Insert time 11792
Update time 1032
Select time 1953
There are now 4 Threads
[[532894452]]
Insert time 9334
Update time 2451
Select time 1575

Se poate observa ca operatiile de insert si de select scaleaza, insa nu si cea de update.
In schimb, la rularea locala cu 1.000.000 de insert-uri in testul de scalabilitate se
poate observa si scalarea functiei update, precum si a functiilor Select si Insert. Insa
nici local functia update nu scaleaza mereu:

There are now 1 Threads
Insert time 8386
Update time 489
Select time 702
There are now 2 Threads
Insert time 7664
Update time 185
Select time 236
[[-1456759948]]
There are now 4 Threads
Insert time 4338
[[-1456759948]]
Update time 67
Select time 213

There are now 1 Threads
Insert time 7597
Update time 141
Select time 630
[[-1456759948]]
There are now 2 Threads
Insert time 4673
Update time 121
Select time 257
[[-1456759948]]
There are now 4 Threads
Insert time 4012
Update time 67
Select time 233
[[-1456759948]]

