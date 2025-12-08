package com.example.mustacheapp.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
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
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Bytes32;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;

/**
 * Simple helper that sends a storeCommitment(commitment, label) transaction
 * to the PasswordCommitments smart contract using a single admin wallet.
 *
 * If the configured private key is invalid or placeholder-like, the service
 * stays "disabled" and calls to sendPasswordCommitment() will simply throw
 * a RuntimeException that the controller catches and ignores, so the UI works
 * even without a valid blockchain config.
 */
@Service
public class BlockchainService {

    private final Web3j web3j;
    private final Credentials credentials; // may be null if misconfigured
    private final String contractAddress;
    private final long chainId;
    private final String secretSalt;
    private final boolean enabled;

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
            @Value("${blockchain.secretSalt:EMPRESARIOS_DEMO_SECRET}") String secretSalt) {
        this.web3j = Web3j.build(new HttpService(rpcUrl));
        this.contractAddress = contractAddress;
        this.chainId = chainId;
        this.secretSalt = secretSalt;

        Credentials creds = null;
        boolean ok = false;

        try {
            if (privateKey != null) {
                String trimmed = privateKey.trim();
                // Treat obvious placeholder patterns as "disabled"
                if (!trimmed.isEmpty()
                        && !trimmed.contains("YOUR")
                        && !trimmed.contains("your")
                        && trimmed.length() >= 10) {
                    creds = Credentials.create(trimmed);
                    ok = true;
                }
            }
        } catch (Exception e) {
            System.err.println(
                    "[BlockchainService] Invalid private key, blockchain integration disabled: " + e.getMessage());
            creds = null;
            ok = false;
        }

        this.credentials = creds;
        this.enabled = ok;

        if (!enabled) {
            System.err.println(
                    "[BlockchainService] WARNING: Blockchain integration is DISABLED (no valid private key configured).");
        } else {
            System.out.println(
                    "[BlockchainService] Blockchain integration ENABLED for address: " + this.credentials.getAddress());
        }
    }

    public CommitResult sendPasswordCommitment(String title, String clearPassword) {
        if (!enabled || credentials == null) {
            throw new RuntimeException("Blockchain integration disabled (no valid private key).");
        }

        try {
            String toHash = clearPassword + "|" + secretSalt + "|" + title;
            String commitmentHex = Numeric.toHexString(Hash.sha3(toHash.getBytes(StandardCharsets.UTF_8)));

            byte[] commitmentBytes = Numeric.hexStringToByteArray(commitmentHex);
            if (commitmentBytes.length != 32) {
                commitmentBytes = Arrays.copyOf(commitmentBytes, 32);
            }

            Bytes32 commitment = new Bytes32(commitmentBytes);
            Utf8String label = new Utf8String(title);

            Function function = new Function(
                    "storeCommitment",
                    Arrays.asList(commitment, label),
                    Collections.emptyList());

            String encodedFunction = FunctionEncoder.encode(function);

            EthGetTransactionCount txCountResp = web3j.ethGetTransactionCount(
                    credentials.getAddress(), DefaultBlockParameterName.LATEST).send();
            BigInteger nonce = txCountResp.getTransactionCount();

            EthGasPrice gasPriceResp = web3j.ethGasPrice().send();
            BigInteger gasPrice = gasPriceResp.getGasPrice();
            BigInteger gasLimit = BigInteger.valueOf(200_000L);

            RawTransaction rawTx = RawTransaction.createTransaction(
                    nonce,
                    gasPrice,
                    gasLimit,
                    contractAddress,
                    BigInteger.ZERO,
                    encodedFunction);

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
