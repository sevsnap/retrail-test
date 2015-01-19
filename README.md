# retrail-test
# ITALIANO 

Retrail si compone di 4 progetti sviluppati su Netbeans 8, i cui repository sono:

https://github.com/kicco73/retrail-commons.git (librerie comuni client server)
https://github.com/kicco73/retrail-client.git (implementazione PEP client generica)
https://github.com/kicco73/retrail-server.git (UCon + PDP server side)
https://github.com/kicco73/retrail-test.git  (contiene il prototype con l'esempio di politiche e le unit test)

INSTALLAZIONE

I progetti indicati sopra vanno scaricati tutti. 
Un modo comodo per scaricarli è usare, per ciascun repo, l'opzione Tools->Git->Clone da NetBeans.
Una volta scaricati i progetti, su Unix è opportuno installare bash-maven-plugin perché è usato nel build e non ha un 
maven repo. Il plugin serve a eliminare residui del precedente eventuale run del progetto (il server viene ucciso,
il db persistente viene cancellato).
In una directory qualunque da bash digitare:

    git clone git@bitbucket.org:atlassian/bash-maven-plugin.git
    cd bash-maven-plugin
    mvn clean install

Si può poi cancellare la directory che non serve piu'. (In caso di problemi si veda: https://bitbucket.org/atlassian/bash-maven-plugin).
Alternativamente, si può rinunciare al plugin eliminando dal pom.xml la sezione <plugin> relativa a bash-maven-plugin.
Occorre anche modificare nbactions.xml ed eliminare tutti i riferimenti a bash-maven-plugin.

Quindi per ciascun progetto (nell'ordine di cui sopra) occorre fare da netbeans:

- tasto destro -> Clean and Build

Queste operazioni riempiono la copia cache del repository maven locale con i pacchetti necessari per far girare retrail-test. 
