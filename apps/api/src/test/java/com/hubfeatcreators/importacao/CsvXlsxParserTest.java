package com.hubfeatcreators.importacao;

import static org.assertj.core.api.Assertions.assertThat;

import com.hubfeatcreators.domain.importacao.CsvXlsxParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CsvXlsxParserTest {

    private final CsvXlsxParser parser = new CsvXlsxParser();

    @TempDir Path tmp;

    @Test
    void probeCsv_utf8_comma() throws IOException {
        Path f = tmp.resolve("test.csv");
        Files.writeString(
                f, "nome,email,telefone\nJoão,joao@x.com,11999\n", StandardCharsets.UTF_8);

        CsvXlsxParser.ParseResult r = parser.probe(f);
        assertThat(r.headers()).containsExactly("nome", "email", "telefone");
        assertThat(r.estimatedTotal()).isEqualTo(1);
        assertThat(r.separator()).isEqualTo(',');
        assertThat(r.encoding()).isEqualTo("UTF-8");
    }

    @Test
    void probeCsv_semicolonSeparator() throws IOException {
        Path f = tmp.resolve("test.csv");
        Files.writeString(f, "nome;email\nMaria;maria@x.com\n", StandardCharsets.UTF_8);

        CsvXlsxParser.ParseResult r = parser.probe(f);
        assertThat(r.separator()).isEqualTo(';');
    }

    @Test
    void probeCsv_tabSeparator() throws IOException {
        Path f = tmp.resolve("test.csv");
        Files.writeString(f, "nome\temail\nMaria\tmaria@x.com\n", StandardCharsets.UTF_8);

        CsvXlsxParser.ParseResult r = parser.probe(f);
        assertThat(r.separator()).isEqualTo('\t');
    }

    @Test
    void streamCsv_mapsColumns() throws IOException {
        Path f = tmp.resolve("test.csv");
        Files.writeString(
                f,
                "Nome Completo,E-mail\nJoão Silva,joao@x.com\nMaria,maria@x.com\n",
                StandardCharsets.UTF_8);

        Map<String, String> mapeamento = Map.of("Nome Completo", "nome", "E-mail", "email");
        List<Map<String, String>> rows = new ArrayList<>();

        parser.stream(f, mapeamento, "UTF-8", ',', (idx, row) -> rows.add(row));

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0))
                .containsEntry("nome", "João Silva")
                .containsEntry("email", "joao@x.com");
        assertThat(rows.get(1))
                .containsEntry("nome", "Maria")
                .containsEntry("email", "maria@x.com");
    }

    @Test
    void streamCsv_unmappedColumnsIgnored() throws IOException {
        Path f = tmp.resolve("test.csv");
        Files.writeString(f, "nome,ignorado,email\nFoo,bar,foo@x.com\n", StandardCharsets.UTF_8);

        Map<String, String> mapeamento = Map.of("nome", "nome", "email", "email");
        List<Map<String, String>> rows = new ArrayList<>();

        parser.stream(f, mapeamento, "UTF-8", ',', (idx, row) -> rows.add(row));

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0)).doesNotContainKey("ignorado");
    }

    @Test
    void probeCsv_empty() throws IOException {
        Path f = tmp.resolve("empty.csv");
        Files.writeString(f, "", StandardCharsets.UTF_8);

        CsvXlsxParser.ParseResult r = parser.probe(f);
        assertThat(r.headers()).isEmpty();
        assertThat(r.estimatedTotal()).isEqualTo(0);
    }

    @Test
    void probeCsv_utf8Bom() throws IOException {
        Path f = tmp.resolve("bom.csv");
        // UTF-8 BOM + content
        byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] content = "nome,email\nJoão,j@x.com\n".getBytes(StandardCharsets.UTF_8);
        byte[] all = new byte[bom.length + content.length];
        System.arraycopy(bom, 0, all, 0, bom.length);
        System.arraycopy(content, 0, all, bom.length, content.length);
        Files.write(f, all);

        CsvXlsxParser.ParseResult r = parser.probe(f);
        assertThat(r.encoding()).isEqualTo("UTF-8");
        assertThat(r.estimatedTotal()).isEqualTo(1);
    }

    @Test
    void rowIndex_startsAtOne() throws IOException {
        Path f = tmp.resolve("test.csv");
        Files.writeString(f, "nome\nAlpha\nBeta\n", StandardCharsets.UTF_8);

        Map<String, String> mapeamento = Map.of("nome", "nome");
        List<Integer> indices = new ArrayList<>();
        parser.stream(f, mapeamento, "UTF-8", ',', (idx, row) -> indices.add(idx));

        assertThat(indices).containsExactly(1, 2);
    }
}
