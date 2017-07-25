import java.util.HashSet;
import java.util.Set;

public class TxHandler {

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    private UTXOPool utxoPool;

    public TxHandler(UTXOPool utxoPool) {
        this.utxoPool = new UTXOPool(utxoPool);
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
        int inSum=0;
        int outSum=0;
        UTXOPool uniqueUTXO = new UTXOPool();

        for(int i=0;i<tx.numInputs();i++){
            Transaction.Input input = tx.getInput(i);

            UTXO tempUtxo = new UTXO(input.prevTxHash,input.outputIndex);

            Transaction.Output output= utxoPool.getTxOutput(tempUtxo);

            if(!utxoPool.contains(tempUtxo))
                return false;
            if(!Crypto.verifySignature(output.address,tx.getRawDataToSign(i),input.signature))
                return false;
            if(uniqueUTXO.contains(tempUtxo))
                return false;
            uniqueUTXO.addUTXO(tempUtxo,output);
            inSum+=output.value;
        }

        for(Transaction.Output output:tx.getOutputs()){
            if(output.value<0)
                return false;
            outSum+=output.value;
        }

        if(outSum>inSum)
            return false;
        else
            return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        Set<Transaction> validTransactions = new HashSet<>();

        for(Transaction transaction: possibleTxs) {
            if (isValidTx(transaction)) {
                validTransactions.add(transaction);
                for (Transaction.Input in : transaction.getInputs()) {
                    UTXO utxo = new UTXO(in.prevTxHash, in.outputIndex);
                    utxoPool.removeUTXO(utxo);
                }
                for (int i = 0; i < transaction.numOutputs(); i++) {
                    Transaction.Output out = transaction.getOutput(i);
                    UTXO utxo = new UTXO(transaction.getHash(), i);
                    utxoPool.addUTXO(utxo, out);
                }
            }
        }
        Transaction[] validTxArray = new Transaction[validTransactions.size()];
        return validTransactions.toArray(validTxArray);
    }

}
