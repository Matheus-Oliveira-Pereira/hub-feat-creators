package com.hubfeatcreators.domain.importacao;

import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

@Component
public class CsvXlsxParser {

    public record ParseResult(
            String[] headers, int estimatedTotal, String encoding, char separator) {}

    /** Detects headers and estimated row count without loading full file. */
    public ParseResult probe(Path filePath) throws IOException {
        String name = filePath.getFileName().toString().toLowerCase();
        if (name.endsWith(".xlsx") || name.endsWith(".xls")) {
            return probeXlsx(filePath);
        }
        return probeCsv(filePath);
    }

    /**
     * Streams rows applying column mapping. Calls rowConsumer for each mapped row. rowConsumer
     * receives (rowIndex, mappedValues) where mappedValues keys are field names.
     */
    public void stream(
            Path filePath,
            java.util.Map<String, String> mapeamento,
            String encoding,
            char separator,
            RowConsumer consumer)
            throws IOException {
        String name = filePath.getFileName().toString().toLowerCase();
        if (name.endsWith(".xlsx") || name.endsWith(".xls")) {
            streamXlsx(filePath, mapeamento, consumer);
        } else {
            streamCsv(filePath, mapeamento, encoding, separator, consumer);
        }
    }

    @FunctionalInterface
    public interface RowConsumer {
        void accept(int rowIndex, java.util.Map<String, String> row) throws Exception;
    }

    // ---- CSV ---

    private ParseResult probeCsv(Path filePath) throws IOException {
        byte[] firstBytes = Files.readAllBytes(filePath);
        Charset charset = detectCharset(firstBytes);
        String preview = new String(firstBytes, 0, Math.min(firstBytes.length, 8192), charset);
        char sep = detectSeparator(preview);

        CsvParserSettings settings = buildSettings(sep);
        settings.setNumberOfRowsToSkip(0);
        CsvParser parser = new CsvParser(settings);

        try (InputStream is = Files.newInputStream(filePath);
                InputStreamReader reader = new InputStreamReader(is, charset)) {
            List<String[]> rows = parser.parseAll(reader);
            String[] headers = rows.isEmpty() ? new String[0] : rows.get(0);
            int total = Math.max(0, rows.size() - 1);
            return new ParseResult(headers, total, charset.name(), sep);
        }
    }

    private void streamCsv(
            Path filePath,
            java.util.Map<String, String> mapeamento,
            String encoding,
            char separator,
            RowConsumer consumer)
            throws IOException {
        Charset charset = Charset.forName(encoding);
        CsvParserSettings settings = buildSettings(separator);

        CsvParser parser = new CsvParser(settings);
        try (InputStream is = Files.newInputStream(filePath);
                InputStreamReader reader = new InputStreamReader(is, charset)) {

            parser.beginParsing(reader);
            String[] headers = parser.parseNext();
            if (headers == null) return;

            String[] row;
            int idx = 1;
            while ((row = parser.parseNext()) != null) {
                java.util.Map<String, String> mapped = mapRow(headers, row, mapeamento);
                try {
                    consumer.accept(idx++, mapped);
                } catch (Exception e) {
                    throw new IOException("Erro na linha " + idx, e);
                }
            }
            parser.stopParsing();
        }
    }

    private CsvParserSettings buildSettings(char sep) {
        CsvParserSettings s = new CsvParserSettings();
        s.getFormat().setDelimiter(sep);
        s.getFormat().setQuote('"');
        s.setLineSeparatorDetectionEnabled(true);
        s.setIgnoreLeadingWhitespacesInQuotes(true);
        s.setIgnoreTrailingWhitespacesInQuotes(true);
        s.setMaxCharsPerColumn(8192);
        s.setMaxColumns(100);
        return s;
    }

    // ---- XLSX ---

    private ParseResult probeXlsx(Path filePath) throws IOException {
        try (InputStream is = Files.newInputStream(filePath);
                var wb = WorkbookFactory.create(is)) {
            Sheet sheet = wb.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) return new ParseResult(new String[0], 0, "UTF-8", ',');

            String[] headers = new String[headerRow.getLastCellNum()];
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                Cell cell = headerRow.getCell(i);
                headers[i] = cell != null ? cellString(cell) : "";
            }
            int total = Math.max(0, sheet.getLastRowNum());
            return new ParseResult(headers, total, "UTF-8", ',');
        } catch (Exception e) {
            throw new IOException("Erro lendo XLSX: " + e.getMessage(), e);
        }
    }

    private void streamXlsx(
            Path filePath, java.util.Map<String, String> mapeamento, RowConsumer consumer)
            throws IOException {
        try (InputStream is = Files.newInputStream(filePath);
                var wb = WorkbookFactory.create(is)) {
            Sheet sheet = wb.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) return;

            String[] headers = new String[headerRow.getLastCellNum()];
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                Cell cell = headerRow.getCell(i);
                headers[i] = cell != null ? cellString(cell) : "";
            }

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                String[] vals = new String[headers.length];
                for (int c = 0; c < headers.length; c++) {
                    Cell cell = row.getCell(c);
                    vals[c] = cell != null ? cellString(cell) : "";
                }
                java.util.Map<String, String> mapped = mapRow(headers, vals, mapeamento);
                try {
                    consumer.accept(r, mapped);
                } catch (Exception e) {
                    throw new IOException("Erro na linha " + r, e);
                }
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Erro lendo XLSX: " + e.getMessage(), e);
        }
    }

    private String cellString(Cell cell) {
        if (cell.getCellType() == CellType.NUMERIC) {
            double v = cell.getNumericCellValue();
            if (v == Math.floor(v) && !Double.isInfinite(v)) {
                return String.valueOf((long) v);
            }
            return String.valueOf(v);
        }
        if (cell.getCellType() == CellType.BOOLEAN) {
            return String.valueOf(cell.getBooleanCellValue());
        }
        return cell.toString().trim();
    }

    // ---- Utilities ---

    private java.util.Map<String, String> mapRow(
            String[] headers, String[] values, java.util.Map<String, String> mapeamento) {
        java.util.Map<String, String> result = new java.util.LinkedHashMap<>();
        for (int i = 0; i < headers.length; i++) {
            String header = headers[i];
            String fieldName = mapeamento.get(header);
            if (fieldName != null && !fieldName.isBlank()) {
                String val = i < values.length ? values[i] : null;
                result.put(fieldName, val != null ? val.trim() : "");
            }
        }
        return result;
    }

    /** Detects encoding from BOM bytes or content sampling. */
    private Charset detectCharset(byte[] bytes) {
        if (bytes.length >= 3
                && bytes[0] == (byte) 0xEF
                && bytes[1] == (byte) 0xBB
                && bytes[2] == (byte) 0xBF) {
            return StandardCharsets.UTF_8; // UTF-8 BOM
        }
        // Try UTF-8: if decoding produces replacement chars, fallback to Latin-1
        String utf8 = new String(bytes, 0, Math.min(bytes.length, 4096), StandardCharsets.UTF_8);
        if (utf8.contains("�")) {
            return Charset.forName("Windows-1252");
        }
        return StandardCharsets.UTF_8;
    }

    /** Counts delimiter occurrences in preview to pick most likely separator. */
    private char detectSeparator(String preview) {
        long commas = preview.chars().filter(c -> c == ',').count();
        long semis = preview.chars().filter(c -> c == ';').count();
        long tabs = preview.chars().filter(c -> c == '\t').count();
        if (tabs >= commas && tabs >= semis) return '\t';
        if (semis >= commas) return ';';
        return ',';
    }
}
