import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/* CompliantNode refers to a node that follows the rules (not malicious)*/
public class CompliantNode implements Node {
	double p_graph;
	double p_malicious;
	double p_txDistribution;
	int numRounds;
	boolean[] followees;
	HashSet<Transaction> pendingTransactions;
	HashSet<Transaction> believedTransactions;
	HashMap<Transaction, Integer> currentTransactions;
	int cur_round = 0;
	
	int roundsCompleted = 0;

    public CompliantNode(double p_graph, double p_malicious, double p_txDistribution, int numRounds) {    	
        this.p_graph = p_graph;
        this.p_malicious = p_malicious;
        this.p_txDistribution = p_txDistribution;
        this.numRounds = numRounds;
        this.believedTransactions = new HashSet<Transaction>();
        this.currentTransactions = new HashMap<Transaction, Integer>();
    }

    public void setFollowees(boolean[] followees) {
    	this.followees = followees;
    }

    public void setPendingTransaction(Set<Transaction> pendingTransactions) {
        this.pendingTransactions = (HashSet<Transaction>)pendingTransactions;
    }

    public Set<Transaction> sendToFollowers() {
    	if(believedTransactions.size()==0) {
    		believedTransactions = pendingTransactions;
    		for(Transaction tx: pendingTransactions) {
    			currentTransactions.put(tx, 1);
    		}
      		cur_round++;
    		return believedTransactions;
    	}
    	
    	double p_malFollowee = (p_graph*p_malicious);
    	int num_followees = followees.length;
    	
    	//If not believed already, min number of followees who have to believe to convince me
    	long min_to_believe = Math.round(Math.ceil(p_malFollowee * num_followees));
    	
    	if(cur_round >= numRounds){
    		return believedTransactions;
    	}
    	cur_round++;

    	for(Map.Entry<Transaction, Integer> entry : currentTransactions.entrySet()) {
    		if(entry.getValue() >= min_to_believe) {
    			believedTransactions.add(entry.getKey());
    		}
    	}
    		
    	return believedTransactions;
    }

    public void receiveFromFollowees(Set<Candidate> candidates) {
    	for(Candidate c: candidates) {
    		if(currentTransactions.containsKey(c.tx)) {
    			int currentCount = currentTransactions.get(c.tx);
    			int newCount = currentCount + 1;
    			currentTransactions.replace(c.tx, newCount);
    		}else {
    			currentTransactions.put(c.tx, 1);
    		}  		
    	}
    }
}
