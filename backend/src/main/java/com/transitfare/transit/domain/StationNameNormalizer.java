package com.transitfare.transit.domain;

/**
 * 역·정류장 이름 정규화.
 * Google 경로 응답의 정류장 이름("博多駅", "ＪＲ博多シティ 前")과 우리 역 이름("博多")을
 * 표기 차이 없이 비교하려고 쓴다(역 매칭 Task 10-A: alias·name_ja 정확 일치가 실패했을 때 두 번째 시도).
 */
public final class StationNameNormalizer {

    private static final String STATION_SUFFIX = "駅";

    /** 전각 영숫자(Ａ, ０ 등)와 반각(A, 0)의 유니코드 차이 */
    private static final int FULL_TO_HALF_OFFSET = 0xFEE0;

    private StationNameNormalizer() {
    }

    /**
     * 이름을 비교용으로 정규화한다.
     * 순서: ① 공백 제거(전각 공백 포함) → ② 전각 영숫자를 반각으로 → ③ 끝의 "駅" 하나 제거.
     *
     * @param name 원래 이름(null 가능)
     * @return 정규화한 이름. null이면 빈 문자열
     */
    public static String normalize(String name) {
        if (name == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(name.length());
        for (char c : name.toCharArray()) {
            // isWhitespace는 전각 공백(U+3000)을 공백으로 보지 않아서 isSpaceChar도 확인
            if (Character.isWhitespace(c) || Character.isSpaceChar(c)) {
                continue;
            }
            sb.append(toHalfWidthAlnum(c));
        }
        String result = sb.toString();
        // 이름이 "駅" 한 글자뿐이면 빈 문자열이 되지 않게 그대로 둔다
        if (result.endsWith(STATION_SUFFIX) && result.length() > STATION_SUFFIX.length()) {
            result = result.substring(0, result.length() - STATION_SUFFIX.length());
        }
        return result;
    }

    /** 전각 숫자·영문 대소문자만 반각으로 바꾸고, 그 밖의 문자(가나, 한자, 기호)는 그대로 둔다 */
    private static char toHalfWidthAlnum(char c) {
        boolean fullWidthDigit = c >= '０' && c <= '９';
        boolean fullWidthUpper = c >= 'Ａ' && c <= 'Ｚ';
        boolean fullWidthLower = c >= 'ａ' && c <= 'ｚ';
        return (fullWidthDigit || fullWidthUpper || fullWidthLower) ? (char) (c - FULL_TO_HALF_OFFSET) : c;
    }
}
