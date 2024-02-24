package org.web3j.spring.actuate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.util.Assert;
import org.web3j.protocol.Web3j;

/**
 * Health check indicator for Web3j
 */
public class Web3jHealthIndicator extends AbstractHealthIndicator {


    private final Web3j web3j;

    public Web3jHealthIndicator(Web3j web3j) {
        Assert.notNull(web3j, "Web3j must not be null");
        this.web3j = web3j;
    }

    @Override
    public void doHealthCheck(Health.Builder builder) {
        try {
            boolean listening = web3j.netListening().send().isListening();
            if (!listening) {
                builder.down();
            } else {
                builder.up();
                List<CompletableFuture<Builder>> futures = new ArrayList<>();

                futures.add(web3j.netVersion()
                        .sendAsync()
                        .thenApply(netVersion ->
                                builder.withDetail("netVersion", netVersion.getNetVersion())));

                futures.add(web3j.web3ClientVersion()
                        .sendAsync()
                        .thenApply(web3ClientVersion ->
                                builder.withDetail("clientVersion", web3ClientVersion.getWeb3ClientVersion())));

                futures.add(web3j.ethBlockNumber()
                        .sendAsync()
                        .thenApply(ethBlockNumber ->
                                builder.withDetail("blockNumber", ethBlockNumber.getBlockNumber())));

                futures.add(web3j.ethProtocolVersion()
                        .sendAsync()
                        .thenApply(ethProtocolVersion ->
                                builder.withDetail("protocolVersion", ethProtocolVersion.getProtocolVersion())));

                futures.add(web3j.netPeerCount()
                        .sendAsync()
                        .thenApply(netPeerCount ->
                                builder.withDetail("netPeerCount", netPeerCount.getQuantity())));

                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();
            }

        } catch (Exception ex) {
            builder.down(ex);
        }
    }
}
