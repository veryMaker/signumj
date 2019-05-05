package burst.kit.entity.response;

import burst.kit.entity.BurstAddress;
import burst.kit.entity.BurstID;
import burst.kit.entity.BurstTimestamp;
import burst.kit.entity.BurstValue;
import burst.kit.entity.response.http.BlockResponse;

import java.math.BigInteger;

public class Block {
    private final BigInteger nonce;
    private final BurstAddress generator;
    private final BurstID id;
    private final BurstID nextBlock;
    private final BurstID previousBlock;
    private final BurstID[] transactions;
    private final BurstTimestamp timestamp;
    private final BurstValue blockReward;
    private final BurstValue totalAmount;
    private final BurstValue totalFee;
    private final byte[] generationSignature;
    private final byte[] generatorPublicKey;
    private final byte[] payloadHash;
    private final byte[] previousBlockHash;
    private final byte[] signature;
    private final int height;
    private final int payloadLength;
    private final int scoopNum;
    private final int version;
    private final long baseTarget;

    public Block(BigInteger nonce, BurstAddress generator, BurstID id, BurstID nextBlock, BurstID previousBlock, BurstID[] transactions, BurstTimestamp timestamp, BurstValue blockReward, BurstValue totalAmount, BurstValue totalFee, byte[] generationSignature, byte[] generatorPublicKey, byte[] payloadHash, byte[] previousBlockHash, byte[] signature, int height, int payloadLength, int scoopNum, int version, long baseTarget) {
        this.nonce = nonce;
        this.generator = generator;
        this.id = id;
        this.nextBlock = nextBlock;
        this.previousBlock = previousBlock;
        this.transactions = transactions;
        this.timestamp = timestamp;
        this.blockReward = blockReward;
        this.totalAmount = totalAmount;
        this.totalFee = totalFee;
        this.generationSignature = generationSignature;
        this.generatorPublicKey = generatorPublicKey;
        this.payloadHash = payloadHash;
        this.previousBlockHash = previousBlockHash;
        this.signature = signature;
        this.height = height;
        this.payloadLength = payloadLength;
        this.scoopNum = scoopNum;
        this.version = version;
        this.baseTarget = baseTarget;
    }

    public Block(BlockResponse blockResponse) {
        this.nonce = new BigInteger(blockResponse.getNonce());
        this.generator = blockResponse.getGenerator();
        this.id = blockResponse.getBlock();
        this.nextBlock = blockResponse.getNextBlock();
        this.previousBlock = blockResponse.getPreviousBlock();
        this.transactions = blockResponse.getTransactions();
        this.timestamp = blockResponse.getTimestamp();
        this.blockReward = blockResponse.getBlockReward();
        this.totalAmount = blockResponse.getTotalAmountNQT();
        this.totalFee = blockResponse.getTotalFeeNQT();
        this.generationSignature = blockResponse.getGenerationSignature().getBytes();
        this.generatorPublicKey = blockResponse.getGeneratorPublicKey().getBytes();
        this.payloadHash = blockResponse.getPayloadHash().getBytes();
        this.previousBlockHash = blockResponse.getPreviousBlockHash().getBytes();
        this.signature = blockResponse.getBlockSignature().getBytes();
        this.height = blockResponse.getHeight();
        this.payloadLength = blockResponse.getPayloadLength();
        this.scoopNum = blockResponse.getScoopNum();
        this.version = blockResponse.getVersion();
        this.baseTarget = blockResponse.getBaseTarget();
    }

    public BigInteger getNonce() {
        return nonce;
    }

    public BurstAddress getGenerator() {
        return generator;
    }

    public BurstID getId() {
        return id;
    }

    public BurstID getNextBlock() {
        return nextBlock;
    }

    public BurstID getPreviousBlock() {
        return previousBlock;
    }

    public BurstID[] getTransactions() {
        return transactions;
    }

    public BurstTimestamp getTimestamp() {
        return timestamp;
    }

    public BurstValue getBlockReward() {
        return blockReward;
    }

    public BurstValue getTotalAmount() {
        return totalAmount;
    }

    public BurstValue getTotalFee() {
        return totalFee;
    }

    public byte[] getGenerationSignature() {
        return generationSignature;
    }

    public byte[] getGeneratorPublicKey() {
        return generatorPublicKey;
    }

    public byte[] getPayloadHash() {
        return payloadHash;
    }

    public byte[] getPreviousBlockHash() {
        return previousBlockHash;
    }

    public byte[] getSignature() {
        return signature;
    }

    public int getHeight() {
        return height;
    }

    public int getPayloadLength() {
        return payloadLength;
    }

    public int getScoopNum() {
        return scoopNum;
    }

    public int getVersion() {
        return version;
    }

    public long getBaseTarget() {
        return baseTarget;
    }
}
