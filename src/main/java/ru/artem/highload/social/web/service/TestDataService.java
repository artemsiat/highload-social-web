package ru.artem.highload.social.web.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.artem.highload.social.web.dto.GenerateTestDataResponse;
import ru.artem.highload.social.web.repository.UserRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestDataService {

    private static final int BATCH_SIZE = 100;
    private static final int MAX_RETRIES_PER_BATCH = 5;

    private final UserRepository userRepository;
    private final RandomUserGenerator userGenerator;

    @Transactional
    public GenerateTestDataResponse generate(long requestedCount, long maxAllowed) {
        long currentCount = userRepository.count();

        if (currentCount >= maxAllowed) {
            return new GenerateTestDataResponse(0, currentCount,
                    "Maximum allowed records (%d) already reached. Current count: %d"
                            .formatted(maxAllowed, currentCount));
        }

        long actualCount = Math.min(requestedCount, maxAllowed - currentCount);
        long generated = insertInBatches(actualCount, currentCount);

        return new GenerateTestDataResponse(generated, currentCount + generated,
                "Successfully generated %d records".formatted(generated));
    }

    private long insertInBatches(long count, long currentCount) {
        if (count <= 0) {
            return 0;
        }

        log.info("Starting test data generation: count={}, currentRecords={}", count, currentCount);

        String passwordHash = userGenerator.computeSharedPasswordHash();
        log.info("Pre-computed shared password hash for all test users");

        long inserted = 0;
        long batchNumber = 0;

        while (inserted < count) {
            int batchSize = (int) Math.min(BATCH_SIZE, count - inserted);

            boolean success = false;
            for (int attempt = 1; attempt <= MAX_RETRIES_PER_BATCH; attempt++) {
                long genStart = System.currentTimeMillis();
                List<Object[]> batch = userGenerator.generateBatch(batchSize, passwordHash);
                long genTime = System.currentTimeMillis() - genStart;

                try {
                    long insertStart = System.currentTimeMillis();
                    userRepository.batchInsertUsers(batch);
                    long insertTime = System.currentTimeMillis() - insertStart;

                    inserted += batchSize;
                    batchNumber++;
                    log.info("Batch {}: inserted {} records (generate={}ms, insert={}ms), progress: {}/{}",
                            batchNumber, batchSize, genTime, insertTime, inserted, count);
                    success = true;
                    break;
                } catch (DuplicateKeyException e) {
                    log.warn("Batch duplicate key collision (attempt {}/{}), regenerating...",
                            attempt, MAX_RETRIES_PER_BATCH);
                }
            }

            if (!success) {
                log.error("Failed to insert batch after {} retries, stopping generation", MAX_RETRIES_PER_BATCH);
                break;
            }
        }

        log.info("Test data generation completed: generated={}, total={}", inserted, currentCount + inserted);
        return inserted;
    }
}
