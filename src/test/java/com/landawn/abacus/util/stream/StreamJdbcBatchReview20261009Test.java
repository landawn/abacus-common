package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.UncheckedSQLException;

@Tag("unit")
class StreamJdbcBatchReview20261009Test extends TestBase {

    enum StatementSource {
        STATEMENT, CONNECTION, DATA_SOURCE
    }

    @ParameterizedTest
    @EnumSource(StatementSource.class)
    void failedAddAtBatchBoundaryStillFlushesEarlierRows(final StatementSource statementSource) throws SQLException {
        final RecordingBatch batch = new RecordingBatch(2);
        final Stream<Integer> saved = batch.save(statementSource, Stream.of(1, 2));

        final UncheckedSQLException failure = assertThrows(UncheckedSQLException.class, saved::count);

        assertSame(batch.addFailure, failure.getCause());
        assertEquals(List.of(List.of(1)), batch.executedBatches);
        assertEquals(List.of(), batch.pendingRows);
    }

    @ParameterizedTest
    @EnumSource(StatementSource.class)
    void failedFirstAddDoesNotExecuteAnEmptyBatch(final StatementSource statementSource) throws SQLException {
        final RecordingBatch batch = new RecordingBatch(1);
        final Stream<Integer> saved = batch.save(statementSource, Stream.of(1));

        assertThrows(UncheckedSQLException.class, saved::count);

        assertEquals(List.of(), batch.executedBatches);
    }

    @ParameterizedTest
    @EnumSource(StatementSource.class)
    void iterationAfterRejectedAddUsesSuccessfulRowsForBatchSize(final StatementSource statementSource) throws SQLException {
        final RecordingBatch batch = new RecordingBatch(1);

        try (Stream<Integer> saved = batch.save(statementSource, Stream.of(1, 2, 3, 4))) {
            final Iterator<Integer> iterator = saved.iterator();

            assertThrows(UncheckedSQLException.class, iterator::next);
            assertEquals(2, iterator.next());
            assertEquals(List.of(), batch.executedBatches);
            assertEquals(3, iterator.next());
            assertEquals(List.of(List.of(2, 3)), batch.executedBatches);
            assertEquals(4, iterator.next());
        }

        assertEquals(List.of(List.of(2, 3), List.of(4)), batch.executedBatches);
    }

    private static final class RecordingBatch {
        private static final String SQL = "INSERT INTO review_values VALUES (?)";

        final PreparedStatement statement = mock(PreparedStatement.class);
        final SQLException addFailure = new SQLException("rejected add");
        final List<Integer> pendingRows = new ArrayList<>();
        final List<List<Integer>> executedBatches = new ArrayList<>();
        private int parameter;
        private int addCalls;

        RecordingBatch(final int rejectedAdd) throws SQLException {
            doAnswer(invocation -> {
                parameter = invocation.getArgument(1);
                return null;
            }).when(statement).setInt(anyInt(), anyInt());
            doAnswer(invocation -> {
                if (++addCalls == rejectedAdd) {
                    throw addFailure;
                }
                pendingRows.add(parameter);
                return null;
            }).when(statement).addBatch();
            when(statement.executeBatch()).thenAnswer(invocation -> {
                executedBatches.add(new ArrayList<>(pendingRows));
                return new int[pendingRows.size()];
            });
            doAnswer(invocation -> {
                pendingRows.clear();
                return null;
            }).when(statement).clearBatch();
        }

        Stream<Integer> save(final StatementSource statementSource, final Stream<Integer> source) throws SQLException {
            if (statementSource == StatementSource.STATEMENT) {
                return source.onEachSave(statement, 2, 0, (value, stmt) -> stmt.setInt(1, value));
            }

            final Connection connection = mock(Connection.class);
            when(connection.prepareStatement(SQL)).thenReturn(statement);

            if (statementSource == StatementSource.CONNECTION) {
                return source.onEachSave(connection, SQL, 2, 0, (value, stmt) -> stmt.setInt(1, value));
            }

            final DataSource dataSource = mock(DataSource.class);
            when(dataSource.getConnection()).thenReturn(connection);
            return source.onEachSave(dataSource, SQL, 2, 0, (value, stmt) -> stmt.setInt(1, value));
        }
    }
}
