package com.example.mustacheapp.util;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Hash;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGasPrice;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Numeric;

/**
 * Simple helper that sends a storeCommitment(title, password) transaction
 * to the PasswordCommitments smart contract using a single admin wallet.
 *
 * This is intentionally minimal and "demo-ready": it does not handle
 * advanced gas estimation, retries, etc.
 */
@Service
public class BlockchainService {

    private final Web3j web3j;
    private final Credentials credentials;
    private final String contractAddress;
    private final long chainId;
    private final String secretSalt;

    public static class CommitResult {
        private final String commitmentHex;
        private final String txHash;

        public CommitResult(String commitmentHex, String txHash) {
            this.commitmentHex = commitmentHex;
            this.txHash = txHash;
        }

        public String getCommitmentHex() {
            return commitmentHex;
        }

        public String getTxHash() {
            return txHash;
        }
    }

    public BlockchainService(
            @Value("${blockchain.rpcUrl}") String rpcUrl,
            @Value("${blockchain.privateKey}") String privateKey,
            @Value("${blockchain.contractAddress}") String contractAddress,
            @Value("${blockchain.chainId:11155111}") long chainId,
            @Value("${blockchain.secretSalt:EMPRESARIOS_DEMO_SECRET}") String secretSalt
    ) {
        this.web3j = Web3j.build(new HttpService(rpcUrl));
        this.credentials = Credentials.create(privateKey);
        this.contractAddress = contractAddress;
        this.chainId = chainId;
        this.secretSalt = secretSalt;
    }

    /**
     * Build a keccak256(bytes(password || '|' || secretSalt || '|' || title)) commitment,
     * send it to storeCommitment(commitment, label) and return the tx hash.
     *
     * If anything fails, this method throws RuntimeException so the controller
     * can decide whether to ignore the blockchain error or not.
     */
    public CommitResult sendPasswordCommitment(String title, String clearPassword) {
        try {
            String toHash = clearPassword + "|" + secretSalt + "|" + title;
            byte[] commitmentBytes = Hash.sha3(toHash.getBytes(StandardCharsets.UTF_8));
            String commitmentHex = Numeric.toHexString(commitmentBytes);

            if (commitmentBytes.length != 32) {
                // ensure exactly 32 bytes (Bytes32)
                commitmentBytes = Arrays.copyOf(commitmentBytes, 32);
            }

            Bytes32 commitment = new Bytes32(commitmentBytes);
            Utf8String label = new Utf8String(title);

            Function function = new Function(
                    "storeCommitment",
                    Arrays.asList(commitment, label),
                    Collections.emptyList()
            );

            String encodedFunction = FunctionEncoder.encode(function);

            EthGetTransactionCount txCountResp = web3j.ethGetTransactionCount(
                    credentials.getAddress(), DefaultBlockParameterName.LATEST
            ).send();
            BigInteger nonce = txCountResp.getTransactionCount();

            EthGasPrice gasPriceResp = web3j.ethGasPrice().send();
            BigInteger gasPrice = gasPriceResp.getGasPrice();

            // Simple fixed gas limit for this tiny function
            BigInteger gasLimit = BigInteger.valueOf(200_000L);

            RawTransaction rawTx = RawTransaction.createTransaction(
                    nonce,
                    gasPrice,
                    gasLimit,
                    contractAddress,
                    BigInteger.ZERO,
                    encodedFunction
            );

            byte[] signedMessage = TransactionEncoder.signMessage(rawTx, chainId, credentials);
            String hexValue = Numeric.toHexString(signedMessage);

            EthSendTransaction response = web3j.ethSendRawTransaction(hexValue).send();
            if (response.hasError()) {
                throw new RuntimeException("Error sending tx: " + response.getError().getMessage());
            }
            String txHash = response.getTransactionHash();
            return new CommitResult(commitmentHex, txHash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send password commitment", e);
        }
    }
}
