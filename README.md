Repository contenente il progetto del corso Distributed and Pervasive Systems erogato dall'Università degli Studi di Milano nell'anno accademico 2024/25. 
- Java
- MQTT
- gRPC
- REST

Il progetto consiste nella progettazione ed implementazione di DESM (Distributed Energy Supply Management), una simulazione semplificata di una rete di fornitura energetica gestita da un fornitore di energia rinnovabile. 

Il fornitore di energia rinnovabile genera energia da fonti rinnovabili come l’energia eolica, idroelettrica e solare. Purtroppo, l’energia prodotta non è sempre sufficiente a soddisfare la domanda totale. Per far fronte a questa carenza, il fornitore di energia rinnovabile si affida ad una rete di centrali termoelettriche per fornire l’energia aggiuntiva richiesta. Quando viene ricevuta una nuova richiesta di produzione di energia, le centrali termoelettriche della rete competono per soddisfarla. L’assegnazione della richiesta si basa sul prezzo più basso tra le centrali concorrenti. La rete di centrali termoelettriche è gestita da un server di amministrazione, che gestisce la registrazione dei nuovi impianti che aderiscono al sistema. Inoltre, ogni centrale termoelettrica è dotata di sensori che monitorano le emissioni di inquinanti durante la produzione di energia; questi dati ambientali vengono trasmessi periodicamente al server di amministrazione, che può essere interrogato dal client di amministrazione per recuperare statistiche. 

> **L’obiettivo di questo progetto è implementare il fornitore di energia rinnovabile, una rete peer-to-peer di centrali termoelettriche, il server di amministrazione e il client di amministrazione.**
> 

# 2. Operazioni generali

La seguente sezione descrive le operazioni generali del sistema. Le sezioni successive forniranno una descrizione dettagliata per ogni specifico componente. 

## 2.1 - Pubblicazione della richiesta di energia

A intervalli regolari di 10 secondi, il fornitore di energia rinnovabile pubblica una richiesta di una determinata quantità di energia, misurata in kWh. Questa richiesta viene trasmessa a tutte le centrali termoelettriche della rete. 

## 2.2 - Fase di offerta

Dopo aver ricevuto la richiesta di energia, ogni centrale termoelettrica avvia un processo di selezione per determinare quale impianto soddisferà la richiesta. Durante questa fase, ogni centrale termoelettrica genera un’offerta che rappresenta il costo per soddisfare la richiesta. Viene selezionato l’impianto che offre il costo più basso. 

## 2.3 - Richiesta di evasione

Una volta che una centrale termoelettrica si aggiudica la gara, procede a soddisfare la richiesta di energia. L’impianto impiega un tempo pari ad 1 millisecondo moltiplicato per il numero di kWh richiesti per completare la produzione di energia. Durante questo periodo, l’impianto non può accettare nuove richieste di produzione di energia.

## 2.4 - Aggiunta di nuove centrali elettriche

Il sistema consente l’aggiunta dinamica di nuove centrali termoelettriche alla rete in qualsiasi momento. Pertanto, tutti gli algoritmi e i processi devono tenere conto di questa possibilità. Ad esempio, una nuova centrale termoelettrica può entrare a far parte della rete durante una campagna elettorale e deve poter partecipare alla fase di gara. 

## 2.5 - Sensori di inquinamento

Una volta che una centrale termoelettrica si unisce alla rete, inizia ad acquisire dati dal suo sensore di inquinamento, che monitora le emissioni durante la produzione di energia. Queste letture vengono trasmesse periodicamente al server di amministrazione. Il server di amministrazione può essere interrogato da un client di amministrazione in modo che un’eventuale autorità competente possa monitorare i livelli di inquinamento e mitigare l’impatto ambientale. 

# 3. Applicazioni da implementare

Per questo progetto, è necessario sviluppare le seguenti applicazioni: 

- Fornitore di energia rinnovabile: il processo che simula il fornitore di energia rinnovabile.
- Centrale termoelettrica: il processo che rappresenta le centrali termoelettriche nella rete DESM.
- Server di amministrazione: un server REST che aggiunge/rimuove dinamicamente centrali elettriche da DESM e consente al client di amministrazione di visualizzare le centrali attualmente attive nella rete e di elaborare statistiche sui livelli di inquinamento.
- Client di amministrazione: un client che consente di interrogare il server di amministrazione per ottenere informazioni sulle centrali termoelettriche attualmente attive nella rete e sulle relative emissioni.

Si noti che ogni centrale elettrica è un processo autonomo e, pertanto, non deve essere implementato come thread. 

Di seguito, forniamo maggiori dettagli sulle applicazioni che devono essere sviluppate.

# 4. Fornitore di energia rinnovabile

Il fornitore di energia rinnovabile genera una richiesta di energia ogni 10 secondi, specificando la quantità di energia richiesta in kWh e un timestamp. La quantità di energia è un numero generato casualmente tra 5000 e 15000. Questa richiesta viene quindi pubblicata su un topic MQTT, a cui sono iscritti tutti gli impianti termoelettrici della rete. La richiesta rimane sul topic finchè non viene selezionato un impianto per fornire l’energia richiesta. 

# 5. Centrale termoelettrica

Ogni centrale termoelettrica gestisce un processo che si occupa di: 

- Coordinarsi con le altre centrali utilizzando gRPC per decidere quale centrale soddisferà la richiesta di energia.
- Inviare al server informazioni sui valori rilevati dai sensori di inquinamento tramite MQTT.

## 5.1 - Inizializzazione

Una centrale termoelettrica viene inizializzata specificando: 

- ID
- Indirizzo e porta di ascolto per la comunicazione con gli altri impianti.
- Indirizzo e porta del server di amministrazione.

Si noti che, ai fini di questo progetto, l’indirizzo di tutti i componenti sarà sempre localhost, poichè tutto viene eseguito localmente sul computer.

Una volta avviato, il processo dell’impianto deve registrarsi nel sistema tramite il server di amministrazione. Se l’inserimento va a buon fine (ovvero, non ci sono altri impianti con lo stesso ID), l’impianto riceve dal server di amministrazione l’elenco degli altri impianti già presenti nella rete (ovvero, indirizzo e numero di porta di ciascun impianto).

Una volta ricevute queste informazioni, l’impianto inizia ad acquisire dati dal sensore di inquinamento. Se sono già presenti altri impianti nella rete, il nuovo impianto si presenta agli altri.

Infine, l’impianto si iscrive al topic MQTT in cui il fornitore di energia rinnovabile pubblicherà le richieste di produzione di energia. 

## 5.2 - Sincronizzazione distribuita

### 5.2.1 - Gestione delle richieste energetiche

Le centrali elettriche devono utilizzare un algoritmo distribuito e decentralizzato per decidere chi si occuperà di ciascuna richiesta di energia. Nello specifico, ogni richiesta sarà gestita dalla centrale che soddisfa i seguenti criteri: 

- La centrale elettrica non deve già gestire un’altra richiesta.
- La centrale elettrica è quella che offre il prezzo più basso.
- Nel caso in cui due o più centrali offrano lo stesso prezzo, vince la centrale con l’ID più alto.

### 5.2.2 - Generazione del prezzo

Ogni volta che una centrale termoelettrica deve partecipare ad un’elezione, utilizza un generatore casuale per simulare il calcolo del prezzo in $/kWh nell’intervallo [0.1, 0.9].

### 5.2.3 - Algoritmo di elezione

Poichè le centrali termoelettriche non conoscono in anticipo il prezzo offerto dalle altre centrali, è necessario implementare un algoritmo ad anello (Chang e Roberts). 

## 5.3 - Sensore di inquinamento

Le centrali elettriche sono dotate di un sensore di inquinamento che rileva la quantità di CO2 emessa durante il processo di produzione. La CO2 viene misurata in grammi (g). Durante il funzionamento del sistema, le centrali elettriche raccolgono informazioni sulle emissioni da inviare al server di amministrazione. Ogni sensore di inquinamento produce periodicamente misurazioni delle emissioni di CO2 dell’impianto. Ogni singola misurazione è caratterizzata da: 

- Valore di CO2 (g).
- Timestamp della misurazione, espresso in millisecondi.

La ganerazione di tali misurazioni viene effettuata da un simulatore. Per semplificare l’implementazione del progetto, è possibile scaricare il codice del simulatore direttamente dalla pagina del corso MyAriel, nella sezione progetto (LINK TBC). Ogni simulatore assegna il numero di millisecondi dopo la mezzanotte come timestamp associato ad una misurazione. Il codice del simulatore deve essere aggiunto come pacchetto al progetto e non deve essere modificato. Durante la fase di inizializzazione, ogni centrale avvia il thread del simulatore che genererà le misurazioni per il sensore di inquinamento. Ogni simulatore è un thread costruito da un ciclo infinito che genera periodicamente (a frequenza predefinita) le misurazioni simulate. Tali misurazioni vengono aggiunte ad una struttura dati appropriata. Forniamo solo l’interfaccia Buffer di questa struttura dati che espone due metodi: 

- `void add(Measurement m)`
- `List<Measurement> readAllAndClean()`

Pertanto, è necessario creare una classe che implementi questa interfaccia. Si noti che ogni centrale elettrica è dotata di un singolo sensore. Il thread di simulazione utilizza il metodo addMeasurement per riempire la struttura dati. Invece, è necessario utilizzare il metodo readAllAndClean per ottenere le misurazioni memorizzate nella strututra dati. Al termine di un’operazione di lettura, readAllAndClean crea spazio per nuove misurazioni nel buffer. Nello specifico, è necessario elaborare i dati dei sensori tramite la tecnica della finestra scorrevole introdotta nelle lezioni teoriche. Bisoga considerare un buffer di 8 misurazioni, con un fattore di sovrapposizione del 50%. Quando la dimensione del buffer è pari ad 8 misurazioni, è necessario calcolare la media di queste 8 misurazioni. L’impianto invierà quindi queste medie al server di amministrazione. 

## 5.4 - Inviare dati al server di amministrazione

Ogni 10 secondi, ogni centrale termoelettrica deve comunicare al server di amministrazione l’elenco delle medie delle misurazioni di CO2 calcolate dopo l’ultima comunicazione con il server. Questo elenco di medie deve essere inviato al server assieme a: 

- L’ID della centrale
- Il timestamp in cui è stato calcolato l’elenco.

Questa comunicazione avviene tramite un apposito topic MQTT a cui il server è iscritto.

## 5.5 - Lasciare la rete

Per semplicità, si assume che nessun impianto, dopo essere entrato nella rete, possa lasciarlo. 

# 6. Server di amministrazione

Il server di amministrazione raccoglie gli ID degli impianti registrati nel sistema e riceve da questi anche i valori dei sensori di inquinamento. Queste informazioni verranno poi interrogate dal client di amministrazione. Pertanto, questo server deve fornire diverse interfacce REST per: 

- Gestire l’inizializzazione delle centrali termoelettriche.
- Abilitare il client di amministrazione all’esecuzione delle query.

Inoltre, il server deve sottoscrivere il topic MQTT su cui le centrali termoelettriche pubblicano i valori dei sensori di inquinamento, leggerli e memorizzarli correttamente.

## 6.1 - Interfaccia REST della centrale termoelettrica

### 6.1.1 - Inizializzazione delle centrali elettriche

Il server deve memorizzare le seguenti informazioni per ogni impianto che si unisce alla rete: 

- ID
- Indirizzo (nel nostro caso, localhost)
- Numero di porta su cui è disponibile per gestire la comunicazione con gli altri processi dell’impianto.

Una centrale termoelettrica può essere aggiunta alla rete solo se non ci sono altri impianti con lo stesso ID. Se l’inserimento va a buon fine, il server di amministrazione restituisce alla centrale l’elenco degli impianti che si sono già uniti alla rete, includendo per ciascuno di essi l’ID, l’indirizzo e il numero di porta per la comunicazione. 

## 6.2 - Statistiche sull’inquinamento

Il server di amministrazione deve essere in grado di ricevere le statistiche locali sulla CO2 dalle centrali termoelettriche, il che significa che deve sottoscrivere un topic MQTT appropriato. Questi dati devono essere memorizzati in strutture dati appropriate che verranno utilizzate per eseguire analissi successive. Durante lo sviluppo del progetto, assicurarsi di sincronizzare correttamente le operazioni di lettura e scrittura eseguite su queste strutture dati. Infatti, il server di amministrazione può ricevere statistiche locali mentre il client di amministrazione lo richiede per eseguire calcoli su tali valori. 

## 6.3 - Interfaccia REST del client di amministrazione

Quando richiesto dal client di amministrazione tramite l’interfaccia descritta nella Sezione 7, il server di amministrazione deve essere in grado di calcolare le seguenti statistiche: 

- L’elenco delle centrali termoelettriche attualmente in rete.
- La media dei livelli di emissione di CO2 inviati da tutte le centrali al server tra il timestamp t1 e il timestamp t2.

# 7. Client di amministrazione

Il client di amministrazione è costituito da una semplice interfaccia a riga di comando che consente l’interazione con l’interfaccia REST fornita dal server di amministrazione. Pertanto, questa applicazione visualizza un semplice menu per selezionare uno dei servizi offerti dal server di amministrazione descritti nella sezione 6.3 e per inserire gli eventuali parametri richiesti. 

# 8. Semplificazioni e restrizioni

È importante ricordare che lo scopo di questo progetto è dimostrare la capacità di progettare e realizzare applicazioni distribuite e pervasive. Pertanto, tutti gli aspetti non strettamente correlati al protocollo di comunicazione, alla concorrenza e alla gestione dei dati sensoriali sono secondari. Inoltre, è possibile presumere che nessun nodo si comporti in modo dannoso. Al contrario, è necessario gestire eventuali errori nei dati inseriti dall'utente. Inoltre, il codice deve essere robusto: tutte le possibili eccezioni devono essere gestite correttamente. Sebbene le librerie Java forniscano diverse classi per la gestione della concorrenza, a fini didattici è obbligatorio utilizzare solo i metodi e le classi spiegati durante il corso di laboratorio. Pertanto, eventuali strutture dati di sincronizzazione necessarie (come lock, semafori o buffer condivisi) devono essere implementate da zero e saranno discusse durante la presentazione del progetto. Considerando la comunicazione tra i processi degli impianti, è necessario utilizzare il framework gRPC. Se sono richieste comunicazioni broadcast, queste devono essere eseguite in parallelo e non in sequenza.

# 9. Broker MQTT

Il broker MQTT su cui si basa DESM è Mosquitto e si presume che sia online al seguente indirizzo: `tcp://localhost:1883` . Il fornitore di energia rinnovabile utilizza questo broker per pubblicare la richiesta di energia. Il broker viene utilizzato anche dalla centrale termoelettrica per pubblicare le misurazioni dei sensori di inquinamento.
