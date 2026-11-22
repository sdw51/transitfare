package com.transitfare.transit.provider;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.springframework.stereotype.Component;

/** GTFS zip(쇼와 버스 GTFS-JP 등, CC BY 4.0)을 읽는 외부 데이터 어댑터. 파일 형식만 다루고 DB는 모른다 */
@Component
public class GtfsFeedReader {

    /**
     * zip에서 stops.txt, fare_attributes.txt, fare_rules.txt를 읽는다.
     *
     * @param zipStream GTFS zip 내용
     * @return 파일별 표
     * @throws IllegalArgumentException zip이 아니거나 stops.txt가 없을 때(유스케이스가 TRANSIT_GTFS_INVALID로 바꾼다)
     */
    public GtfsFeed read(InputStream zipStream) {
        Map<String, String> files = unzipTextFiles(zipStream);
        if (!files.containsKey("stops.txt")) {
            throw new IllegalArgumentException("stops.txt가 없습니다");
        }
        return new GtfsFeed(
                table(files.get("stops.txt")),
                Optional.ofNullable(files.get("fare_attributes.txt")).map(GtfsFeedReader::table),
                Optional.ofNullable(files.get("fare_rules.txt")).map(GtfsFeedReader::table));
    }

    /** zip 안의 .txt 파일들을 이름 → 내용(UTF-8)으로. 폴더 안에 있어도 파일 이름만 본다 */
    private static Map<String, String> unzipTextFiles(InputStream zipStream) {
        Map<String, String> files = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(zipStream, StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = Path.of(entry.getName()).getFileName().toString();
                if (!entry.isDirectory() && name.endsWith(".txt")) {
                    files.put(name, new String(zip.readAllBytes(), StandardCharsets.UTF_8));
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("zip을 읽을 수 없습니다", e);
        }
        if (files.isEmpty()) {
            throw new IllegalArgumentException("zip이 아니거나 비어 있습니다");   // zip이 아닌 파일은 항목이 0개로 읽힌다
        }
        return files;
    }

    /** CSV 내용 → 행 목록. 첫 줄은 열 이름. 값이 모자란 행은 빈 문자열로 채운다 */
    private static List<Map<String, String>> table(String csv) {
        List<List<String>> rows = Csv.parse(csv);
        if (rows.isEmpty()) {
            return List.of();
        }
        List<String> header = rows.get(0);
        return rows.subList(1, rows.size()).stream()
                .map(row -> {
                    Map<String, String> map = new HashMap<>();
                    for (int i = 0; i < header.size(); i++) {
                        map.put(header.get(i).strip(), i < row.size() ? row.get(i).strip() : "");
                    }
                    return map;
                })
                .toList();
    }
}
