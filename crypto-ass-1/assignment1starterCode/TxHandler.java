import java.util.LinkedList;

import java.util.Iterator;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class TxHandler {

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    private UTXOPool upool;

    public TxHandler(UTXOPool utxoPool) {
        upool = new UTXOPool(utxoPool);
    }

	
	private boolean allClaimedInUTXO(Transaction tx){
		//1. all o/ps in tx are in upool
		ArrayList<Transaction.Input> inputs = tx.getInputs();
		
		for(Transaction.Input ip : tx.getInputs()){
			byte[] prevHash = ip.prevTxHash;
			int opIndex = ip.outputIndex;
			UTXO utxo = new UTXO(prevHash, opIndex);

			if(!upool.contains(utxo)){
				return false;
			}
		}
		return true;
	}
	
	private boolean allSignaturesValid(Transaction tx){
		//2. all signatures on each input of tx are valid
		int i=0;
		for(Transaction.Input ip : tx.getInputs()){
			byte[] rawTx = tx.getRawDataToSign(i++);
			byte[] sig = ip.signature;
			byte[] prevHash = ip.prevTxHash;
			int opIndex = ip.outputIndex;
			UTXO utxo = new UTXO(prevHash, opIndex);
			
			Transaction.Output op = upool.getTxOutput(utxo);
			PublicKey pubkey = op.address;

			if(!Crypto.verifySignature(pubkey, rawTx, sig)){
				return false;
			}
		}
		return true;
	}

	private boolean noMultipleClaim(Transaction tx){
		//3. No utxo is claimed multiple times
		HashMap<UTXO, Boolean> hm = new HashMap<UTXO, Boolean>();
		for(Transaction.Input ip : tx.getInputs()){
			byte[] prevHash = ip.prevTxHash;
			int opIndex = ip.outputIndex;
			UTXO utxo = new UTXO(prevHash, opIndex);
			Boolean b = hm.get(utxo);
			if(b != null && b){
				return false;
			}
		
			hm.put(utxo, true);
		}
		return true;
	}

	private boolean allOutputNonNegative(Transaction tx){
		//4. all o/p values are non negative
		for(Transaction.Output op : tx.getOutputs()){
			if(op.value < 0){
				return false;
			}
		}  
		return true;
	}

	private boolean ipSufficientForOutput(Transaction tx){
		//5. sum of i/p values exceed sum of o/p values not strictly
		double ip_sum = 0;
		for(Transaction.Input ip : tx.getInputs()){
			byte[] prevHash = ip.prevTxHash;
			int opIndex = ip.outputIndex;
			UTXO utxo = new UTXO(prevHash, opIndex);
		
			Transaction.Output op = upool.getTxOutput(utxo);
			ip_sum += op.value;

		}

		double op_sum = 0;
		for(Transaction.Output op : tx.getOutputs()){
			op_sum += op.value;
		}
	 
		if(ip_sum < op_sum){
			return false;
		}
		
		return true;
	}
	
    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
    
		if(!allClaimedInUTXO(tx)){
			return false;
		}

		if(!allSignaturesValid(tx)){
			return false;
		}
	
		if(!noMultipleClaim(tx)){
			return false;
		}
		
		if(!allOutputNonNegative(tx)){
			return false;
		}	

		if(!ipSufficientForOutput(tx)){
			return false;
		}
			
		return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
   		//1. Loop through possibleTxs
		//2. Check if the current trx is valid wrt current upool
		//3. If yes, update upool 
		//4. Loop again from beginning
		//5. If no valid trx found in a loop, return the set
		LinkedList<Transaction> txList = new LinkedList<Transaction>(Arrays.asList(possibleTxs));

		ArrayList<Transaction> handledTxs = new ArrayList<Transaction>();
		Transaction tx;
		for(Iterator<Transaction> iter = txList.iterator(); iter.hasNext();){
			tx = iter.next();
			if(!isValidTx(tx))
				continue;

			//if tx is valid, update upool
			//1. Consume coins refered to by tx.inputs
			for(Transaction.Input ip : tx.getInputs()){
				byte[] prevTxHash = ip.prevTxHash;
				int outputIndex = ip.outputIndex;
				UTXO utxo = new UTXO(prevTxHash, outputIndex);
				upool.removeUTXO(utxo);		
			}	
			//2. Add coins refered to by tx.outputs to upool
			byte[] txHash = tx.getHash();
			ArrayList<Transaction.Output> outputs = tx.getOutputs();
			for(int i=0; i<outputs.size(); ++i){
				UTXO utxo = new UTXO(txHash, i);
				upool.addUTXO(utxo, outputs.get(i));
			}

			//add tx to handledTxs
			handledTxs.add(tx);

			//remove tx from possibleTxs
			//start looping again;
			iter.remove();
			iter = txList.iterator();
		}
		
		return handledTxs.toArray(new Transaction[handledTxs.size()]);
	
	}

}



