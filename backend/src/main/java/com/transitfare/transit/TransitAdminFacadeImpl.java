package com.transitfare.transit;

import com.transitfare.transit.usecase.ImportGtfsFaresUseCase;
import com.transitfare.transit.usecase.dto.GtfsImportResult;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** TransitAdminFacade 구현. 메서드마다 관리 행동 하나에 해당하는 유스케이스로 넘긴다. */
@Service
@RequiredArgsConstructor
public class TransitAdminFacadeImpl implements TransitAdminFacade {

    private final ImportGtfsFaresUseCase importGtfsFaresUseCase;

    /**
     * {@link ImportGtfsFaresUseCase}: GTFS zip을 읽어 같은 이름 정류장을 한 역으로 합쳐 넣고,
     * origin·destination 구역 간 요금을 역 쌍 요금(BASE)으로 넣거나 갱신한다
     */
    @Override
    public GtfsImportResult importGtfsFares(String operatorCode, String sourceUrl, InputStream zip) {
        return importGtfsFaresUseCase.execute(operatorCode, sourceUrl, zip);
    }
}
