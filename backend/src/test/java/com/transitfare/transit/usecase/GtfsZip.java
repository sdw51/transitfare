package com.transitfare.transit.usecase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** 테스트용 GTFS zip 만들기. null인 파일은 넣지 않는다 */
public final class GtfsZip {

    private GtfsZip() {
    }

    public static InputStream of(String stops, String fareAttributes, String fareRules) {
        return new ByteArrayInputStream(bytes(stops, fareAttributes, fareRules));
    }

    public static byte[] bytes(String stops, String fareAttributes, String fareRules) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            put(zip, "stops.txt", stops);
            put(zip, "fare_attributes.txt", fareAttributes);
            put(zip, "fare_rules.txt", fareRules);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    private static void put(ZipOutputStream zip, String name, String content) throws IOException {
        if (content == null) {
            return;
        }
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }
}
