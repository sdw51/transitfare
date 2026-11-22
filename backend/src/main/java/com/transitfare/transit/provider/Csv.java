package com.transitfare.transit.provider;

import java.util.ArrayList;
import java.util.List;

/** GTFS용 최소 CSV 파서. build.gradle.kts(공용 파일)에 CSV 라이브러리를 추가하지 않으려고 직접 둔다 */
final class Csv {

    private Csv() {
    }

    /**
     * 글자를 하나씩 읽으며 칸과 줄을 나눈다. 따옴표 안에서는 쉼표·줄바꿈도 값의 일부이고, "" 는 따옴표 한 글자다.
     * 맨 앞 BOM(엑셀로 저장한 UTF-8)과 빈 줄은 버린다.
     */
    static List<List<String>> parse(String text) {
        String s = text.startsWith("﻿") ? text.substring(1) : text;
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (inQuotes) {
                if (c == '"' && i + 1 < s.length() && s.charAt(i + 1) == '"') {
                    field.append('"');   // "" → 따옴표 한 글자
                    i++;
                } else if (c == '"') {
                    inQuotes = false;    // 따옴표 닫힘
                } else {
                    field.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                row.add(field.toString());
                field.setLength(0);
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < s.length() && s.charAt(i + 1) == '\n') {
                    i++;                 // CRLF는 한 줄바꿈
                }
                row.add(field.toString());
                field.setLength(0);
                addIfNotBlank(rows, row);
                row = new ArrayList<>();
            } else {
                field.append(c);
            }
        }
        row.add(field.toString());       // 마지막 줄(줄바꿈 없이 끝난 경우)
        addIfNotBlank(rows, row);
        return rows;
    }

    private static void addIfNotBlank(List<List<String>> rows, List<String> row) {
        boolean blank = row.size() == 1 && row.get(0).isBlank();
        if (!blank) {
            rows.add(row);
        }
    }
}
