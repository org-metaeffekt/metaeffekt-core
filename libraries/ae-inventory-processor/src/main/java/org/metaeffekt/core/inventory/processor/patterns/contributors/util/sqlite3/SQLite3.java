/*
 * Copyright 2009-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.metaeffekt.core.inventory.processor.patterns.contributors.util.sqlite3;

import org.metaeffekt.core.inventory.processor.patterns.contributors.util.Database;
import org.metaeffekt.core.inventory.processor.patterns.contributors.util.Entry;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.sql.*;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class SQLite3 implements Database {

    private static final byte[] SQLITE3_HEADER_MAGIC = "SQLite format 3\00".getBytes();

    private final Connection connection;

    public SQLite3(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Database open(String path) {
        try (RandomAccessFile file = new RandomAccessFile(path, "r")) {
            // Read file to byte array of length 16
            ByteBuffer buffer = ByteBuffer.allocate(16);
            byte[] headerBuff = buffer.array();
            file.read(headerBuff);

            // Check if file is a SQLite3 database
            if (!Arrays.equals(headerBuff, SQLITE3_HEADER_MAGIC)) {
                throw new RuntimeException("File is not a SQLite3 database: " + path);
            }
            Class.forName("org.sqlite.JDBC");
            File sqliteFile = new File(path);
            String uri = "jdbc:sqlite:" + sqliteFile.getAbsolutePath();
            Connection connection = DriverManager.getConnection(uri);
            return new SQLite3(connection);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public BlockingQueue<Entry> read() {
        BlockingQueue<Entry> entries = new LinkedBlockingQueue<>();
        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.submit(() -> {
            try (Statement stmt = this.connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT blob FROM Packages")) {

                if (rs == null) {
                    entries.put(new Entry(null, new SQLException("Query failed to return rows")));
                    return;
                }

                while (rs.next()) {
                    byte[] blob = rs.getBytes("blob");
                    entries.put(new Entry(blob, null));
                }
            } catch (SQLException e) {
                try {
                    entries.put(new Entry(null, new Exception("Failed to SELECT query: " + e.getMessage(), e)));
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                // signal that processing is complete
                try {
                    entries.put(new Entry()); // sentinel value indicating completion
                    this.connection.close();
                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                }
                executor.shutdown();
            }
        });
        return entries;
    }
}
