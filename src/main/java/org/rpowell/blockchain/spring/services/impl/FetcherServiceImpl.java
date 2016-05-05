package org.rpowell.blockchain.spring.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.rpowell.blockchain.domain.*;
import org.rpowell.blockchain.network.requests.BlockInfoRequests;
import org.rpowell.blockchain.util.file.FileComparator;
import org.rpowell.blockchain.spring.services.IFetcherService;
import org.rpowell.blockchain.util.file.FileUtil;
import org.rpowell.blockchain.util.constant.StringConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class FetcherServiceImpl implements IFetcherService {

    public static Block latestBlockOnDisk;

    private static final Logger log = LoggerFactory.getLogger(FetcherServiceImpl.class);

    private final ObjectMapper mapper = new ObjectMapper();
    private int count = 0;
    private boolean resume = false;

    // For use by spring
    protected FetcherServiceImpl() {}

    @Override
    public void writeBlockchainToJSON() {

        // Ensure directory exists
        File file = new File(StringConstants.JSON_PATH);
        if (!file.exists()) {
            file.mkdirs();
        }

        // Retrieve files already on disk
        List<File> jsonFiles = FileUtil.getFolderContents(StringConstants.JSON_PATH);

        // Sort the list by numeric file name
        Collections.sort(jsonFiles, new FileComparator(StringConstants.JSON_FILE_EXT));

        // Retrieve the latest block on the blockchain
        LatestBlock latestBlock = BlockInfoRequests.getLatestBlock();
        Block latestNetworkBlock = BlockInfoRequests.getBlockByHash(latestBlock.getHash());

        Block genesisBlock = BlockInfoRequests.getBlockByHash(StringConstants.GENESIS_BLOCK);

        if (jsonFiles.isEmpty()) {
            log.info(StringConstants.LINE_BREAK);
            log.info("Starting blockchain download");
            log.info(StringConstants.LINE_BREAK);

            downloadBlocks(latestNetworkBlock, genesisBlock.getBlock_index());
        } else {
            log.info(StringConstants.LINE_BREAK);
            log.info("Downloading new blocks. Please let this process complete.");
            log.info(StringConstants.LINE_BREAK);

            latestBlockOnDisk = getLatestBlockOnDisk(jsonFiles);

            if (latestBlock.getBlock_index() == latestBlockOnDisk.getBlock_index()) {
                log.info("Already up to date.");
            } else {
                downloadBlocks(latestNetworkBlock, latestBlockOnDisk.getBlock_index());
            }

            if (resume) {
                Block earliestBlockOnDisk = getEarliestBlockOnDisk(jsonFiles);
                if (earliestBlockOnDisk.getBlock_index() > genesisBlock.getBlock_index()) {
                    count = 0;
                    log.info(StringConstants.LINE_BREAK);
                    log.info("Continuing blockchain history download. Feel free to cancel this process.");
                    log.info(StringConstants.LINE_BREAK);

                    Block previousBlock = BlockInfoRequests.getBlockByHash(earliestBlockOnDisk.getPrev_block());
                    downloadBlocks(previousBlock, genesisBlock.getBlock_index());
                }
            }
        }
    }

    /**
     * Download blocks to disk starting from the given block and
     * working backwards.
     * @param startBlock The block to start downloading from.
     */
    private void downloadBlocks(Block startBlock, long stopIndex) {

        while (startBlock.getBlock_index() > stopIndex) {
            try {
                long index = startBlock.getBlock_index();
                File newFile = new File(StringConstants.JSON_PATH + index + StringConstants.JSON_FILE_EXT);

                filterInvalidTransactions(startBlock);

                mapper.writeValue(newFile, startBlock);
                log.info("Writing file " + newFile.toString());
                log.info("Blocks downloaded: " + count);
                log.info("Remaining Blocks: " + (index - stopIndex));
                log.info(StringConstants.LINE_BREAK);

                startBlock = BlockInfoRequests.getBlockByHash(startBlock.getPrev_block());

                count++;
            } catch (Exception e) {
                log.error("Error downloading block", e);
                log.error("Sleeping for one minute", e);
                try {
                    Thread.sleep(TimeUnit.MINUTES.toMillis(1));
                } catch (InterruptedException e1) {
                    log.error("Interrupted during sleep");
                }
            }
        }
    }

    /**
     * Some blockchain data is lossy. Remove the transactions from the block that are missing information about
     * the addresses of inputs / outputs. This may result in data loss.
     *
     * @param block The block to filter.
     */
    private void filterInvalidTransactions(Block block) {
        List<Transaction> filteredTransactions = new ArrayList<>();

        for (Transaction transaction : block.getTx()) {
            boolean filterTransaction = false;

            for (Input input : transaction.getInputs()) {
                if (input.getPrev_out() == null || input.getPrev_out().getAddr() == null) {
                    filterTransaction = true;
                }
            }

            for (Output output : transaction.getOut()) {
                if (output.getAddr() == null) {
                    filterTransaction = true;
                }
            }

            if (!filterTransaction) {
                filteredTransactions.add(transaction);
            }
        }

        block.setTx(filteredTransactions);
    }

    /**
     * Get the earliest block on disk.
     * Assumes the list of files are sorted from low to high.
     *
     * @param jsonFiles The files to search.
     * @return          The earliest block.
     */
    private Block getEarliestBlockOnDisk(List<File> jsonFiles) {
        Block earliestBlock = null;

        if (jsonFiles != null && !jsonFiles.isEmpty()) {
            File lastFile = jsonFiles.get(0);

            try {
                earliestBlock = mapper.readValue(lastFile, Block.class);
            } catch (IOException e) {
                log.error("Error getting earliest block", e);
            }
        }

        return earliestBlock;
    }

    /**
     * Get the latest block on disk.
     * Assumes the list of files are sorted from low to high.
     *
     * @param jsonFiles The files to search.
     * @return          The latest block.
     */
    private Block getLatestBlockOnDisk(List<File> jsonFiles) {
        Block lastBlock = null;

        if (jsonFiles != null && !jsonFiles.isEmpty()) {
            File lastFile = jsonFiles.get(jsonFiles.size() - 1);

            try {
                lastBlock = mapper.readValue(lastFile, Block.class);
            } catch (IOException e) {
                log.error("Error getting latest block", e);
            }
        }

        return lastBlock;
    }
}
